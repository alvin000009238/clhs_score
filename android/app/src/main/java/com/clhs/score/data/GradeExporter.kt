package com.clhs.score.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExamSelection(
    val yearValue: String,
    val examValue: String,
    val displayName: String,
)

object GradeExporter {

    private const val BOM = "\uFEFF"

    private val HEADER = listOf(
        "學期", "考試名稱", "科目", "分數", "班平均",
        "班排名", "校/類排名",
        "頂標", "前標", "均標", "後標", "底標", "標準差",
    )

    fun buildCsvContent(reports: List<Pair<String, GradeReport>>): String {
        val sb = StringBuilder()
        sb.append(BOM)
        sb.appendLine(HEADER.joinToString(",") { csvEscape(it) })

        for ((examDisplayName, report) in reports) {
            val yearTermDisplay = report.examSummary?.let { summary ->
                val yearText = summary.year?.let { "${it}" } ?: ""
                val termText = summary.termText
                if (yearText.isNotEmpty()) "${yearText}${termText}" else termText
            } ?: ""

            for ((index, subject) in report.subjects.withIndex()) {
                val standard = report.standardFor(subject, index)
                val row = listOf(
                    yearTermDisplay,
                    examDisplayName,
                    cleanSubjectName(subject.subjectName),
                    subject.scoreDisplay,
                    subject.classAverageDisplay,
                    formatRank(subject.classRank, subject.classRankCount),
                    formatRank(subject.yearRank, subject.yearRankCount),
                    standard?.top?.formatScore() ?: "",
                    standard?.front?.formatScore() ?: "",
                    standard?.average?.formatScore() ?: "",
                    standard?.back?.formatScore() ?: "",
                    standard?.bottom?.formatScore() ?: "",
                    standard?.standardDeviation?.formatScore() ?: "",
                )
                sb.appendLine(row.joinToString(",") { csvEscape(it) })
            }
        }
        return sb.toString()
    }

    fun saveCsvToDownloads(
        context: Context,
        csv: String,
        studentNo: String,
    ): Result<String> = runCatching {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val fileName = "${studentNo}_成績_${timestamp}.csv"
        val bytes = csv.toByteArray(Charsets.UTF_8)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                values,
            ) ?: throw IllegalStateException("無法建立檔案")
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: throw IllegalStateException("無法寫入檔案")
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            val file = File(dir, fileName)
            FileOutputStream(file).use { it.write(bytes) }
        }
        fileName
    }

    private fun formatRank(rank: Int?, count: Int?): String {
        if (rank == null) return ""
        return if (count != null) "$rank/$count" else "$rank"
    }

    private fun Double.formatScore(): String {
        return if (this == kotlin.math.floor(this) && !this.isInfinite()) {
            this.toInt().toString()
        } else {
            "%.1f".format(this)
        }
    }

    private fun csvEscape(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
