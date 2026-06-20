package dev.hossain.neon.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.AnnotatedString
import dev.hossain.neon.core.HighlightException
import dev.hossain.neon.core.HighlightResult
import dev.hossain.neon.core.HighlightTheme
import dev.hossain.neon.core.HighlightTimings

public data class UiThemedHighlightResult(
    val light: AnnotatedString,
    val dark: AnnotatedString,
    val durationMs: Long,
    val timings: HighlightTimings,
)

@Composable
public fun rememberHighlightedCode(
    code: String,
    language: String,
    theme: HighlightTheme,
    onHighlightComplete: ((HighlightResult) -> Unit)? = null,
    onError: ((HighlightException) -> Unit)? = null,
): State<AnnotatedString?> {
    if (LocalInspectionMode.current) {
        return remember(code) { mutableStateOf(AnnotatedString(code)) }
    }

    val engine = LocalHighlightEngine.current
    val state = remember(code, language, theme) { mutableStateOf<AnnotatedString?>(null) }
    val latestCallback = rememberUpdatedState(onHighlightComplete)
    val latestErrorCallback = rememberUpdatedState(onError)

    LaunchedEffect(code, language, theme, engine) {
        state.value = null
        engine.highlight(code, language, theme)
            .onSuccess { result ->
                val annotated = tokensToAnnotatedString(result.tokens)
                println("[DEBUG] Highlight success! Tokens count: ${result.tokens.size}, Annotated length: ${annotated.length}")
                if (annotated.length > 0) {
                    println("[DEBUG] First 100 chars of annotated text: '${annotated.text.take(100)}'")
                }
                state.value = annotated
                latestCallback.value?.invoke(result)
            }.onFailure { error ->
                val neonException = when (error) {
                    is HighlightException -> error
                    else -> HighlightException.NetworkError(error)
                }
                latestErrorCallback.value?.invoke(neonException)
            }
    }

    return state
}

@Composable
public fun rememberHighlightedCodeBothThemes(
    code: String,
    language: String,
    lightTheme: HighlightTheme,
    darkTheme: HighlightTheme,
    onHighlightComplete: ((UiThemedHighlightResult) -> Unit)? = null,
    onError: ((HighlightException) -> Unit)? = null,
): State<UiThemedHighlightResult?> {
    if (LocalInspectionMode.current) {
        return remember { mutableStateOf(null) }
    }

    val engine = LocalHighlightEngine.current
    val state = remember(code, language, lightTheme, darkTheme) { mutableStateOf<UiThemedHighlightResult?>(null) }
    val latestCallback = rememberUpdatedState(onHighlightComplete)
    val latestErrorCallback = rememberUpdatedState(onError)

    LaunchedEffect(code, language, lightTheme, darkTheme, engine) {
        state.value = null
        engine.highlightBoth(code, language, lightTheme, darkTheme)
            .onSuccess { result ->
                val lightAnnotated = tokensToAnnotatedString(result.light.tokens)
                val darkAnnotated = tokensToAnnotatedString(result.dark.tokens)
                val themedResult = UiThemedHighlightResult(
                    light = lightAnnotated,
                    dark = darkAnnotated,
                    durationMs = result.light.timings.total.inWholeMilliseconds,
                    timings = result.light.timings
                )
                state.value = themedResult
                latestCallback.value?.invoke(themedResult)
            }.onFailure { error ->
                val neonException = when (error) {
                    is HighlightException -> error
                    else -> HighlightException.NetworkError(error)
                }
                latestErrorCallback.value?.invoke(neonException)
            }
    }

    return state
}
