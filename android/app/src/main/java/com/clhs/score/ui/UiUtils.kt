package com.clhs.score.ui

import androidx.compose.ui.graphics.Color

import kotlin.math.floor

fun formatRank(rank: Double?, count: Int?, showCount: Boolean): String {
    if (rank == null) return "--"
    val rankText = floor(rank).toInt().toString()
    return if (showCount && count != null && count > 0) "$rankText/$count" else rankText
}

fun formatRank(rank: Int?, count: Int?, emptyValue: String = "--"): String {
    if (rank == null) return emptyValue
    if (count == null || count <= 0) return "$rank"
    return "$rank / $count"
}

fun formatExportRank(rank: Int?, count: Int?): String {
    if (rank == null) return ""
    return if (count != null && count > 0) "$rank/$count" else "$rank"
}

fun subjectPercentLabel(rank: Int?, count: Int?): String {
    if (rank == null || count == null || count <= 0) return "--"
    val percent = ((rank.toDouble() / count) * 100.0).toInt().coerceIn(1, 100)
    return "前 $percent%"
}

fun distributionColor(label: String): Color = when (label) {
    "90-100" -> Color(0xFF4F8F63)
    "80-89" -> Color(0xFF4B8F86)
    "70-79" -> Color(0xFF5A789A)
    "60-69" -> Color(0xFFC47A2C)
    "50-59" -> Color(0xFFC26A45)
    else -> Color(0xFFB75E62)
}
