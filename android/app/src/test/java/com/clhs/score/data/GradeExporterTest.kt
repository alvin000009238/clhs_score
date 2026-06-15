package com.clhs.score.data

import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

class GradeExporterTest {

    @Test
    fun buildCsvContent_escapesCsvInjection() {
        val subjects = listOf(
            SubjectScore(
                subjectName = "=cmd|' /C calc'!A0",
                scoreDisplay = "+100",
                classAverageDisplay = "-50",
                classRank = 1,
                classRankCount = 30,
                yearRank = 1,
                yearRankCount = 300,
                score = 100.0,
                classAverage = 50.0,
                yearTermDisplay = "114學年度 上學期",
                flunk = false,
                absent = false,
                cheating = false
            )
        )
        val report = GradeReport(
            message = "",
            studentInfo = StudentInfo("123", "User", "Class", "1", "2023", true, true, true, true),
            examSummary = null,
            subjects = subjects,
            standards = emptyList(),
            rawResult = JsonObject(emptyMap())
        )

        val csv = GradeExporter.buildCsvContent(listOf("Exam" to report))
        val rows = csv.lines()
        val dataRow = rows[1]

        val columns = dataRow.split(",")
        assertEquals("'-50", columns[4])
    }
}
