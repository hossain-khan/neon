package dev.hossain.neon.core

public interface HighlightTheme {
    public val name: String
    public val isDark: Boolean
    public val backgroundColorHex: String? get() = null
    public val defaultTextColorHex: String? get() = null
}
