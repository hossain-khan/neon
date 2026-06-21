package dev.hossain.neon.engine.shiki.internal

import io.ktor.client.HttpClient

internal expect fun createDefaultShikiHttpClient(): HttpClient
