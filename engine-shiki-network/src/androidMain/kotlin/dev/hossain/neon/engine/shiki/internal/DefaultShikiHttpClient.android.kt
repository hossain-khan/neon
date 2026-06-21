package dev.hossain.neon.engine.shiki.internal

import io.ktor.client.HttpClient

internal actual fun createDefaultShikiHttpClient(): HttpClient = HttpClient()
