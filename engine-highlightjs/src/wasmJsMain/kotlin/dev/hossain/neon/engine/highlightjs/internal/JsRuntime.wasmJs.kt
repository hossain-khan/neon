package dev.hossain.neon.engine.highlightjs.internal

@JsFun("(script) => { try { return String(eval(script)); } catch(e) { return 'null'; } }")
private external fun jsEval(script: String): String

internal actual class JsRuntime actual constructor() : AutoCloseable {
    actual suspend fun initialize(htmlContent: String) {}

    actual suspend fun evaluate(script: String): String {
        return jsEval(script)
    }

    actual override fun close() {}
}
