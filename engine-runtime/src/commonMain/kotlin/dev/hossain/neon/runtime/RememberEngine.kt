package dev.hossain.neon.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import dev.hossain.neon.core.AnyHighlightEngineProvider
import dev.hossain.neon.core.EngineConfig
import dev.hossain.neon.core.HighlightEngine
import dev.hossain.neon.core.HighlightEngineId
import dev.hossain.neon.core.HighlightEngineProvider
import dev.hossain.neon.core.HighlightException

@Composable
public fun <C : EngineConfig> rememberEngine(
    provider: HighlightEngineProvider<C>,
    config: C,
): HighlightEngine {
    val delegatingEngine = remember(provider, config) { ManagedHighlightEngine(provider.descriptor.id.value) }
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

public data class RegisteredEngineSelection(
    val engineId: HighlightEngineId,
    val config: EngineConfig,
)

@Composable
public fun rememberRegisteredEngine(
    registry: HighlightEngineRegistry,
    selection: RegisteredEngineSelection,
): HighlightEngine {
    val provider = remember(registry, selection.engineId) { registry.requireProvider(selection.engineId) }
    return rememberEngine(provider, selection.config)
}

@Composable
public fun rememberRegisteredEngine(
    registry: HighlightEngineRegistry,
    engineId: HighlightEngineId,
    configResolver: (HighlightEngineId) -> EngineConfig,
): HighlightEngine {
    return rememberRegisteredEngine(
        registry = registry,
        selection = RegisteredEngineSelection(
            engineId = engineId,
            config = configResolver(engineId),
        ),
    )
}

@Composable
private fun rememberEngine(
    provider: AnyHighlightEngineProvider,
    config: EngineConfig,
): HighlightEngine {
    val delegatingEngine = remember(provider, config) { ManagedHighlightEngine(provider.descriptor.id.value) }
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
