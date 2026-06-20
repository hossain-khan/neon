package dev.hossain.neon.core

public data class HighlightToken(
    val text: String,
    val color: String? = null,          // hex "#FF0000" or null for default
    val background: String? = null,     // hex "#000000" or null for default
    val fontWeight: TokenFontWeight? = null,
    val fontStyle: TokenFontStyle? = null,
    val isUnderline: Boolean = false,
)

public enum class TokenFontWeight { NORMAL, BOLD }
public enum class TokenFontStyle { NORMAL, ITALIC }
