package dev.hossain.neon.engine.highlightjs

import dev.hossain.neon.core.*
import dev.hossain.neon.core.internal.escapeForJs
import dev.hossain.neon.core.internal.unescapeJsString
import dev.hossain.neon.engine.highlightjs.internal.HtmlParser
import dev.hossain.neon.engine.highlightjs.internal.JsRuntime
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

public class HljsEngine internal constructor(
    private val runtime: JsRuntime
) : HighlightEngine {

    override val name: String = "highlightjs"

    private val mutex = Mutex()
    private var _supportedLanguages: Set<String>? = null

    override val supportedLanguages: Set<String>
        get() = _supportedLanguages ?: emptySet()

    internal suspend fun initSupportedLanguages() {
        try {
            val json = runtime.evaluate("JSON.stringify(listLanguages())")
            val unescaped = unescapeJsString(json)
            val jsonArray = Json.parseToJsonElement(unescaped).jsonArray
            _supportedLanguages = jsonArray.map { it.jsonPrimitive.content }.toSet()
        } catch (e: Exception) {
            _supportedLanguages = emptySet()
        }
    }

    override suspend fun highlight(
        code: String,
        language: String,
        theme: HighlightTheme
    ): Result<HighlightResult> = runCatching {
        if (theme !is HljsTheme) {
            throw HighlightException.ThemeLoadFailed(theme.name, Exception("Theme must be an instance of HljsTheme"))
        }

        val startTotal = TimeSource.Monotonic.markNow()

        val escapeStart = TimeSource.Monotonic.markNow()
        val escapedCode = escapeForJs(code)
        val escapedLang = escapeForJs(language)
        val script = "highlightCode('$escapedCode', '$escapedLang')"
        val escapeDuration = escapeStart.elapsedNow()

        val bridgeStart = TimeSource.Monotonic.markNow()
        val jsonResult = mutex.withLock {
            runtime.evaluate(script)
        }
        val bridgeDuration = bridgeStart.elapsedNow()

        val unescapeStart = TimeSource.Monotonic.markNow()
        val unescapedJson = unescapeJsString(jsonResult)
        val unescapeDuration = unescapeStart.elapsedNow()

        val parseStart = TimeSource.Monotonic.markNow()
        val element = Json.parseToJsonElement(unescapedJson).jsonObject
        val isError = element["error"]?.jsonPrimitive?.booleanOrNull ?: false
        if (isError) {
            val errorMsg = element["message"]?.jsonPrimitive?.content ?: "Unknown highlightjs error"
            if (errorMsg.contains("Unknown language") || errorMsg.contains("Unsupported language")) {
                throw HighlightException.UnsupportedLanguage(language, "highlightjs")
            }
            throw HighlightException.JavaScriptEvaluationFailed(Exception(errorMsg))
        }
        val html = element["html"]?.jsonPrimitive?.content ?: ""
        val parseDuration = parseStart.elapsedNow()

        val htmlParseStart = TimeSource.Monotonic.markNow()
        val tokens = HtmlParser.parse(html, theme.colorMap)
        val htmlParseDuration = htmlParseStart.elapsedNow()

        val totalDuration = startTotal.elapsedNow()

        val timings = HighlightTimings(
            jsBridge = bridgeDuration,
            jsonUnescape = escapeDuration + unescapeDuration + parseDuration,
            htmlParse = htmlParseDuration,
            themeParse = 0.milliseconds,
            total = totalDuration
        )

        HighlightResult(
            tokens = tokens,
            language = language,
            timings = timings
        )
    }

    override suspend fun highlightBoth(
        code: String,
        language: String,
        lightTheme: HighlightTheme,
        darkTheme: HighlightTheme
    ): Result<ThemedHighlightResult> = runCatching {
        val lightResult = highlight(code, language, lightTheme).getOrThrow()
        val darkResult = highlight(code, language, darkTheme).getOrThrow()
        ThemedHighlightResult(lightResult, darkResult)
    }

    override suspend fun autoDetectLanguage(code: String): Result<String> = runCatching {
        val escapedCode = escapeForJs(code)
        val script = "highlightAuto('$escapedCode')"
        val jsonResult = mutex.withLock {
            runtime.evaluate(script)
        }
        val unescapedJson = unescapeJsString(jsonResult)
        val element = Json.parseToJsonElement(unescapedJson).jsonObject
        val isError = element["error"]?.jsonPrimitive?.booleanOrNull ?: false
        if (isError) {
            val errorMsg = element["message"]?.jsonPrimitive?.content ?: "Unknown highlightjs error"
            throw HighlightException.JavaScriptEvaluationFailed(Exception(errorMsg))
        }
        element["language"]?.jsonPrimitive?.content ?: ""
    }

    override suspend fun listLanguages(): List<HighlightLanguageInfo> {
        val list = supportedLanguages.toList()
        return list.map { langId ->
            val script = "getLanguage('$langId')"
            val jsonResult = mutex.withLock {
                runtime.evaluate(script)
            }
            val unescapedJson = unescapeJsString(jsonResult)
            if (unescapedJson != "null" && unescapedJson.isNotBlank()) {
                try {
                    val element = Json.parseToJsonElement(unescapedJson).jsonObject
                    val name = element["name"]?.jsonPrimitive?.content ?: langId
                    val aliases = element["aliases"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                    HighlightLanguageInfo(langId, name, aliases)
                } catch (e: Exception) {
                    HighlightLanguageInfo(langId, langId, emptyList())
                }
            } else {
                HighlightLanguageInfo(langId, langId, emptyList())
            }
        }
    }

    override fun close() {
        runtime.close()
    }
}
