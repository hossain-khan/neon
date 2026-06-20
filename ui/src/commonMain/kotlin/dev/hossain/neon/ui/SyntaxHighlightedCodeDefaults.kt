package dev.hossain.neon.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import neonproject.ui.generated.resources.Res
import neonproject.ui.generated.resources.copy_code_block
import org.jetbrains.compose.resources.painterResource

public object SyntaxHighlightedCodeDefaults {
    public val codeTextStyle: TextStyle =
        TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 20.sp,
        )

    public val shape: Shape = RoundedCornerShape(8.dp)
    public val padding: PaddingValues = PaddingValues(16.dp)
    public val headerPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    public val lineNumberWidth: Dp = 32.dp
    public val copyButtonSize: Dp = 32.dp

    public val fallbackBackgroundColor: Color = Color(0xFF1E1E1E)
    public val fallbackTextColor: Color = Color(0xFFCCCCCC)

    @Composable
    public fun CopyButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        tint: Color = LocalContentColor.current.copy(alpha = 0.7f),
        contentDescription: String = "Copy code",
        size: Dp = copyButtonSize,
    ) {
        IconButton(
            onClick = onClick,
            modifier =
                modifier
                    .size(size)
                    .semantics { this.contentDescription = contentDescription },
        ) {
            Icon(
                painter = painterResource(Res.drawable.copy_code_block),
                contentDescription = null,
                modifier = Modifier.size(size * 0.6f),
                tint = tint,
            )
        }
    }

    @Composable
    public fun LanguageLabel(
        language: String,
        modifier: Modifier = Modifier,
        color: Color = LocalContentColor.current.copy(alpha = 0.6f),
        fontSize: TextUnit = 12.sp,
    ) {
        if (language.isNotBlank()) {
            Text(
                text = language,
                modifier = modifier,
                style =
                    TextStyle(
                        fontFamily = FontFamily.Monospace,
                        color = color,
                        fontSize = fontSize,
                    ),
            )
        }
    }
}
