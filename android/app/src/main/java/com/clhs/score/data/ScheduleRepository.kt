package com.clhs.score.data

interface ScheduleRepository {
    suspend fun getScheduleYears(): List<ScheduleYearTermOption>
    suspend fun getScheduleClasses(year: String, term: String): List<ScheduleClassOption>
    suspend fun fetchSchedule(yearValue: String, year: String, term: String, classNo: String): ScheduleReport
    suspend fun getLatestSchedule(): ScheduleReport?
    suspend fun getWidgetPreferences(): Triple<Boolean, Boolean, Boolean>
    suspend fun saveWidgetPreferences(showTeacher: Boolean, showClassroom: Boolean, showTime: Boolean)
}

class NetworkScheduleRepository(
    private val client: SchoolGradeClient,
    private val sessionStore: SessionStore,
    private val cacheStore: GradeCacheStore,
) : ScheduleRepository {

    override suspend fun getScheduleYears(): List<ScheduleYearTermOption> {
        val session = sessionStore.loadSession() ?: throw SchoolException("未登入")
        return client.getScheduleYears(session)
    }

    override suspend fun getScheduleClasses(year: String, term: String): List<ScheduleClassOption> {
        val session = sessionStore.loadSession() ?: throw SchoolException("未登入")
        return client.getScheduleClasses(session, year, term)
    }

    override suspend fun fetchSchedule(yearValue: String, year: String, term: String, classNo: String): ScheduleReport {
        val session = sessionStore.loadSession() ?: throw SchoolException("未登入")
        val report = client.fetchSchedule(session, yearValue, year, term, classNo)
        
        // Use a composite key for caching so different classes in the same semester are cached separately.
        val cacheKey = if (classNo.isNotBlank()) "${yearValue}_${classNo}" else yearValue
        cacheStore.saveScheduleReport(session.studentNo, cacheKey, report)
        
        return report
    }

    override suspend fun getLatestSchedule(): ScheduleReport? {
        val session = sessionStore.loadSession() ?: return null
        return cacheStore.loadLatestScheduleReport(session.studentNo)
    }

    override suspend fun getWidgetPreferences(): Triple<Boolean, Boolean, Boolean> {
        return cacheStore.getWidgetPreferences()
    }

    override suspend fun saveWidgetPreferences(showTeacher: Boolean, showClassroom: Boolean, showTime: Boolean) {
        cacheStore.saveWidgetPreferences(showTeacher, showClassroom, showTime)
    }
}

class FakeScheduleRepository : ScheduleRepository {
    override suspend fun getScheduleYears(): List<ScheduleYearTermOption> {
        return FakeScheduleData.years
    }

    override suspend fun getScheduleClasses(year: String, term: String): List<ScheduleClassOption> {
        return FakeScheduleData.classes
    }

    override suspend fun fetchSchedule(yearValue: String, year: String, term: String, classNo: String): ScheduleReport {
        return FakeScheduleData.report(yearValue, classNo)
    }

    override suspend fun getLatestSchedule(): ScheduleReport? {
        return FakeScheduleData.report("114_2", "230")
    }

    override suspend fun getWidgetPreferences(): Triple<Boolean, Boolean, Boolean> {
        return Triple(true, true, true)
    }

    override suspend fun saveWidgetPreferences(showTeacher: Boolean, showClassroom: Boolean, showTime: Boolean) {
        // No-op for fake
    }
}
