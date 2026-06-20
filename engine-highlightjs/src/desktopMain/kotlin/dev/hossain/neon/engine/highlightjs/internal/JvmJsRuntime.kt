package dev.hossain.neon.engine.highlightjs.internal

import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import dev.hossain.neon.core.HighlightException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal actual class JsRuntime actual constructor() : AutoCloseable {
    private var engine: ScriptEngine? = null
    private val readyDeferred = CompletableDeferred<ScriptEngine>()

    actual suspend fun initialize(htmlContent: String) {
        withContext(Dispatchers.Default) {
            try {
                val manager = ScriptEngineManager()
                val jsEngine = manager.getEngineByName("JavaScript")
                    ?: manager.getEngineByName("js")
                    ?: throw HighlightException.UnsupportedPlatform(
                        engine = "highlightjs",
                        platform = "desktop",
                        details = "No JSR-223 JavaScript engine found. Add a JavaScript engine such as GraalJS to the runtime classpath."
                    )

                for (script in extractInlineScripts(htmlContent)) {
                    jsEngine.eval(script)
                }

                jsEngine.eval("""
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

                readyDeferred.complete(jsEngine)
                engine = jsEngine
            } catch (e: Exception) {
                readyDeferred.completeExceptionally(e)
            }
        }
    }

    actual suspend fun evaluate(script: String): String {
        val jsEngine = readyDeferred.await()
        return withContext(Dispatchers.Default) {
            val result = jsEngine.eval(script)
            result?.toString() ?: "null"
        }
    }

    actual override fun close() {
        engine = null
    }
}
