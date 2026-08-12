package com.clhs.score.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runInterruptible
import java.io.InterruptedIOException

internal suspend fun <T> runInterruptibleHttp(block: () -> T): T = try {
    runInterruptible(Dispatchers.IO, block)
} catch (error: InterruptedIOException) {
    currentCoroutineContext().ensureActive()
    throw error
}
