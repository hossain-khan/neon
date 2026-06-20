package dev.hossain.neon.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.hossain.neon.core.HighlightException
import dev.hossain.neon.core.HighlightResult
import dev.hossain.neon.core.HighlightTheme
import kotlinx.coroutines.launch

private val DefaultCopyButtonSentinel: (@Composable (onClick: () -> Unit) -> Unit) = { }
private val DefaultLanguageLabelSentinel: (@Composable () -> Unit) = { }
private val LineNumberGutterSpacing = 8.dp

@Composable
public fun SyntaxHighlightedCode(
    code: String,
    language: String,
    theme: HighlightTheme,
    modifier: Modifier = Modifier,
    style: CodeBlockStyle = CodeBlockStyle.Default,
    showLineNumbers: Boolean = false,
    scrollState: ScrollState = rememberScrollState(),
    languageLabel: (@Composable () -> Unit)? =
        if (language.isNotBlank()) DefaultLanguageLabelSentinel else null,
    copyButton: (@Composable (onClick: () -> Unit) -> Unit)? = DefaultCopyButtonSentinel,
    onCopyClick: ((String) -> Unit)? = null,
    onHighlightComplete: ((HighlightResult) -> Unit)? = null,
    onError: ((HighlightException) -> Unit)? = null,
    placeholder: (@Composable (code: String) -> Unit)? = null,
) {
    // Resolve fallback / default theme colors or style defaults

    
    // Let's define the UI-resolved background/text colors:
    val backgroundColorHex = (theme as? dev.hossain.neon.core.HighlightTheme)?.backgroundColorHex
    val defaultTextColorHex = (theme as? dev.hossain.neon.core.HighlightTheme)?.defaultTextColorHex
    
    val backgroundColor = remember(theme, style) {
        backgroundColorHex?.let { parseHexColor(it) } ?: style.fallbackBackgroundColor
    }
    val textColor = remember(theme, style) {
        defaultTextColorHex?.let { parseHexColor(it) } ?: style.fallbackTextColor
    }
    val lineNumberColor = remember(theme, style) {
        style.lineNumberColor.takeIf { it != Color.Unspecified }
            ?: textColor.copy(alpha = 0.4f)
    }

    val themedCodeStyle = remember(theme, style) { style.textStyle.copy(color = textColor) }
    val themedLineNumStyle = remember(theme, style) { style.textStyle.copy(color = lineNumberColor) }

    val effectiveLanguageLabel: (@Composable () -> Unit)? =
        remember(languageLabel, language) {
            when {
                languageLabel === DefaultLanguageLabelSentinel -> {
                    { SyntaxHighlightedCodeDefaults.LanguageLabel(language = language) }
                }
                else -> languageLabel
            }
        }

    val effectiveCopyButton: (@Composable (onClick: () -> Unit) -> Unit)? =
        remember(copyButton, style.copyButtonSize) {
            when {
                copyButton === DefaultCopyButtonSentinel -> {
                    { onClick: () -> Unit -> SyntaxHighlightedCodeDefaults.CopyButton(onClick = onClick, size = style.copyButtonSize) }
                }
                else -> copyButton
            }
        }

    if (LocalInspectionMode.current) {
        Surface(
            modifier = modifier,
            shape = style.shape,
            color = backgroundColor,
            contentColor = textColor,
        ) {
            Text(
                text = code,
                modifier = Modifier.padding(style.padding),
                style = themedCodeStyle,
            )
        }
        return
    }

    val latestOnError = rememberUpdatedState(onError)
    var highlightFailed by remember(code, language, theme) { mutableStateOf(false) }
    val highlightedState =
        rememberHighlightedCode(
            code = code,
            language = language,
            theme = theme,
            onHighlightComplete = onHighlightComplete,
            onError = { error ->
                highlightFailed = true
                latestOnError.value?.invoke(error)
            },
        )
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    Surface(
        modifier = modifier,
        shape = style.shape,
        color = backgroundColor,
        contentColor = textColor,
    ) {
        Column {
            if (effectiveLanguageLabel != null || effectiveCopyButton != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(style.headerPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    effectiveLanguageLabel?.invoke()
                    Spacer(modifier = Modifier.weight(1f))
                    if (effectiveCopyButton != null) {
                        effectiveCopyButton {
                            val handler = onCopyClick
                            if (handler != null) {
                                handler(code)
                            } else {
                                scope.launch {
                                    clipboard.setText(AnnotatedString(code))
                                }
                            }
                        }
                    }
                }
            }

            LaunchedEffect(code) {
                scrollState.scrollTo(0)
            }
            Box(modifier = Modifier.horizontalScroll(scrollState)) {
                val highlighted = highlightedState.value
                val placeholderContent = placeholder
                val shouldShowPlaceholder = highlighted == null && placeholderContent != null && !highlightFailed
                if (shouldShowPlaceholder) {
                    SelectionContainer {
                        if (showLineNumbers) {
                            LineNumberedPlaceholder(
                                code = code,
                                lineNumTextStyle = themedLineNumStyle,
                                style = style,
                                placeholder = { placeholderContent(code) },
                            )
                        } else {
                            Box(modifier = Modifier.padding(style.padding)) {
                                placeholderContent(code)
                            }
                        }
                    }
                } else {
                    AnimatedContent(
                        targetState = highlighted,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "syntax-highlight-fade",
                    ) { animatedHighlighted ->
                        SelectionContainer {
                            if (showLineNumbers) {
                                LineNumberedCode(
                                    code = code,
                                    highlighted = animatedHighlighted,
                                    codeTextStyle = themedCodeStyle,
                                    lineNumTextStyle = themedLineNumStyle,
                                    style = style,
                                )
                            } else {
                                Text(
                                    text = animatedHighlighted ?: AnnotatedString(code),
                                    modifier = Modifier.padding(style.padding),
                                    style = themedCodeStyle,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LineNumberedPlaceholder(
    code: String,
    lineNumTextStyle: TextStyle,
    style: CodeBlockStyle,
    placeholder: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lineCount = remember(code) { code.lines().size }
    val lineNumbers = remember(lineCount) { (1..lineCount).joinToString("\n") }

    Row(modifier = modifier.padding(style.padding)) {
        Text(
            text = lineNumbers,
            style = lineNumTextStyle,
            modifier = Modifier.width(style.lineNumberWidth),
            textAlign = TextAlign.End,
        )
        Spacer(modifier = Modifier.width(LineNumberGutterSpacing))
        Box {
            placeholder()
        }
    }
}

@Composable
private fun LineNumberedCode(
    code: String,
    highlighted: AnnotatedString?,
    codeTextStyle: TextStyle,
    lineNumTextStyle: TextStyle,
    style: CodeBlockStyle,
    modifier: Modifier = Modifier,
) {
    val lineCount = remember(highlighted?.text, code) { (highlighted?.text ?: code).lines().size }
    val lineNumbers = remember(lineCount) { (1..lineCount).joinToString("\n") }

    Row(modifier = modifier.padding(style.padding)) {
        Text(
            text = lineNumbers,
            style = lineNumTextStyle,
            modifier = Modifier.width(style.lineNumberWidth),
            textAlign = TextAlign.End,
        )
        Spacer(modifier = Modifier.width(LineNumberGutterSpacing))
        if (highlighted != null) {
            Text(text = highlighted, style = codeTextStyle)
        } else {
            Text(text = code, style = codeTextStyle)
        }
    }
}
