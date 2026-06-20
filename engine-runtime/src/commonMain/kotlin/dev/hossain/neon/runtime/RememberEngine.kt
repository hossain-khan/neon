package dev.hossain.neon.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import dev.hossain.neon.core.AnyHighlightEngineProvider
import dev.hossain.neon.core.EngineConfig
import dev.hossain.neon.core.HighlightEngine
import dev.hossain.neon.core.HighlightEngineProvider
import dev.hossain.neon.core.HighlightException
import dev.hossain.neon.core.HighlightLanguageInfo
import dev.hossain.neon.core.HighlightResult
import dev.hossain.neon.core.HighlightTheme
import dev.hossain.neon.core.HighlightTimings
import dev.hossain.neon.core.ThemedHighlightResult

@Composable
public fun <C : EngineConfig> rememberEngine(
    provider: HighlightEngineProvider<C>,
    config: C,
): HighlightEngine {
    val delegatingEngine = remember(provider, config) { DelegatingHighlightEngine(provider.descriptor.id.value) }
    LaunchedEffect(provider, config) {
        try {
            val engine = provider.createTyped(config)
            delegatingEngine.setDelegate(engine)
        } catch (e: Exception) {
            val failure = e as? HighlightException
                ?: HighlightException.EngineInitializationFailed(provider.descriptor.id.value, e)
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

@Composable
public fun rememberRegisteredEngine(
    registry: HighlightEngineRegistry,
    engineId: dev.hossain.neon.core.HighlightEngineId,
    config: EngineConfig,
): HighlightEngine {
    val provider = remember(registry, engineId) { registry.requireProvider(engineId) }
    return rememberEngine(provider, config)
}

@Composable
public fun rememberRegisteredEngine(
    registry: HighlightEngineRegistry,
    engineId: dev.hossain.neon.core.HighlightEngineId,
    configResolver: (dev.hossain.neon.core.HighlightEngineId) -> EngineConfig,
): HighlightEngine {
    return rememberRegisteredEngine(
        registry = registry,
        engineId = engineId,
        config = configResolver(engineId),
    )
}

@Composable
private fun rememberEngine(
    provider: AnyHighlightEngineProvider,
    config: EngineConfig,
): HighlightEngine {
    val delegatingEngine = remember(provider, config) { DelegatingHighlightEngine(provider.descriptor.id.value) }
    LaunchedEffect(provider, config) {
        try {
            val engine = provider.create(config)
            delegatingEngine.setDelegate(engine)
        } catch (e: Exception) {
            val failure = e as? HighlightException
                ?: HighlightException.EngineInitializationFailed(provider.descriptor.id.value, e)
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

private class DelegatingHighlightEngine(
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
