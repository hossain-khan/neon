package dev.hossain.neon.engine.highlightjs.internal

internal actual class JsRuntime actual constructor() : AutoCloseable {
    actual suspend fun initialize(htmlContent: String) {
        // highlight.js is loaded dynamically or already loaded in the page
    }

    actual suspend fun evaluate(script: String): String {
        return try {
            val result = js("eval(script)")
            result?.toString() ?: "null"
        } catch (e: Exception) {
            "null"
        }
    }

    actual override fun close() {}
}
