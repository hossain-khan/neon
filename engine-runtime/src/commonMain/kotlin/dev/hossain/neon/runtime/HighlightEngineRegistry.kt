package dev.hossain.neon.runtime

import dev.hossain.neon.core.AnyHighlightEngineProvider
import dev.hossain.neon.core.EngineConfig
import dev.hossain.neon.core.HighlightEngine
import dev.hossain.neon.core.HighlightEngineDescriptor
import dev.hossain.neon.core.HighlightEngineId
import dev.hossain.neon.core.HighlightTheme
import dev.hossain.neon.core.HighlightThemeCatalog
import dev.hossain.neon.core.HighlightThemeDescriptor

public class HighlightEngineRegistry private constructor(
    private val providersById: Map<HighlightEngineId, AnyHighlightEngineProvider>,
) {
    public val providers: List<AnyHighlightEngineProvider>
        get() = providersById.values.toList()

    public val descriptors: List<HighlightEngineDescriptor>
        get() = providers.map { it.descriptor }

    public fun contains(engineId: HighlightEngineId): Boolean = providersById.containsKey(engineId)

    public fun provider(engineId: HighlightEngineId): AnyHighlightEngineProvider? = providersById[engineId]

    public fun requireProvider(engineId: HighlightEngineId): AnyHighlightEngineProvider {
        return provider(engineId) ?: error("No highlight engine provider registered for '${engineId.value}'")
    }

    public fun selection(
        engineId: HighlightEngineId,
        config: EngineConfig,
    ): HighlightEngineSelection {
        requireProvider(engineId)
        return HighlightEngineSelection(
            engineId = engineId,
            config = config,
        )
    }

    public fun selection(
        provider: AnyHighlightEngineProvider,
        config: EngineConfig,
    ): HighlightEngineSelection {
        return selection(
            engineId = provider.descriptor.id,
            config = config,
        )
    }

    public fun isAvailable(engineId: HighlightEngineId): Boolean {
        return provider(engineId)?.isAvailable() == true
    }

    public fun themeDescriptors(engineId: HighlightEngineId): List<HighlightThemeDescriptor> {
        return provider(engineId)?.themeCatalog?.themes.orEmpty()
    }

    public fun defaultThemeId(engineId: HighlightEngineId): String? {
        return provider(engineId)?.themeCatalog?.defaultThemeId
    }

    public fun requireThemeCatalog(engineId: HighlightEngineId): HighlightThemeCatalog {
        return requireProvider(engineId).themeCatalog
            ?: error("No theme catalog registered for '${engineId.value}'")
    }

    public suspend fun loadTheme(
        engineId: HighlightEngineId,
        themeId: String,
    ): HighlightTheme {
        return requireThemeCatalog(engineId).loadTheme(themeId)
    }

    public suspend fun createEngine(selection: HighlightEngineSelection): HighlightEngine {
        return requireProvider(selection.engineId).create(selection.config)
    }

    public companion object {
        public fun of(vararg providers: AnyHighlightEngineProvider): HighlightEngineRegistry {
            return HighlightEngineRegistry(
                providers.associateBy { it.descriptor.id }
            )
        }
    }
}
