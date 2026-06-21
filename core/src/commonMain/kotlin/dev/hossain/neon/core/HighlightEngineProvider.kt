package dev.hossain.neon.core

import kotlin.jvm.JvmInline

public interface EngineConfig

@JvmInline
public value class HighlightEngineId(public val value: String)

public enum class HighlightTarget {
    ANDROID,
    IOS,
    DESKTOP,
    JS,
    WASM_JS,
}

public data class HighlightEngineCapabilities(
    val supportsAutoDetect: Boolean = false,
    val supportsDualTheme: Boolean = false,
    val requiresNetworkAccess: Boolean = false,
)

public data class HighlightThemeDescriptor(
    val id: String,
    val displayName: String,
    val isDark: Boolean,
)

public interface HighlightThemeCatalog {
    public val themes: List<HighlightThemeDescriptor>
    public val defaultThemeId: String

    public suspend fun loadTheme(themeId: String): HighlightTheme
}

public data class HighlightEngineDescriptor(
    val id: HighlightEngineId,
    val displayName: String,
    val capabilities: HighlightEngineCapabilities = HighlightEngineCapabilities(),
    val supportedTargets: Set<HighlightTarget>,
)

public interface AnyHighlightEngineProvider {
    public val descriptor: HighlightEngineDescriptor
    public val themeCatalog: HighlightThemeCatalog? get() = null
    public fun isAvailable(): Boolean
    public suspend fun create(config: EngineConfig): HighlightEngine
}

public interface HighlightEngineProvider<C : EngineConfig> : AnyHighlightEngineProvider {
    public suspend fun createTyped(config: C): HighlightEngine
}

public abstract class BaseHighlightEngineProvider<C : EngineConfig> : HighlightEngineProvider<C> {
    public abstract fun accepts(config: EngineConfig): Boolean

    final override suspend fun create(config: EngineConfig): HighlightEngine {
        if (!accepts(config)) {
            throw IllegalArgumentException(
                "Provider '${descriptor.id.value}' cannot create an engine from config ${config::class}"
            )
        }

        @Suppress("UNCHECKED_CAST")
        return createTyped(config as C)
    }
}
