package dev.hossain.neon.engine.shiki

import dev.hossain.neon.core.HighlightTheme
import dev.hossain.neon.core.HighlightToken
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Ignore

class ShikiNetworkEngineTest {

    @Test
    fun testInitSupportedLanguages() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = ByteReadChannel("""{"languages":["kotlin","javascript"],"themes":["github-dark"]}"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val httpClient = HttpClient(mockEngine)
        val config = ShikiNetworkConfig(
            serviceUrl = "https://mock-service",
            httpClient = httpClient
        )
        val engine = ShikiNetworkEngine(config)
        engine.initSupportedLanguages()

        assertEquals(setOf("kotlin", "javascript"), engine.supportedLanguages)
    }

    @Test
    fun testHighlightSingleTheme() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = ByteReadChannel("""
                    {
                        "language": "kotlin",
                        "theme": "github-dark",
                        "tokens": [
                            [
                                {"text": "val", "color": "#FF0000"},
                                {"text": " ", "color": "#000000"},
                                {"text": "x", "color": "#00FF00"}
                            ]
                        ]
                    }
                """.trimIndent()),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val httpClient = HttpClient(mockEngine)
        val config = ShikiNetworkConfig(
            serviceUrl = "https://mock-service",
            httpClient = httpClient
        )
        val engine = ShikiNetworkEngine(config)
        val result = engine.highlight("val x", "kotlin", ShikiTheme.builtin("github-dark")).getOrThrow()

        val expectedTokens = listOf(
            HighlightToken("val", color = "#FF0000"),
            HighlightToken(" ", color = "#000000"),
            HighlightToken("x", color = "#00FF00")
        )
        assertEquals(expectedTokens, result.tokens)
        assertEquals("kotlin", result.language)
    }

    @Test
    fun testHighlightDualTheme() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = ByteReadChannel("""
                    {
                        "language": "kotlin",
                        "darkTheme": "github-dark",
                        "lightTheme": "github-light",
                        "tokens": [
                            [
                                {"text": "val", "darkColor": "#FF0000", "lightColor": "#0000FF"}
                            ]
                        ]
                    }
                """.trimIndent()),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val httpClient = HttpClient(mockEngine)
        val config = ShikiNetworkConfig(
            serviceUrl = "https://mock-service",
            httpClient = httpClient
        )
        val engine = ShikiNetworkEngine(config)
        val result = engine.highlightBoth(
            code = "val",
            language = "kotlin",
            lightTheme = ShikiTheme.builtin("github-light"),
            darkTheme = ShikiTheme.builtin("github-dark")
        ).getOrThrow()

        val expectedLightTokens = listOf(
            HighlightToken("val", color = "#0000FF")
        )
        val expectedDarkTokens = listOf(
            HighlightToken("val", color = "#FF0000")
        )
        assertEquals(expectedLightTokens, result.light.tokens)
        assertEquals(expectedDarkTokens, result.dark.tokens)
    }

    @Ignore
    @Test
    fun testRealHighlight() = runTest {
        val config = ShikiNetworkConfig.Default
        val engine = ShikiNetworkEngine(config)
        engine.initSupportedLanguages()
        println("Supported languages from real server: ${engine.supportedLanguages}")
        val result = engine.highlight("val x = 42", "kotlin", ShikiTheme.builtin("github-dark"))
        if (result.isFailure) {
            val err = result.exceptionOrNull()
            println("Real highlight failed with: $err")
            err?.printStackTrace()
            throw err!!
        }
        val highlightResult = result.getOrThrow()
        println("Real highlight result tokens: ${highlightResult.tokens.size}")
        assertTrue(highlightResult.tokens.isNotEmpty())
    }
}
