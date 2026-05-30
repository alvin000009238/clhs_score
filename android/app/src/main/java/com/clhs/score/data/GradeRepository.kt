package com.clhs.score.data

interface GradeRepository {
    fun restoreSession(): AuthenticatedSession?


    suspend fun loadStructure(session: AuthenticatedSession, forceRefresh: Boolean = false): List<YearTermOption>

    suspend fun fetchGrades(
        session: AuthenticatedSession,
        yearValue: String,
        examValue: String,
        forceRefresh: Boolean = false,
    ): GradeReport

    suspend fun logout()

    suspend fun loginWithCookies(
        studentNo: String,
        cookies: Map<String, String>,
    ): AuthenticatedSession
}

class SchoolGradeRepository(
    private val client: SchoolGradeClient,
    private val sessionStore: SessionStore,
    private val cacheStore: GradeCacheStore,
) : GradeRepository {
    override fun restoreSession(): AuthenticatedSession? {
        val session = sessionStore.loadSession() ?: return null
        client.restoreSession(session)
        return session
    }



    override suspend fun loadStructure(session: AuthenticatedSession, forceRefresh: Boolean): List<YearTermOption> {
        if (!forceRefresh) {
            val cached = cacheStore.loadStructure(session.studentNo)
            if (cached != null) return cached
        }
        val structure = client.loadStructure(session)
        cacheStore.saveStructure(session.studentNo, structure)
        return structure
    }

    override suspend fun fetchGrades(
        session: AuthenticatedSession,
        yearValue: String,
        examValue: String,
        forceRefresh: Boolean,
    ): GradeReport {
        if (!forceRefresh) {
            val cached = cacheStore.loadGradeReport(session.studentNo, yearValue, examValue)
            if (cached != null) return cached
        }
        val report = client.fetchGrades(session, yearValue, examValue)
        cacheStore.saveGradeReport(session.studentNo, yearValue, examValue, report)
        return report
    }

    override suspend fun logout() {
        val studentNo = sessionStore.loadSession()?.studentNo
        sessionStore.clear()
        client.clearSession()
        if (studentNo != null) {
            cacheStore.clearStudent(studentNo)
        }
    }

    override suspend fun loginWithCookies(
        studentNo: String,
        cookies: Map<String, String>,
    ): AuthenticatedSession {
        val session = client.loginWithCookies(studentNo, cookies)
        sessionStore.saveSession(session)
        return session
    }
}
