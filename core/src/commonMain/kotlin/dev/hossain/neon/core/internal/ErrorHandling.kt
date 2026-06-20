package dev.hossain.neon.core.internal

import dev.hossain.neon.core.HighlightException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException

internal suspend fun <T> withEngineErrorHandling(
    engineName: String,
    block: suspend () -> Result<T>
): Result<T> =
    try {
        block()
    } catch (e: TimeoutCancellationException) {
        Result.failure(HighlightException.Timeout())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HighlightException) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(HighlightException.JavaScriptEvaluationFailed(e))
    }

internal suspend fun <T> withHtmlParsingErrorHandling(block: suspend () -> Result<T>): Result<T> =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(HighlightException.HtmlParseFailed(e))
    }
