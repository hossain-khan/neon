package dev.hossain.neon.engine.highlightjs.internal

import platform.JavaScriptCore.JSContext
import platform.JavaScriptCore.JSValue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal actual class JsRuntime actual constructor(
    platformContext: Any?,
) : AutoCloseable {
    private var context: JSContext? = null
    private val readyDeferred = CompletableDeferred<JSContext>()

    actual suspend fun initialize(htmlContent: String) {
        withContext(Dispatchers.Default) {
            try {
                val ctx = JSContext()
                for (script in extractInlineScripts(htmlContent)) {
                    ctx.evaluateScript(script)
                }

                ctx.evaluateScript("""
                    var document = {
                        getElementById: function(id) {
                            return {
                                textContent: "",
                                className: "",
                                dataset: {},
                                innerHTML: ""
                            };
                        }
                    };
                """)

                readyDeferred.complete(ctx)
                context = ctx
            } catch (e: Exception) {
                readyDeferred.completeExceptionally(e)
            }
        }
    }

    actual suspend fun evaluate(script: String): String {
        val ctx = readyDeferred.await()
        return withContext(Dispatchers.Default) {
            val result = ctx.evaluateScript(script)
            result?.toString() ?: "null"
        }
    }

    actual override fun close() {
        context = null
    }
}
