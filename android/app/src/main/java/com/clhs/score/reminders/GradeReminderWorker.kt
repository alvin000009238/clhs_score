package com.clhs.score.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.clhs.score.data.GradeCacheStore
import com.clhs.score.data.GradeReminderRepository
import com.clhs.score.data.GradeReportDiffer
import com.clhs.score.data.SchoolAuthenticationException
import com.clhs.score.data.SchoolCookieJar
import com.clhs.score.data.SchoolGradeClient
import com.clhs.score.data.SchoolGradeRepository
import com.clhs.score.data.SchoolTransientException
import com.clhs.score.data.SessionStore
import com.clhs.score.data.SessionStorageException
import kotlinx.coroutines.CancellationException
import java.io.IOException

internal enum class ReminderFailureAction(val message: String) {
    STOP("登入狀態已失效，段考提醒已停止"),
    RETRY("網路連線暫時異常，稍後重試"),
    COUNT_FAILURE("檢查失敗"),
}

internal fun reminderFailureAction(error: Throwable): ReminderFailureAction = when (error) {
    is SchoolAuthenticationException -> ReminderFailureAction.STOP
    is SchoolTransientException, is IOException -> ReminderFailureAction.RETRY
    else -> ReminderFailureAction.COUNT_FAILURE
}

class GradeReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    private val reminderRepository = GradeReminderRepository(appContext)
    private val notifier = GradeReminderNotifier(appContext)
    private val scheduler = GradeReminderScheduler(appContext)

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        val state = reminderRepository.loadState()
        if (!state.enabled) return Result.success()

        val sessionStore = SessionStore(applicationContext)

        if (now >= state.expiresAtMillis) {
            reminderRepository.stop("段考提醒已超過 48 小時")
            scheduler.cancel()
            try {
                sessionStore.clearReminderSession()
            } catch (_: SessionStorageException) {
                // State and work are already stopped; encrypted storage cleanup can retry later.
            }
            return Result.success()
        }

        val session = try {
            sessionStore.loadReminderSession(now, state.studentNo)
        } catch (_: SessionStorageException) {
            stopAndNotify(sessionStore, "登入狀態已失效，段考提醒已停止")
            return Result.success()
        }
        if (session == null) {
            stopAndNotify(sessionStore, "登入狀態已失效，段考提醒已停止")
            return Result.success()
        }

        return runCatching {
            val repository = SchoolGradeRepository(
                client = SchoolGradeClient(cookieJar = SchoolCookieJar()),
                sessionStore = sessionStore,
                cacheStore = GradeCacheStore(applicationContext),
            )
            val report = repository.fetchGrades(
                session = session,
                yearValue = state.yearValue,
                examValue = state.examValue,
                forceRefresh = true,
            )
            val newSnapshot = GradeReportDiffer.snapshot(report)
            val oldSnapshot = state.snapshot ?: newSnapshot
            val changeSet = GradeReportDiffer.diff(
                before = oldSnapshot,
                after = newSnapshot,
                studentNo = state.studentNo,
                yearValue = state.yearValue,
                examValue = state.examValue,
                examName = state.examName,
                checkedAtMillis = now,
            )
            reminderRepository.saveState(
                state.copy(
                    lastCheckedAtMillis = now,
                    snapshot = newSnapshot,
                    latestChangeSet = changeSet.takeIf { it.hasChanges } ?: state.latestChangeSet,
                    consecutiveFailures = 0,
                    stoppedReason = null,
                ),
            )
            if (changeSet.hasChanges) {
                notifier.showChangedNotification(changeSet)
            }
            Result.success()
        }.getOrElse { error ->
            if (error is CancellationException) throw error
            when (val action = reminderFailureAction(error)) {
                ReminderFailureAction.STOP -> {
                    stopAndNotify(sessionStore, action.message)
                    Result.success()
                }
                ReminderFailureAction.RETRY -> Result.retry()
                ReminderFailureAction.COUNT_FAILURE -> {
                    val failures = state.consecutiveFailures + 1
                    if (failures >= MAX_FAILURES_BEFORE_STOP) {
                        stopAndNotify(sessionStore, "連續檢查失敗，段考提醒已停止")
                    } else {
                        reminderRepository.saveState(
                            state.copy(
                                lastCheckedAtMillis = now,
                                consecutiveFailures = failures,
                                stoppedReason = action.message,
                            ),
                        )
                    }
                    Result.success()
                }
            }
        }
    }

    private suspend fun stopAndNotify(sessionStore: SessionStore, reason: String) {
        reminderRepository.stop(reason)
        scheduler.cancel()
        try {
            sessionStore.clearReminderSession()
        } catch (_: SessionStorageException) {
            // The reminder state and work are already stopped; the fixed user message avoids leaking crypto details.
        }
        notifier.showStoppedNotification(reason)
    }

    private companion object {
        const val MAX_FAILURES_BEFORE_STOP = 3
    }
}
