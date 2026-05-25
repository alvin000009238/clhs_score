package com.clhs.score.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class CachedGradeReport(
    val cacheVersion: Int = 1,
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

internal fun GradeReport.withoutRawResult(): GradeReport = copy(
    rawResult = JsonObject(emptyMap()),
)
