package dev.hossain.neon.engine.shiki

import dev.hossain.neon.core.EngineConfig
import dev.hossain.neon.core.HighlightEngine
import dev.hossain.neon.core.HighlightEngineFactory

public class ShikiNetworkEngineFactory : HighlightEngineFactory {
    override val name: String = "shiki-network"

    override fun isAvailable(): Boolean {
        return true
    }

    override suspend fun create(config: EngineConfig): HighlightEngine {
        require(config is ShikiNetworkConfig) { "Expected ShikiNetworkConfig, got ${config::class}" }
        val engine = ShikiNetworkEngine(config)
        engine.initSupportedLanguages()
        return engine
    }
}
