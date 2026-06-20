package dev.hossain.neon.core

public interface HighlightEngineFactory {
    public val name: String
    public fun isAvailable(): Boolean
    public suspend fun create(config: EngineConfig): HighlightEngine
}

public interface EngineConfig
