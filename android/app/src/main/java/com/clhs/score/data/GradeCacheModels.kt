package com.clhs.score.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

internal const val GRADE_REPORT_CACHE_VERSION = 2

@Serializable
internal data class CachedGradeReport(
    val cacheVersion: Int = GRADE_REPORT_CACHE_VERSION,
    val message: String,
    val studentInfo: StudentInfo,
    val examSummary: ExamSummary?,
    val subjects: List<SubjectScore>,
    val standards: List<GradeStandard>,
)

internal fun GradeReport.toCachedGradeReport(): CachedGradeReport = CachedGradeReport(
    message = message,
    studentInfo = studentInfo,
    examSummary = examSummary,
    subjects = subjects,
    standards = standards,
)

internal fun CachedGradeReport.toGradeReport(): GradeReport = GradeReport(
    message = message,
    studentInfo = studentInfo,
    examSummary = examSummary,
    subjects = subjects,
    standards = standards,
    rawResult = JsonObject(emptyMap()),
)

internal fun CachedGradeReport.toCurrentGradeReport(): GradeReport? =
    takeIf { cacheVersion == GRADE_REPORT_CACHE_VERSION }?.toGradeReport()
