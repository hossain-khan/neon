package dev.hossain.neon.runtime

import dev.hossain.neon.core.BaseHighlightEngineProvider
import dev.hossain.neon.core.EngineConfig
import dev.hossain.neon.core.HighlightEngine
import dev.hossain.neon.core.HighlightEngineCapabilities
import dev.hossain.neon.core.HighlightEngineDescriptor
import dev.hossain.neon.core.HighlightEngineId
import dev.hossain.neon.core.HighlightLanguageInfo
import dev.hossain.neon.core.HighlightTarget
import dev.hossain.neon.core.HighlightResult
import dev.hossain.neon.core.HighlightThemeCatalog
import dev.hossain.neon.core.HighlightThemeDescriptor
import dev.hossain.neon.core.HighlightTheme
import dev.hossain.neon.core.HighlightException
import dev.hossain.neon.core.HighlightTimings
import dev.hossain.neon.core.HighlightToken
import dev.hossain.neon.core.ThemedHighlightResult
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue
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

    @Test
    fun exposesRegisteredProviders() {
        val registry = HighlightEngineRegistry.of(FakeProvider)

        assertEquals(listOf(FakeProvider), registry.providers)
        assertTrue(registry.contains(FakeProvider.descriptor.id))
    }

    @Test
    fun createsSelectionFromRegisteredProvider() {
        val registry = HighlightEngineRegistry.of(FakeProvider)

        val selection = registry.selection(FakeProvider.descriptor.id, FakeConfig())

        assertEquals(FakeProvider.descriptor.id, selection.engineId)
    }

    @Test
    fun exposesThemeCatalogThroughRegistryHelpers() {
        val registry = HighlightEngineRegistry.of(FakeProvider)

        assertEquals("fake-dark", registry.defaultThemeId(FakeProvider.descriptor.id))
        assertEquals(
            listOf(HighlightThemeDescriptor(id = "fake-dark", displayName = "Fake Dark", isDark = true)),
            registry.themeDescriptors(FakeProvider.descriptor.id),
        )
    }

    @Test
    fun createsEngineFromSelection() {
        val registry = HighlightEngineRegistry.of(FakeProvider)
        val selection = registry.selection(FakeProvider.descriptor.id, FakeConfig())

        val engine = runSuspend { registry.createEngine(selection) }

        assertSame(FakeEngine, engine)
    }

    @Test
    fun loadsThemeFromSelectionProvider() {
        val registry = HighlightEngineRegistry.of(FakeProvider)

        val theme = runSuspend {
            registry.loadTheme(
                engineId = FakeProvider.descriptor.id,
                themeId = "fake-dark",
            )
        }

        assertEquals("fake-dark", theme.name)
        assertTrue(theme.isDark)
    }

    @Test
    fun managedEngineReplacesAndClosesPreviousDelegate() {
        val managed = ManagedHighlightEngine("managed")
        val first = ClosableFakeEngine("first")
        val second = ClosableFakeEngine("second")

        managed.setDelegate(first)
        managed.setDelegate(second)

        assertTrue(first.closed)
        assertEquals(setOf("text"), managed.supportedLanguages)
    }

    @Test
    fun managedEnginePropagatesFailureWhenUninitialized() {
        val managed = ManagedHighlightEngine("managed")
        val failure = HighlightException.EngineNotReady("managed")

        managed.setFailure(failure)

        val thrown = assertFailsWith<HighlightException> {
            runSuspend { managed.listLanguages() }
        }
        assertSame(failure, thrown)
    }
}

private data class FakeConfig(val enabled: Boolean = true) : EngineConfig

private object FakeProvider : BaseHighlightEngineProvider<FakeConfig>() {
    override val descriptor: HighlightEngineDescriptor = HighlightEngineDescriptor(
        id = HighlightEngineId("fake"),
        displayName = "Fake",
        capabilities = HighlightEngineCapabilities(),
        supportedTargets = setOf(HighlightTarget.DESKTOP),
    )

    override val themeCatalog: HighlightThemeCatalog = object : HighlightThemeCatalog {
        override val themes: List<HighlightThemeDescriptor> = listOf(
            HighlightThemeDescriptor(id = "fake-dark", displayName = "Fake Dark", isDark = true)
        )
        override val defaultThemeId: String = "fake-dark"
        override suspend fun loadTheme(themeId: String): HighlightTheme = object : HighlightTheme {
            override val name: String = themeId
            override val isDark: Boolean = true
        }
    }

    override fun isAvailable(): Boolean = true

    override fun accepts(config: EngineConfig): Boolean = config is FakeConfig

    override suspend fun createTyped(config: FakeConfig): HighlightEngine = FakeEngine
}

private class ClosableFakeEngine(
    override val name: String,
) : HighlightEngine {
    var closed: Boolean = false
        private set

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

    override fun close() {
        closed = true
    }
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

private fun <T> runSuspend(block: suspend () -> T): T {
    var completionResult: Result<T>? = null
    block.startCoroutine(object : Continuation<T> {
        override val context = EmptyCoroutineContext

        override fun resumeWith(result: Result<T>) {
            completionResult = result
        }
    })
    return completionResult!!.getOrThrow()
}
