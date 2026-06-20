package dev.hossain.neon.engine.highlightjs

import dev.hossain.neon.core.BaseHighlightEngineProvider
import dev.hossain.neon.core.EngineConfig
import dev.hossain.neon.core.HighlightEngine
import dev.hossain.neon.core.HighlightEngineCapabilities
import dev.hossain.neon.core.HighlightEngineDescriptor
import dev.hossain.neon.core.HighlightEngineId
import dev.hossain.neon.core.HighlightException
import dev.hossain.neon.engine.highlightjs.internal.JsRuntime
import neonproject.engine_highlightjs.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

public object HljsEngineProvider : BaseHighlightEngineProvider<HljsConfig>() {
    override val descriptor: HighlightEngineDescriptor = HighlightEngineDescriptor(
        id = HighlightEngineId("highlightjs"),
        displayName = "Highlight.js",
        capabilities = HighlightEngineCapabilities(
            supportsAutoDetect = true,
            supportsDualTheme = true,
        ),
    )

    override fun isAvailable(): Boolean = true

    override fun accepts(config: EngineConfig): Boolean = config is HljsConfig

    @OptIn(ExperimentalResourceApi::class)
    override suspend fun createTyped(config: HljsConfig): HighlightEngine {
        val runtime = JsRuntime()
        try {
            val bridgeBytes = Res.readBytes("files/bridge.html")
            val jsBytes = Res.readBytes("files/highlight.min.js")
            val bridgeHtml = bridgeBytes.decodeToString()
            val highlightJs = jsBytes.decodeToString()

            val mergedHtml = bridgeHtml.replace(
                "<script src=\"highlight.min.js\"></script>",
                "<script>\n$highlightJs\n</script>"
            )

            runtime.initialize(mergedHtml)
            val engine = HljsEngine(runtime)
            engine.initSupportedLanguages()
            return engine
        } catch (e: Exception) {
            runtime.close()
            throw HighlightException.EngineInitializationFailed(descriptor.id.value, e)
        }
    }
}
