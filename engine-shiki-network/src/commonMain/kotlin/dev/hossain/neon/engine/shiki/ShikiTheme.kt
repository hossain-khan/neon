package dev.hossain.neon.engine.shiki

import dev.hossain.neon.core.HighlightTheme

public class ShikiTheme(
    override val name: String,
    override val isDark: Boolean,
) : HighlightTheme {
    public companion object {
        public fun builtin(name: String): ShikiTheme {
            val isDark = when (name.lowercase()) {
                "github-light", "light-plus", "min-light" -> false
                else -> true
            }
            return ShikiTheme(name, isDark)
        }
    }
}
