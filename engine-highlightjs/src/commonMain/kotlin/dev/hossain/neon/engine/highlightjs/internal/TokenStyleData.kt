package dev.hossain.neon.engine.highlightjs.internal

import dev.hossain.neon.core.TokenFontStyle
import dev.hossain.neon.core.TokenFontWeight

internal data class TokenStyleData(
    val color: String? = null,
    val background: String? = null,
    val fontWeight: TokenFontWeight? = null,
    val fontStyle: TokenFontStyle? = null,
    val isUnderline: Boolean = false,
)
