package dev.hossain.neon

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform