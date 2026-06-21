package dev.hossain.neon.engine.highlightjs.internal

internal expect class JsRuntime(platformContext: Any? = null) : AutoCloseable {
    suspend fun initialize(htmlContent: String)
    suspend fun evaluate(script: String): String
    override fun close()
}
