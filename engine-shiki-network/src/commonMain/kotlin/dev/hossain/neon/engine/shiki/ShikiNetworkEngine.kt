package dev.hossain.neon.engine.shiki

import dev.hossain.neon.core.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

public class ShikiNetworkEngine(
    private val config: ShikiNetworkConfig
) : HighlightEngine {

    override val name: String = "shiki-network"

    private val client = (config.httpClient ?: HttpClient()).config {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = config.timeout.inWholeMilliseconds
            connectTimeoutMillis = config.timeout.inWholeMilliseconds
            socketTimeoutMillis = config.timeout.inWholeMilliseconds
        }
    }

    private var _supportedLanguages: Set<String>? = null

    override val supportedLanguages: Set<String>
        get() = _supportedLanguages ?: emptySet()

    internal suspend fun initSupportedLanguages() {
        try {
            val response: LanguagesResponse = client.get("${config.serviceUrl}/languages").body()
            _supportedLanguages = response.languages.toSet()
        } catch (e: Exception) {
            _supportedLanguages = emptySet()
        }
    }

    override suspend fun highlight(
        code: String,
        language: String,
        theme: HighlightTheme
    ): Result<HighlightResult> = runCatching {
        val startTotal = TimeSource.Monotonic.markNow()

        val httpStart = TimeSource.Monotonic.markNow()
        val response = client.post("${config.serviceUrl}/highlight") {
            contentType(ContentType.Application.Json)
            setBody(HighlightRequest(
                code = code,
                language = language,
                theme = theme.name,
                debug = true
            ))
        }

        if (!response.status.isSuccess()) {
            handleErrorResponse(response, language)
        }

        val highlightResponse: HighlightResponse = response.body()
        val httpDuration = httpStart.elapsedNow()

        val parseStart = TimeSource.Monotonic.markNow()
        val flatTokens = mutableListOf<HighlightToken>()
        for (i in highlightResponse.tokens.indices) {
            if (i > 0) {
                flatTokens.add(HighlightToken(text = "\n"))
            }
            val lineTokens = highlightResponse.tokens[i]
            for (t in lineTokens) {
                flatTokens.add(
                    HighlightToken(
                        text = t.text,
                        color = t.color,
                        background = null,
                        fontWeight = null,
                        fontStyle = null,
                        isUnderline = false
                    )
                )
            }
        }
        val parseDuration = parseStart.elapsedNow()
        val totalDuration = startTotal.elapsedNow()

        val timings = HighlightTimings(
            jsBridge = httpDuration,
            jsonUnescape = 0.milliseconds,
            htmlParse = parseDuration,
            themeParse = 0.milliseconds,
            total = totalDuration
        )

        HighlightResult(
            tokens = flatTokens,
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
        val startTotal = TimeSource.Monotonic.markNow()

        val httpStart = TimeSource.Monotonic.markNow()
        val response = client.post("${config.serviceUrl}/highlight/dual") {
            contentType(ContentType.Application.Json)
            setBody(HighlightDualRequest(
                code = code,
                language = language,
                darkTheme = darkTheme.name,
                lightTheme = lightTheme.name,
                debug = true
            ))
        }

        if (!response.status.isSuccess()) {
            handleErrorResponse(response, language)
        }

        val highlightResponse: HighlightDualResponse = response.body()
        val httpDuration = httpStart.elapsedNow()

        val parseStart = TimeSource.Monotonic.markNow()
        val lightTokens = mutableListOf<HighlightToken>()
        val darkTokens = mutableListOf<HighlightToken>()

        for (i in highlightResponse.tokens.indices) {
            if (i > 0) {
                lightTokens.add(HighlightToken(text = "\n"))
                darkTokens.add(HighlightToken(text = "\n"))
            }
            val lineTokens = highlightResponse.tokens[i]
            for (t in lineTokens) {
                lightTokens.add(
                    HighlightToken(
                        text = t.text,
                        color = t.lightColor,
                        background = null,
                        fontWeight = null,
                        fontStyle = null,
                        isUnderline = false
                    )
                )
                darkTokens.add(
                    HighlightToken(
                        text = t.text,
                        color = t.darkColor,
                        background = null,
                        fontWeight = null,
                        fontStyle = null,
                        isUnderline = false
                    )
                )
            }
        }
        val parseDuration = parseStart.elapsedNow()
        val totalDuration = startTotal.elapsedNow()

        val timings = HighlightTimings(
            jsBridge = httpDuration,
            jsonUnescape = 0.milliseconds,
            htmlParse = parseDuration,
            themeParse = 0.milliseconds,
            total = totalDuration
        )

        ThemedHighlightResult(
            light = HighlightResult(lightTokens, language, timings),
            dark = HighlightResult(darkTokens, language, timings)
        )
    }

    override suspend fun autoDetectLanguage(code: String): Result<String> = runCatching {
        "text"
    }

    override suspend fun listLanguages(): List<HighlightLanguageInfo> {
        return supportedLanguages.map { langId ->
            HighlightLanguageInfo(
                id = langId,
                name = langId.replaceFirstChar { it.uppercase() },
                aliases = emptyList()
            )
        }
    }

    private suspend fun handleErrorResponse(response: HttpResponse, language: String): Nothing {
        val errorText = runCatching { response.bodyAsText() }.getOrDefault("")
        val errorMsg = try {
            val element = Json.parseToJsonElement(errorText)
            element.jsonObject["error"]?.jsonPrimitive?.content ?: errorText
        } catch (e: Exception) {
            errorText
        }

        if (response.status == HttpStatusCode.BadRequest && (errorMsg.contains("Unsupported language") || errorMsg.contains("invalid language"))) {
            throw HighlightException.UnsupportedLanguage(language, "shiki-network")
        }
        throw HighlightException.NetworkError(Exception("Shiki API returned status ${response.status}: $errorMsg"))
    }

    override fun close() {
        client.close()
    }
}

@Serializable
private data class HighlightRequest(
    val code: String,
    val language: String,
    val theme: String,
    val debug: Boolean = true,
)

@Serializable
private data class ShikiToken(
    val text: String,
    val color: String,
)

@Serializable
private data class HighlightResponse(
    val language: String,
    val theme: String,
    val tokens: List<List<ShikiToken>>,
    val _debug: DebugInfo? = null,
)

@Serializable
private data class DebugInfo(
    val totalMs: Double,
    val tokenizerMs: Double? = null,
    val requestBodyBytes: Int? = null,
)

@Serializable
private data class HighlightDualRequest(
    val code: String,
    val language: String,
    val darkTheme: String,
    val lightTheme: String,
    val debug: Boolean = true,
)

@Serializable
private data class ShikiDualToken(
    val text: String,
    val darkColor: String,
    val lightColor: String,
)

@Serializable
private data class HighlightDualResponse(
    val language: String,
    val darkTheme: String,
    val lightTheme: String,
    val tokens: List<List<ShikiDualToken>>,
    val _debug: DebugInfo? = null,
)

@Serializable
private data class LanguagesResponse(
    val languages: List<String>,
    val themes: List<String>,
)
