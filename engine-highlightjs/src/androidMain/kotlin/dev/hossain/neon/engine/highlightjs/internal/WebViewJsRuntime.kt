package dev.hossain.neon.engine.highlightjs.internal

import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import android.content.Context
import dev.hossain.neon.core.HighlightException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal actual class JsRuntime actual constructor(
    platformContext: Any?,
) : AutoCloseable {
    private val context: Context? = platformContext as? Context
    private var webView: WebView? = null
    private val readyDeferred = CompletableDeferred<WebView>()

    actual suspend fun initialize(htmlContent: String) {
        withContext(Dispatchers.Main) {
            val context = context ?: throw HighlightException.UnsupportedPlatform(
                engine = "highlightjs",
                platform = "android",
                details = "Create HljsConfig with HljsConfig.android(context) before creating the engine."
            )
            val wv = try {
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            if (webView == null) return
                            if (!readyDeferred.isCompleted) {
                                readyDeferred.complete(this@apply)
                            }
                        }
                    }
                    loadDataWithBaseURL("https://appassets.androidplatform.net/", htmlContent, "text/html", "UTF-8", null)
                }
            } catch (e: Exception) {
                val ex = HighlightException.EngineInitializationFailed("highlightjs", e)
                readyDeferred.completeExceptionally(ex)
                throw ex
            }
            webView = wv
        }
    }

    actual suspend fun evaluate(script: String): String {
        val wv = readyDeferred.await()
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                wv.evaluateJavascript(script) { result ->
                    if (continuation.isActive) {
                        if (result == null) {
                            continuation.resumeWithException(HighlightException.JavaScriptEvaluationFailed(Exception("evaluateJavascript returned null")))
                        } else {
                            continuation.resume(result)
                        }
                    }
                }
            }
        }
    }

    actual override fun close() {
        val wv = webView ?: return
        webView = null
        if (!readyDeferred.isCompleted) {
            readyDeferred.cancel()
        }
        Handler(Looper.getMainLooper()).post {
            wv.destroy()
        }
    }
}
