package dev.hossain.neon.engine.shiki

import dev.hossain.neon.core.BaseHighlightEngineProvider
import dev.hossain.neon.core.EngineConfig
import dev.hossain.neon.core.HighlightEngine
import dev.hossain.neon.core.HighlightEngineCapabilities
import dev.hossain.neon.core.HighlightEngineDescriptor
import dev.hossain.neon.core.HighlightEngineId

public object ShikiNetworkEngineProvider : BaseHighlightEngineProvider<ShikiNetworkConfig>() {
    override val descriptor: HighlightEngineDescriptor = HighlightEngineDescriptor(
        id = HighlightEngineId("shiki-network"),
        displayName = "Shiki (Network)",
        capabilities = HighlightEngineCapabilities(
            supportsDualTheme = true,
            requiresNetworkAccess = true,
        ),
    )

    override fun isAvailable(): Boolean = true

    override fun accepts(config: EngineConfig): Boolean = config is ShikiNetworkConfig

    override suspend fun createTyped(config: ShikiNetworkConfig): HighlightEngine {
        val engine = ShikiNetworkEngine(config)
        engine.initSupportedLanguages()
        return engine
    }
}
