package com.clhs.score.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Serializable
data class PeriodTime(val start: String, val end: String) {
    val singleLine: String get() = "$start-$end"
    val multiLine: String get() = "$start\n$end"
}

val PERIOD_TIMES = listOf(
    PeriodTime("08:10", "09:00"),
    PeriodTime("09:10", "10:00"),
    PeriodTime("10:10", "11:00"),
    PeriodTime("11:10", "12:00"),
    PeriodTime("13:00", "13:50"),
    PeriodTime("14:00", "14:50"),
    PeriodTime("15:05", "15:55"),
    PeriodTime("16:05", "16:55"),
)

@Serializable
data class ScheduleYearTermOption(
    val text: String,
    val value: String,
)

@Serializable
data class ScheduleClassOption(
    val text: String,
    val value: String,
)

@Serializable
data class ScheduleItem(
    val dayOfWeek: Int, // 1 for Monday, 7 for Sunday
    val period: Int,    // 1 to 9 (or more)
    val subjectName: String,
    val teacherName: String = "",
    val classroom: String = "",
    val rawData: JsonElement? = null
)

@Serializable
data class ScheduleReport(
    val yearTermValue: String,
    val items: List<ScheduleItem>
)

internal fun parseScheduleItems(timetableJson: String): List<ScheduleItem> {
    val root = runCatching { SchoolJson.parseToJsonElement(timetableJson) }.getOrNull() ?: return emptyList()
    val items = mutableListOf<ScheduleItem>()

    fun extractClasses(element: JsonElement) {
        when (element) {
            is JsonObject -> {
                val subjectName = element.stringField(
                    "SubjectName",
                    "subjectName",
                    "CourseName",
                    "SubjectDisplay",
                )
                val dayOfWeek = element.intField("WeekDay", "weekDay", "DayOfWeek")
                val period = element.intField("SectionSeq", "sectionSeq", "Section", "Period")

                if (!subjectName.isNullOrBlank() && dayOfWeek != null && period != null && dayOfWeek > 0 && period > 0) {
                    items.add(
                        ScheduleItem(
                            dayOfWeek = dayOfWeek,
                            period = period,
                            subjectName = subjectName,
                            teacherName = element.stringField("TeacherNameDisplay", "TeacherName", "FirstTeacherName").orEmpty(),
                            classroom = element.stringField("ClassroomName", "ClassroomDisplay", "Classroom").orEmpty(),
                            rawData = element,
                        ),
                    )
                }

                element.values.forEach { extractClasses(it) }
            }
            is JsonArray -> element.forEach { extractClasses(it) }
            else -> Unit
        }
    }

    extractClasses(root)
    return items.distinctBy { it.dayOfWeek to it.period }
}

private fun JsonObject.stringField(vararg names: String): String? =
    names.firstNotNullOfOrNull { name ->
        (this[name] as? JsonPrimitive)
            ?.contentOrNull
            ?.trim()
            ?.takeIf { it.isNotBlank() && it != "null" }
    }

private fun JsonObject.intField(vararg names: String): Int? =
    names.firstNotNullOfOrNull { name ->
        (this[name] as? JsonPrimitive)?.contentOrNull?.toIntOrNull()
    }

// Helper to determine background color dynamically
val predefinedColors = listOf(
    0xFFE3F2FD, // Light Blue
    0xFFF3E5F5, // Light Purple
    0xFFFFEBEE, // Light Pink
    0xFFE8F5E9, // Light Green
    0xFFFFF3E0, // Light Orange
    0xFFEFEBE9, // Light Brown
    0xFFFFF9C4, // Light Yellow
    0xFFFCE4EC, // Pink
    0xFFE0F7FA, // Cyan
    0xFFF1F8E9  // Lime
)

fun getSubjectColor(subjectName: String): Long {
    val index = kotlin.math.abs(subjectName.hashCode()) % predefinedColors.size
    return predefinedColors[index]
}
