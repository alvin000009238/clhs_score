package com.clhs.score.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.clhs.score.data.GradeCacheStore
import com.clhs.score.data.GradeReminderRepository
import com.clhs.score.data.GradeReportDiffer
import com.clhs.score.data.SchoolCookieJar
import com.clhs.score.data.SchoolGradeClient
import com.clhs.score.data.SchoolGradeRepository
import com.clhs.score.data.SessionStore
import kotlinx.coroutines.CancellationException

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
            sessionStore.clearReminderSession()
            scheduler.cancel()
            return Result.success()
        }

        val session = sessionStore.loadReminderSession(now) ?: sessionStore.loadSession()
        if (session == null) {
            stopAndNotify(sessionStore, "登入狀態已失效，段考提醒已停止")
            return Result.success()
        }
        if (session.studentNo != state.studentNo) {
            stopAndNotify(sessionStore, "登入學生已變更，段考提醒已停止")
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
            val failures = state.consecutiveFailures + 1
            if (failures >= MAX_FAILURES_BEFORE_STOP) {
                stopAndNotify(sessionStore, "連續檢查失敗，段考提醒已停止：${error.message ?: "未知錯誤"}")
            } else {
                reminderRepository.saveState(
                    state.copy(
                        lastCheckedAtMillis = now,
                        consecutiveFailures = failures,
                        stoppedReason = error.message ?: "檢查失敗",
                    ),
                )
            }
            Result.success()
        }
    }

    private suspend fun stopAndNotify(sessionStore: SessionStore, reason: String) {
        reminderRepository.stop(reason)
        sessionStore.clearReminderSession()
        scheduler.cancel()
        notifier.showStoppedNotification(reason)
    }

    private companion object {
        const val MAX_FAILURES_BEFORE_STOP = 3
    }
}
