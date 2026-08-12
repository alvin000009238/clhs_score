package com.clhs.score.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.io.InterruptedIOException

internal suspend fun <T> runInterruptibleHttp(block: () -> T): T = try {
    runInterruptible(Dispatchers.IO, block)
} catch (error: InterruptedIOException) {
    currentCoroutineContext().ensureActive()
    throw error
}

internal suspend fun <T> Call.executeCancellable(block: (Response) -> T): T =
    suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWith(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                if (!continuation.isActive) {
                    response.close()
                    return
                }
                continuation.resumeWith(runCatching { response.use(block) })
            }
        })
    }
