package dev.hossain.neon.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Stable
public data class CodeBlockStyle(
    val shape: Shape = SyntaxHighlightedCodeDefaults.shape,
    val padding: PaddingValues = SyntaxHighlightedCodeDefaults.padding,
    val headerPadding: PaddingValues = SyntaxHighlightedCodeDefaults.headerPadding,
    val lineNumberColor: Color = Color.Unspecified,
    val lineNumberWidth: Dp = SyntaxHighlightedCodeDefaults.lineNumberWidth,
    val copyButtonSize: Dp = SyntaxHighlightedCodeDefaults.copyButtonSize,
    val textStyle: TextStyle = SyntaxHighlightedCodeDefaults.codeTextStyle,
    val fallbackBackgroundColor: Color = SyntaxHighlightedCodeDefaults.fallbackBackgroundColor,
    val fallbackTextColor: Color = SyntaxHighlightedCodeDefaults.fallbackTextColor,
) {
    public companion object {
        public val Default: CodeBlockStyle = CodeBlockStyle()

        public val Compact: CodeBlockStyle =
            CodeBlockStyle(
                padding = PaddingValues(12.dp),
                headerPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            )
    }
}
