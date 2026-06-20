package dev.hossain.neon.engine.highlightjs

import android.content.Context
import java.lang.ref.WeakReference

public object NeonAndroidContext {
    private var contextRef: WeakReference<Context>? = null

    public var applicationContext: Context
        get() = contextRef?.get() ?: error("NeonAndroidContext has not been initialized. Set NeonAndroidContext.applicationContext in MainActivity or Application.")
        set(value) {
            contextRef = WeakReference(value.applicationContext)
        }
}
