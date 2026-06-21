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
    return rememberManagedEngine(
        engineKey = config,
        providerId = provider.descriptor.id.value,
        loadEngine = { provider.createTyped(config) },
    )
}

public data class HighlightEngineSelection(
    val engineId: HighlightEngineId,
    val config: EngineConfig,
)

@Composable
public fun rememberEngine(
    registry: HighlightEngineRegistry,
    selection: HighlightEngineSelection,
): HighlightEngine {
    val provider = remember(registry, selection.engineId) { registry.requireProvider(selection.engineId) }
    return rememberManagedEngine(
        engineKey = selection,
        providerId = provider.descriptor.id.value,
        loadEngine = { registry.createEngine(selection) },
    )
}

@Composable
public fun rememberEngine(
    registry: HighlightEngineRegistry,
    engineId: HighlightEngineId,
    configResolver: (HighlightEngineId) -> EngineConfig,
): HighlightEngine {
    return rememberEngine(
        registry = registry,
        selection = registry.selection(engineId, configResolver(engineId)),
    )
}

@Composable
private fun rememberManagedEngine(
    engineKey: Any?,
    providerId: String,
    loadEngine: suspend () -> HighlightEngine,
): HighlightEngine {
    val delegatingEngine = remember(providerId, engineKey) { ManagedHighlightEngine(providerId) }
    LaunchedEffect(providerId, engineKey) {
        try {
            delegatingEngine.setDelegate(loadEngine())
        } catch (e: Exception) {
            val failure = e as? HighlightException
                ?: HighlightException.EngineInitializationFailed(providerId, e)
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
private fun rememberEngine(
    provider: AnyHighlightEngineProvider,
    config: EngineConfig,
): HighlightEngine {
    return rememberManagedEngine(
        engineKey = config,
        providerId = provider.descriptor.id.value,
        loadEngine = { provider.create(config) },
    )
}
