package dev.hossain.neon.engine.highlightjs

import android.content.Context

public fun HljsConfig.Companion.android(context: Context): HljsConfig {
    return HljsConfig(platformContext = context.applicationContext)
}
