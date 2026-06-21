package dev.hossain.neon.engine.shiki.internal

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js

internal actual fun createDefaultShikiHttpClient(): HttpClient = HttpClient(Js)
