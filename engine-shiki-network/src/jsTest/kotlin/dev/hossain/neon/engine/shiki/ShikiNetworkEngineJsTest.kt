package dev.hossain.neon.engine.shiki

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShikiNetworkEngineJsTest {
    @Test
    fun providerCreatesEngineWithDefaultJsClient() = runTest {
        val engine = ShikiNetworkEngineProvider.createTyped(
            ShikiNetworkConfig(
                serviceUrl = "https://127.0.0.1:1",
            )
        )

        try {
            assertEquals("shiki-network", engine.name)
            assertTrue(engine.supportedLanguages.isEmpty())
        } finally {
            engine.close()
        }
    }
}
