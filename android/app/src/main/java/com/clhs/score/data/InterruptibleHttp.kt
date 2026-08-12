package com.clhs.score.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runInterruptible
import okhttp3.Call
import okhttp3.Response
import java.io.InterruptedIOException

internal suspend fun <T> runInterruptibleHttp(block: () -> T): T = try {
    runInterruptible(Dispatchers.IO, block)
} catch (error: InterruptedIOException) {
    currentCoroutineContext().ensureActive()
    throw error
}

internal suspend fun <T> Call.executeCancellable(block: (Response) -> T): T {
    val cancellation = currentCoroutineContext()[Job]?.invokeOnCompletion { cause ->
        if (cause is CancellationException) cancel()
    }
    return try {
        runInterruptibleHttp { execute().use(block) }
    } finally {
        cancellation?.dispose()
    }
}
