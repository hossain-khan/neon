package dev.hossain.neon.runtime

import dev.hossain.neon.core.BaseHighlightEngineProvider
import dev.hossain.neon.core.EngineConfig
import dev.hossain.neon.core.HighlightEngine
import dev.hossain.neon.core.HighlightEngineCapabilities
import dev.hossain.neon.core.HighlightEngineDescriptor
import dev.hossain.neon.core.HighlightEngineId
import dev.hossain.neon.core.HighlightLanguageInfo
import dev.hossain.neon.core.HighlightResult
import dev.hossain.neon.core.HighlightTheme
import dev.hossain.neon.core.HighlightTimings
import dev.hossain.neon.core.HighlightToken
import dev.hossain.neon.core.ThemedHighlightResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.ZERO

class HighlightEngineRegistryTest {
    @Test
    fun exposesRegisteredDescriptorsById() {
        val registry = HighlightEngineRegistry.of(FakeProvider)

        assertEquals(listOf(FakeProvider.descriptor), registry.descriptors)
        assertEquals(FakeProvider, registry.requireProvider(FakeProvider.descriptor.id))
    }

    @Test
    fun failsForUnknownProviderId() {
        val registry = HighlightEngineRegistry.of(FakeProvider)

        assertFailsWith<IllegalStateException> {
            registry.requireProvider(HighlightEngineId("missing"))
        }
    }
}

private data class FakeConfig(val enabled: Boolean = true) : EngineConfig

private object FakeProvider : BaseHighlightEngineProvider<FakeConfig>() {
    override val descriptor: HighlightEngineDescriptor = HighlightEngineDescriptor(
        id = HighlightEngineId("fake"),
        displayName = "Fake",
        capabilities = HighlightEngineCapabilities()
    )

    override fun isAvailable(): Boolean = true

    override fun accepts(config: EngineConfig): Boolean = config is FakeConfig

    override suspend fun createTyped(config: FakeConfig): HighlightEngine = FakeEngine
}

private object FakeEngine : HighlightEngine {
    override val name: String = "fake"
    override val supportedLanguages: Set<String> = setOf("text")

    override suspend fun highlight(code: String, language: String, theme: HighlightTheme): Result<HighlightResult> {
        return Result.success(HighlightResult(listOf(HighlightToken(code)), language, noTimings()))
    }

    override suspend fun highlightBoth(
        code: String,
        language: String,
        lightTheme: HighlightTheme,
        darkTheme: HighlightTheme,
    ): Result<ThemedHighlightResult> {
        val result = HighlightResult(listOf(HighlightToken(code)), language, noTimings())
        return Result.success(ThemedHighlightResult(result, result))
    }

    override suspend fun autoDetectLanguage(code: String): Result<String> = Result.success("text")

    override suspend fun listLanguages(): List<HighlightLanguageInfo> = listOf(
        HighlightLanguageInfo(id = "text", name = "Text", aliases = emptyList())
    )

    override fun close() = Unit
}

private fun noTimings(): HighlightTimings = HighlightTimings(
    jsBridge = ZERO,
    jsonUnescape = ZERO,
    htmlParse = ZERO,
    themeParse = ZERO,
    total = ZERO,
)
