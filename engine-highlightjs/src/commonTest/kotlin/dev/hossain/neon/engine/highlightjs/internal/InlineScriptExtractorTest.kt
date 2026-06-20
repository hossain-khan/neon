package dev.hossain.neon.engine.highlightjs.internal

import kotlin.test.Test
import kotlin.test.assertEquals

class InlineScriptExtractorTest {
    @Test
    fun extractsInlineScriptsInOrder() {
        val html = """
            <html>
            <body>
            <script>const a = 1;</script>
            <div>content</div>
            <script>const b = 2;</script>
            </body>
            </html>
        """.trimIndent()

        assertEquals(
            expected = listOf("const a = 1;", "const b = 2;"),
            actual = extractInlineScripts(html),
        )
    }

    @Test
    fun ignoresUnclosedScriptTag() {
        val html = "<script>const a = 1;"

        assertEquals(emptyList(), extractInlineScripts(html))
    }
}
