package dev.hossain.neon.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import dev.hossain.neon.core.EngineConfig
import dev.hossain.neon.core.HighlightEngine
import dev.hossain.neon.core.HighlightEngineFactory
import dev.hossain.neon.core.HighlightException
import dev.hossain.neon.core.HighlightLanguageInfo
import dev.hossain.neon.core.HighlightResult
import dev.hossain.neon.core.HighlightTimings
import dev.hossain.neon.core.HighlightToken
import dev.hossain.neon.core.ThemedHighlightResult
import kotlin.time.Duration

public val LocalHighlightEngine: androidx.compose.runtime.ProvidableCompositionLocal<HighlightEngine> = staticCompositionLocalOf {
    error("No HighlightEngine provided. Wrap with HighlightEngineProvider {}")
}

@Composable
public fun HighlightEngineProvider(
    engine: HighlightEngine,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalHighlightEngine provides engine
    ) {
        content()
    }
}

@Composable
public fun rememberHighlightEngine(
    factory: HighlightEngineFactory,
    config: EngineConfig,
): HighlightEngine {
    val delegatingEngine = remember(factory, config) { DelegatingHighlightEngine(factory.name) }
    LaunchedEffect(factory, config) {
        try {
            val engine = factory.create(config)
            delegatingEngine.setDelegate(engine)
        } catch (e: Exception) {
            val failure = e as? HighlightException
                ?: HighlightException.EngineInitializationFailed(factory.name, e)
            delegatingEngine.setFailure(failure)
        }
    }
    DisposableEffect(delegatingEngine) {
        onDispose {
            delegatingEngine.close()
        }
    }
    return delegatingEngine
}

private class DelegatingHighlightEngine(override val name: String) : HighlightEngine {
    private var delegate: HighlightEngine? = null
    private var failure: HighlightException? = null

    fun setDelegate(engine: HighlightEngine) {
        delegate?.close()
        this.delegate = engine
        this.failure = null
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
        theme: dev.hossain.neon.core.HighlightTheme,
    ): Result<HighlightResult> {
        val d = delegate
        return if (d != null) {
            d.highlight(code, language, theme)
        } else {
            Result.failure(currentFailure())
        }
    }

    override suspend fun highlightBoth(
        code: String,
        language: String,
        lightTheme: dev.hossain.neon.core.HighlightTheme,
        darkTheme: dev.hossain.neon.core.HighlightTheme,
    ): Result<ThemedHighlightResult> {
        val d = delegate
        return if (d != null) {
            d.highlightBoth(code, language, lightTheme, darkTheme)
        } else {
            Result.failure(currentFailure())
        }
    }

    override suspend fun autoDetectLanguage(code: String): Result<String> {
        return delegate?.autoDetectLanguage(code) ?: Result.failure(currentFailure())
    }

    override suspend fun listLanguages(): List<HighlightLanguageInfo> {
        val d = delegate ?: throw currentFailure()
        return d.listLanguages()
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
