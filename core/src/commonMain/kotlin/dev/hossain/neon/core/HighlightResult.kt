package dev.hossain.neon.core

import kotlin.time.Duration

public data class HighlightResult(
    val tokens: List<HighlightToken>,
    val language: String,
    val timings: HighlightTimings,
)

public data class ThemedHighlightResult(
    val light: HighlightResult,
    val dark: HighlightResult,
)

public data class HighlightTimings(
    val jsBridge: Duration,
    val jsonUnescape: Duration,
    val htmlParse: Duration,
    val themeParse: Duration,
    val total: Duration,
)

public data class HighlightLanguageInfo(
    val id: String,
    val name: String,
    val aliases: List<String>,
)
