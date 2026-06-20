package dev.hossain.neon.core

public sealed class HighlightException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    public class EngineInitializationFailed(
        public val engine: String,
        cause: Throwable,
    ) : HighlightException("Engine '$engine' initialization failed", cause)

    public class UnsupportedLanguage(
        public val language: String,
        public val engine: String,
    ) : HighlightException("Language '$language' is not supported by engine '$engine'")

    public class ThemeLoadFailed(
        public val themeName: String,
        cause: Throwable,
    ) : HighlightException("Theme '$themeName' load failed", cause)

    public class NetworkError(
        cause: Throwable,
    ) : HighlightException("Network request failed: ${cause.message ?: cause::class.simpleName}", cause)

    public class JavaScriptEvaluationFailed(
        cause: Throwable,
    ) : HighlightException("JavaScript evaluation failed", cause)

    public class HtmlParseFailed(
        cause: Throwable,
    ) : HighlightException("HTML parsing failed", cause)

    public class Timeout(
        seconds: Long = TIMEOUT_SECONDS,
    ) : HighlightException("Highlighting timed out after ${seconds}s")

    public companion object {
        public const val TIMEOUT_SECONDS: Long = 5L
    }
}
