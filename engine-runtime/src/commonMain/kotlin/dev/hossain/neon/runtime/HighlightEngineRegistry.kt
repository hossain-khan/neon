package dev.hossain.neon.runtime

import dev.hossain.neon.core.AnyHighlightEngineProvider
import dev.hossain.neon.core.HighlightEngineDescriptor
import dev.hossain.neon.core.HighlightEngineId

public class HighlightEngineRegistry private constructor(
    private val providersById: Map<HighlightEngineId, AnyHighlightEngineProvider>,
) {
    public val descriptors: List<HighlightEngineDescriptor>
        get() = providersById.values.map { it.descriptor }

    public fun contains(engineId: HighlightEngineId): Boolean = providersById.containsKey(engineId)

    public fun provider(engineId: HighlightEngineId): AnyHighlightEngineProvider? = providersById[engineId]

    public fun requireProvider(engineId: HighlightEngineId): AnyHighlightEngineProvider {
        return provider(engineId) ?: error("No highlight engine provider registered for '${engineId.value}'")
    }

    public fun isAvailable(engineId: HighlightEngineId): Boolean {
        return provider(engineId)?.isAvailable() == true
    }

    public companion object {
        public fun of(vararg providers: AnyHighlightEngineProvider): HighlightEngineRegistry {
            return HighlightEngineRegistry(
                providers.associateBy { it.descriptor.id }
            )
        }
    }
}
