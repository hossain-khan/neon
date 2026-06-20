package dev.hossain.neon.engine.highlightjs.internal

internal expect class JsRuntime() : AutoCloseable {
    suspend fun initialize(htmlContent: String)
    suspend fun evaluate(script: String): String
    override fun close()
}
