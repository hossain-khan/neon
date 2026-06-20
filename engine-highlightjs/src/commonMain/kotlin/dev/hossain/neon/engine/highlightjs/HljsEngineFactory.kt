package dev.hossain.neon.engine.highlightjs

import dev.hossain.neon.core.EngineConfig
import dev.hossain.neon.core.HighlightEngine
import dev.hossain.neon.core.HighlightEngineFactory
import dev.hossain.neon.core.HighlightException
import dev.hossain.neon.engine.highlightjs.internal.JsRuntime
import neonproject.engine_highlightjs.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

public object HljsEngineFactory : HighlightEngineFactory {

    override val name: String = "highlightjs"

    override fun isAvailable(): Boolean {
        return true
    }

    @OptIn(ExperimentalResourceApi::class)
    override suspend fun create(config: EngineConfig): HighlightEngine {
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
            throw HighlightException.EngineInitializationFailed("highlightjs", e)
        }
    }
}
