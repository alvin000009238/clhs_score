package com.clhs.score.data

import java.time.LocalDate

interface ScheduleRepository {
    suspend fun getScheduleYears(): List<ScheduleYearTermOption>
    suspend fun getScheduleClasses(year: String, term: String): List<ScheduleClassOption>
    suspend fun fetchSchedule(
        yearValue: String,
        year: String,
        term: String,
        classNo: String,
        scope: ScheduleScope,
        targetDate: LocalDate,
    ): ScheduleReport
    suspend fun getLatestSchedule(): ScheduleReport?
}

class NetworkScheduleRepository(
    private val client: SchoolGradeClient,
    private val sessionStore: SessionStore,
    private val cacheStore: GradeCacheStore,
    activeSessionProvider: () -> AuthenticatedSession? = { null },
) : ScheduleRepository {
    private val sessionResolver = ActiveSessionResolver(
        activeSessionProvider = activeSessionProvider,
        storedSessionProvider = sessionStore::loadSession,
        biometricSessionPresentProvider = sessionStore::hasBiometricSession,
    )

    override suspend fun getScheduleYears(): List<ScheduleYearTermOption> {
        val session = sessionResolver.requireSession()
        return client.getScheduleYears(session)
    }

    override suspend fun getScheduleClasses(year: String, term: String): List<ScheduleClassOption> {
        val session = sessionResolver.requireSession()
        return client.getScheduleClasses(session, year, term)
    }

    override suspend fun fetchSchedule(
        yearValue: String,
        year: String,
        term: String,
        classNo: String,
        scope: ScheduleScope,
        targetDate: LocalDate,
    ): ScheduleReport {
        val session = sessionResolver.requireSession()
        val report = client.fetchSchedule(
            session,
            yearValue,
            year,
            term,
            classNo,
            scope,
            targetDate,
        )
        
        // Use a composite key for caching so different classes in the same semester are cached separately.
        val cacheKey = if (classNo.isNotBlank()) "${yearValue}_${classNo}" else yearValue
        cacheStore.saveScheduleReport(session.studentNo, cacheKey, report)
        
        return report
    }

    override suspend fun getLatestSchedule(): ScheduleReport? {
        val session = sessionResolver.currentSession() ?: return null
        val report = cacheStore.loadLatestScheduleReport(session.studentNo) ?: return null
        cacheStore.saveWidgetScheduleReport(session.studentNo, report)
        return report
    }

}

internal class ActiveSessionResolver(
    private val activeSessionProvider: () -> AuthenticatedSession?,
    private val storedSessionProvider: () -> AuthenticatedSession?,
    private val biometricSessionPresentProvider: () -> Boolean,
) {
    fun currentSession(): AuthenticatedSession? {
        activeSessionProvider()?.let { return it }
        if (biometricSessionPresentProvider()) return null
        return storedSessionProvider()
    }

    fun requireSession(): AuthenticatedSession =
        currentSession() ?: throw SchoolException("未登入")
}

class FakeScheduleRepository : ScheduleRepository {
    override suspend fun getScheduleYears(): List<ScheduleYearTermOption> {
        return FakeScheduleData.years
    }

    override suspend fun getScheduleClasses(year: String, term: String): List<ScheduleClassOption> {
        return FakeScheduleData.classes
    }

    override suspend fun fetchSchedule(
        yearValue: String,
        year: String,
        term: String,
        classNo: String,
        scope: ScheduleScope,
        targetDate: LocalDate,
    ): ScheduleReport {
        return FakeScheduleData.report(yearValue, classNo, scope)
    }

    override suspend fun getLatestSchedule(): ScheduleReport? {
        return FakeScheduleData.report("114_2", "230")
    }

}
