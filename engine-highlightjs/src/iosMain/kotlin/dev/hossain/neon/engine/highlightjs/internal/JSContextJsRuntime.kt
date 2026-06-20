package dev.hossain.neon.engine.highlightjs.internal

import platform.JavaScriptCore.JSContext
import platform.JavaScriptCore.JSValue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal actual class JsRuntime actual constructor() : AutoCloseable {
    private var context: JSContext? = null
    private val readyDeferred = CompletableDeferred<JSContext>()

    actual suspend fun initialize(htmlContent: String) {
        withContext(Dispatchers.Default) {
            try {
                val ctx = JSContext()
                // Extract scripts from the merged htmlContent
                val scripts = mutableListOf<String>()
                var start = 0
                while (true) {
                    val s = htmlContent.indexOf("<script>", start)
                    if (s == -1) break
                    val e = htmlContent.indexOf("</script>", s)
                    if (e == -1) break
                    scripts.add(htmlContent.substring(s + 8, e))
                    start = e + 9
                }

                // If no scripts were extracted, it might be the unmerged bridge.html.
                // In that case, we should run a basic mock or load.
                // Since our commonMain merges them, scripts will have both highlight.min.js and the bridge functions.
                for (script in scripts) {
                    ctx.evaluateScript(script)
                }

                // Mock DOM APIs if highlightElement is called, although we will call hljs.highlight directly
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
