package dev.hossain.neon.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import dev.hossain.neon.core.HighlightToken
import dev.hossain.neon.core.TokenFontStyle
import dev.hossain.neon.core.TokenFontWeight

public fun tokensToAnnotatedString(
    tokens: List<HighlightToken>,
    defaultColor: Color = Color.Unspecified,
    defaultBackground: Color = Color.Unspecified,
): AnnotatedString = buildAnnotatedString {
    for (token in tokens) {
        val style = SpanStyle(
            color = token.color?.let { parseHexColor(it) } ?: defaultColor,
            background = token.background?.let { parseHexColor(it) } ?: defaultBackground,
            fontWeight = when (token.fontWeight) {
                TokenFontWeight.BOLD -> FontWeight.Bold
                TokenFontWeight.NORMAL -> FontWeight.Normal
                else -> null
            },
            fontStyle = when (token.fontStyle) {
                TokenFontStyle.ITALIC -> FontStyle.Italic
                TokenFontStyle.NORMAL -> FontStyle.Normal
                else -> null
            },
            textDecoration = if (token.isUnderline) TextDecoration.Underline else null,
        )
        withStyle(style) { append(token.text) }
    }
}

internal fun parseHexColor(hex: String): Color {
    val cleaned = hex.trimStart('#')
    return try {
        when (cleaned.length) {
            3 -> {
                val r = cleaned[0].toString().repeat(2).toInt(16)
                val g = cleaned[1].toString().repeat(2).toInt(16)
                val b = cleaned[2].toString().repeat(2).toInt(16)
                Color(r, g, b)
            }
            4 -> {
                val a = cleaned[0].toString().repeat(2).toInt(16)
                val r = cleaned[1].toString().repeat(2).toInt(16)
                val g = cleaned[2].toString().repeat(2).toInt(16)
                val b = cleaned[3].toString().repeat(2).toInt(16)
                Color(r, g, b, a)
            }
            6 -> {
                val r = cleaned.substring(0, 2).toInt(16)
                val g = cleaned.substring(2, 4).toInt(16)
                val b = cleaned.substring(4, 6).toInt(16)
                Color(r, g, b)
            }
            8 -> {
                val a = cleaned.substring(0, 2).toInt(16)
                val r = cleaned.substring(2, 4).toInt(16)
                val g = cleaned.substring(4, 6).toInt(16)
                val b = cleaned.substring(6, 8).toInt(16)
                Color(r, g, b, a)
            }
            else -> Color.Unspecified
        }
    } catch (e: Exception) {
        Color.Unspecified
    }
}
