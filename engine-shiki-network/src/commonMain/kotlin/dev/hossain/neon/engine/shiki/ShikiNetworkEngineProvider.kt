package dev.hossain.neon.engine.shiki

import dev.hossain.neon.core.BaseHighlightEngineProvider
import dev.hossain.neon.core.EngineConfig
import dev.hossain.neon.core.HighlightEngine
import dev.hossain.neon.core.HighlightEngineCapabilities
import dev.hossain.neon.core.HighlightEngineDescriptor
import dev.hossain.neon.core.HighlightEngineId
import dev.hossain.neon.core.HighlightTarget
import dev.hossain.neon.core.HighlightThemeCatalog
import dev.hossain.neon.core.HighlightThemeDescriptor

public object ShikiNetworkEngineProvider : BaseHighlightEngineProvider<ShikiNetworkConfig>() {
    override val descriptor: HighlightEngineDescriptor = HighlightEngineDescriptor(
        id = HighlightEngineId("shiki-network"),
        displayName = "Shiki (Network)",
        capabilities = HighlightEngineCapabilities(
            supportsDualTheme = true,
            requiresNetworkAccess = true,
        ),
        supportedTargets = setOf(
            HighlightTarget.ANDROID,
            HighlightTarget.IOS,
            HighlightTarget.DESKTOP,
            HighlightTarget.JS,
            HighlightTarget.WASM_JS,
        ),
    )

    override val themeCatalog: HighlightThemeCatalog = object : HighlightThemeCatalog {
        private val builtinThemes = listOf(
            "github-dark" to true,
            "github-light" to false,
            "one-dark-pro" to true,
            "dracula" to true,
            "min-light" to false,
        )

        override val themes: List<HighlightThemeDescriptor> = builtinThemes.map { (id, isDark) ->
            HighlightThemeDescriptor(
                id = id,
                displayName = id,
                isDark = isDark,
            )
        }

        override val defaultThemeId: String = "github-dark"

        override suspend fun loadTheme(themeId: String): ShikiTheme = ShikiTheme.builtin(themeId)
    }

    override fun isAvailable(): Boolean = true

    override fun accepts(config: EngineConfig): Boolean = config is ShikiNetworkConfig

    override suspend fun createTyped(config: ShikiNetworkConfig): HighlightEngine {
        val engine = ShikiNetworkEngine(config)
        engine.initSupportedLanguages()
        return engine
    }
}
