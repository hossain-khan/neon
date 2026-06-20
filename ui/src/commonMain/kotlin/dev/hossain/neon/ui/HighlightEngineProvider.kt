package dev.hossain.neon.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import dev.hossain.neon.core.HighlightEngine

public val LocalHighlightEngine: androidx.compose.runtime.ProvidableCompositionLocal<HighlightEngine> = staticCompositionLocalOf {
    error("No HighlightEngine provided. Wrap with HighlightEngineProvider {}")
}

@Composable
public fun HighlightEngineProvider(
    engine: HighlightEngine,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalHighlightEngine provides engine
    ) {
        content()
    }
}
