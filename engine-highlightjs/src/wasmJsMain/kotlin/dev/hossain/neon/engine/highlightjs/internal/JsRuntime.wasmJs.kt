package dev.hossain.neon.engine.highlightjs.internal

import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(script) => { try { return String(eval(script)); } catch(e) { return 'null'; } }")
private external fun jsEval(script: String): String

internal actual class JsRuntime actual constructor(
    platformContext: Any?,
) : AutoCloseable {
    actual suspend fun initialize(htmlContent: String) {
        extractInlineScripts(htmlContent).forEach(::jsEval)
    }

    actual suspend fun evaluate(script: String): String {
        return jsEval(script)
    }

    actual override fun close() {}
}
