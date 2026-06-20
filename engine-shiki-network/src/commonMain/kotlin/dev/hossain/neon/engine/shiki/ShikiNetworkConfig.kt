package dev.hossain.neon.engine.shiki

import dev.hossain.neon.core.EngineConfig
import io.ktor.client.HttpClient
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

public data class ShikiNetworkConfig(
    val serviceUrl: String,
    val timeout: Duration = 5.seconds,
    val httpClient: HttpClient? = null,
    val enableLogging: Boolean = false,
    val sendDebugMetadata: Boolean = false,
) : EngineConfig {
    public companion object {
        public val Default: ShikiNetworkConfig = ShikiNetworkConfig(
            serviceUrl = "https://syntax-highlight.gohk.xyz"
        )
    }
}
