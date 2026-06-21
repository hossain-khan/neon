package dev.hossain.neon.runtime

import dev.hossain.neon.core.HighlightEngine
import dev.hossain.neon.core.HighlightException
import dev.hossain.neon.core.HighlightLanguageInfo
import dev.hossain.neon.core.HighlightResult
import dev.hossain.neon.core.HighlightTheme
import dev.hossain.neon.core.ThemedHighlightResult

internal class ManagedHighlightEngine(
    override val name: String,
) : HighlightEngine {
    private var delegate: HighlightEngine? = null
    private var failure: HighlightException? = null

    fun setDelegate(engine: HighlightEngine) {
        delegate?.close()
        delegate = engine
        failure = null
    }

    fun setFailure(error: HighlightException) {
        delegate?.close()
        delegate = null
        failure = error
    }

    override val supportedLanguages: Set<String>
        get() = delegate?.supportedLanguages ?: emptySet()

    override suspend fun highlight(
        code: String,
        language: String,
        theme: HighlightTheme,
    ): Result<HighlightResult> {
        val currentDelegate = delegate
        return if (currentDelegate != null) {
            currentDelegate.highlight(code, language, theme)
        } else {
            Result.failure(currentFailure())
        }
    }

    override suspend fun highlightBoth(
        code: String,
        language: String,
        lightTheme: HighlightTheme,
        darkTheme: HighlightTheme,
    ): Result<ThemedHighlightResult> {
        val currentDelegate = delegate
        return if (currentDelegate != null) {
            currentDelegate.highlightBoth(code, language, lightTheme, darkTheme)
        } else {
            Result.failure(currentFailure())
        }
    }

    override suspend fun autoDetectLanguage(code: String): Result<String> {
        return delegate?.autoDetectLanguage(code) ?: Result.failure(currentFailure())
    }

    override suspend fun listLanguages(): List<HighlightLanguageInfo> {
        val currentDelegate = delegate ?: throw currentFailure()
        return currentDelegate.listLanguages()
    }

    override fun close() {
        delegate?.close()
        delegate = null
        failure = null
    }

    private fun currentFailure(): HighlightException {
        return failure ?: HighlightException.EngineNotReady(name)
    }
}
