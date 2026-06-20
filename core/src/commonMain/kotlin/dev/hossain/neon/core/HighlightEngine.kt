package dev.hossain.neon.core

public interface HighlightEngine : AutoCloseable {
    public val name: String
    public val supportedLanguages: Set<String>

    public suspend fun highlight(
        code: String,
        language: String,
        theme: HighlightTheme,
    ): Result<HighlightResult>

    public suspend fun highlightBoth(
        code: String,
        language: String,
        lightTheme: HighlightTheme,
        darkTheme: HighlightTheme,
    ): Result<ThemedHighlightResult>

    public suspend fun autoDetectLanguage(code: String): Result<String>

    public suspend fun listLanguages(): List<HighlightLanguageInfo>
}
