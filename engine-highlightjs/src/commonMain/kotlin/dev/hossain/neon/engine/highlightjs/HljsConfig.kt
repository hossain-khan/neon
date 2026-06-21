package dev.hossain.neon.engine.highlightjs

import dev.hossain.neon.core.EngineConfig

public data class HljsConfig(
    internal val platformContext: Any? = null,
) : EngineConfig {
    public companion object {
        public val Default: HljsConfig = HljsConfig()
    }
}
