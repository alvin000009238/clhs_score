package com.clhs.score.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import com.clhs.score.data.GradeReport
import com.clhs.score.data.cleanSubjectName
import com.clhs.score.data.shortenSubjectName
import kotlin.math.max
import kotlin.math.min
import com.clhs.score.data.getSubjectBaseName

private data class SelectedPoint(val x: Float, val y: Float, val score: Double, val subject: String, val color: Color, val isPointClick: Boolean = true)

private data class ChartData(
    val exams: List<Pair<String, String>>,
    val subjectPoints: Map<String, List<Double?>>,
    val minScore: Double,
    val maxScore: Double,
    val yLabels: List<Int>,
    val groupedSubjects: Map<String, List<String>>,
    val allPointsMap: Map<String, List<Triple<Int, Double, String>>>,
    val dashedLines: Map<String, List<Triple<Triple<Int, Double, String>, Triple<Int, Double, String>, Color>>>
)

@Composable
fun SubjectTrendLineChart(
    reports: List<GradeReport>,
    selectedSubjectKeys: Set<String>,
    subjectColors: Map<String, Color>,
    modifier: Modifier = Modifier,
    selectedBaseName: String? = null,
    onBaseNameSelected: (String?) -> Unit = {}
) {
    if (reports.isEmpty() || selectedSubjectKeys.isEmpty()) {
        return
    }

    var selectedPoint by remember { mutableStateOf<SelectedPoint?>(null) }
    
    androidx.compose.runtime.LaunchedEffect(selectedBaseName) {
        if (selectedBaseName != selectedPoint?.subject?.let { getSubjectBaseName(it) }) {
            selectedPoint = null
        }
    }
    val textMeasurer = rememberTextMeasurer()
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    // Pre-calculate data points and mapping structures to avoid recomposition/drawing overhead
    val chartData = remember(reports, selectedSubjectKeys, subjectColors) {
        val exams = reports.map { 
            val year = it.examSummary?.year?.toString().orEmpty()
            val term = it.examSummary?.termText.orEmpty()
            Pair("$year-$term", it.examSummary?.examName.orEmpty()) 
        }
        
        var minScore = 100.0
        var maxScore = 0.0
        val subjectPoints = mutableMapOf<String, List<Double?>>()
        
        selectedSubjectKeys.forEach { subjectKey ->
            val points = reports.map { report ->
                val subject = report.subjects.find { cleanSubjectName(it.subjectName).substringBefore("-") == subjectKey }
                val score = subject?.scoreValue
                if (score != null && score > 0) {
                    if (score < minScore) minScore = score
                    if (score > maxScore) maxScore = score
                    score
                } else null
            }
            subjectPoints[subjectKey] = points
        }

        if (minScore > maxScore) {
            minScore = 0.0
            maxScore = 100.0
        } else {
            minScore = max(0.0, minScore - 10.0)
            maxScore = min(100.0, maxScore + 10.0)
        }

        val yLabels = listOf(maxScore.toInt(), ((maxScore + minScore) / 2).toInt(), minScore.toInt())

        val groupedSubjects = subjectPoints.keys.groupBy { getSubjectBaseName(it) }
        val allPointsMap = mutableMapOf<String, List<Triple<Int, Double, String>>>()
        val dashedLines = mutableMapOf<String, List<Triple<Triple<Int, Double, String>, Triple<Int, Double, String>, Color>>>()

        groupedSubjects.forEach { (baseName, keys) ->
            val allPoints = mutableListOf<Triple<Int, Double, String>>()
            keys.forEach { key ->
                subjectPoints[key]?.forEachIndexed { index, score ->
                    if (score != null) {
                        allPoints.add(Triple(index, score, key))
                    }
                }
            }
            allPointsMap[baseName] = allPoints

            if (allPoints.isNotEmpty()) {
                val firstKey = allPoints.minByOrNull { it.first }?.third ?: keys.first()
                val groupColor = subjectColors[firstKey] ?: Color.Black
                
                val pointsByIndex = allPoints.groupBy { it.first }.toSortedMap()
                val indices = pointsByIndex.keys.toList()
                val dashed = mutableListOf<Triple<Triple<Int, Double, String>, Triple<Int, Double, String>, Color>>()
                
                for (i in 0 until indices.size - 1) {
                    val currentPoints = pointsByIndex[indices[i]]!!
                    val nextPoints = pointsByIndex[indices[i+1]]!!
                    
                    // Optimization: Precompute subjects to avoid O(N^3) complexity
                    val currentSubjects = currentPoints.map { it.third }.toSet()
                    val nextSubjects = nextPoints.map { it.third }.toSet()

                    currentPoints.forEach { p1 ->
                        nextPoints.forEach { p2 ->
                            val p1HasSuccessor = nextSubjects.contains(p1.third)
                            val p2HasPredecessor = currentSubjects.contains(p2.third)
                            
                            val isDifferentSubjectConnection = (!p1HasSuccessor || !p2HasPredecessor) && p1.third != p2.third
                            val isSameSubjectGap = p1.third == p2.third && (p2.first - p1.first > 1)
                            
                            if (isDifferentSubjectConnection || isSameSubjectGap) {
                                dashed.add(Triple(p1, p2, groupColor))
                            }
                        }
                    }
                }
                dashedLines[baseName] = dashed
            }
        }
        
        ChartData(exams, subjectPoints, minScore, maxScore, yLabels, groupedSubjects, allPointsMap, dashedLines)
    }

    val minSpacing = 80.dp
    val scrollState = rememberScrollState()

    BoxWithConstraints(modifier = modifier) {
        val requiredWidth = max(
            maxWidth,
            minSpacing * max(1, chartData.exams.size - 1) + 80.dp
        )

        Box(
            modifier = Modifier.horizontalScroll(scrollState)
        ) {
            Canvas(modifier = Modifier
                .width(requiredWidth)
                .fillMaxHeight()
                .pointerInput(chartData) {
                detectTapGestures { offset ->
                    val paddingLeft = 64.dp.toPx()
                    val paddingBottom = 60.dp.toPx()
                    val paddingTop = 20.dp.toPx()
                    val paddingRight = 32.dp.toPx()

                    val chartWidth = size.width - paddingLeft - paddingRight
                    val chartHeight = size.height - paddingTop - paddingBottom
                    val stepX = chartWidth / max(1, chartData.exams.size - 1).toFloat()

                    var closestPoint: SelectedPoint? = null
                    var minPointDistance = Float.MAX_VALUE
                    var closestLine: SelectedPoint? = null
                    var minLineDistance = Float.MAX_VALUE

                    val currentSelectedBaseName = selectedBaseName ?: selectedPoint?.subject?.let { getSubjectBaseName(it) }
                    val orderedGroups = chartData.groupedSubjects.entries.sortedBy { if (it.key == currentSelectedBaseName) 1 else 0 }

                    orderedGroups.forEach { (baseName, keys) ->
                        val allPoints = chartData.allPointsMap[baseName] ?: return@forEach
                        if (allPoints.isEmpty()) return@forEach
                        val firstKey = allPoints.minByOrNull { it.first }?.third ?: keys.first()
                        val groupColor = subjectColors[firstKey] ?: Color.Black
                        
                        // Pass 1: Find closest point
                        allPoints.forEach { (index, score, key) ->
                            val px = paddingLeft + stepX * index
                            val py = paddingTop + chartHeight * (1f - ((score - chartData.minScore) / (chartData.maxScore - chartData.minScore)).toFloat())
                            
                            val dx = offset.x - px
                            val dy = offset.y - py
                            val distance = dx * dx + dy * dy
                            
                            // Use <= to ensure that elements rendered later (on top) win in case of an overlap
                            if (distance <= minPointDistance) {
                                minPointDistance = distance
                                closestPoint = SelectedPoint(px, py, score, key, groupColor, true)
                            }
                        }
                        
                        // Pass 2: Find closest line segment
                        val pointsByIndex = allPoints.groupBy { it.first }.toSortedMap()
                        val indices = pointsByIndex.keys.toList()
                        for (i in 0 until indices.size - 1) {
                            val currentPoints = pointsByIndex[indices[i]]!!
                            val nextPoints = pointsByIndex[indices[i+1]]!!
                            
                            currentPoints.forEach { p1 ->
                                nextPoints.forEach { p2 ->
                                    val px1 = paddingLeft + stepX * p1.first
                                    val py1 = paddingTop + chartHeight * (1f - (p1.second - chartData.minScore).toFloat() / (chartData.maxScore - chartData.minScore).toFloat())
                                    val px2 = paddingLeft + stepX * p2.first
                                    val py2 = paddingTop + chartHeight * (1f - (p2.second - chartData.minScore).toFloat() / (chartData.maxScore - chartData.minScore).toFloat())
                                    
                                    val l2 = (px1 - px2) * (px1 - px2) + (py1 - py2) * (py1 - py2)
                                    val distance = if (l2 == 0f) {
                                        (offset.x - px1) * (offset.x - px1) + (offset.y - py1) * (offset.y - py1)
                                    } else {
                                        val t = ((offset.x - px1) * (px2 - px1) + (offset.y - py1) * (py2 - py1)) / l2
                                        val clampedT = t.coerceIn(0f, 1f)
                                        val projX = px1 + clampedT * (px2 - px1)
                                        val projY = py1 + clampedT * (py2 - py1)
                                        (offset.x - projX) * (offset.x - projX) + (offset.y - projY) * (offset.y - projY)
                                    }
                                    
                                    // Use <= to ensure that elements rendered later (on top) win in case of an overlap
                                    if (distance <= minLineDistance) {
                                        minLineDistance = distance
                                        val distToP1 = (offset.x - px1) * (offset.x - px1) + (offset.y - py1) * (offset.y - py1)
                                        val distToP2 = (offset.x - px2) * (offset.x - px2) + (offset.y - py2) * (offset.y - py2)
                                        closestLine = if (distToP1 < distToP2) {
                                            SelectedPoint(px1, py1, p1.second, p1.third, groupColor, false)
                                        } else {
                                            SelectedPoint(px2, py2, p2.second, p2.third, groupColor, false)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    val pointTolerance = 20.dp.toPx()
                    val lineTolerance = 28.dp.toPx()
                    
                    selectedPoint = if (minPointDistance < pointTolerance * pointTolerance) {
                        closestPoint
                    } else if (minLineDistance < lineTolerance * lineTolerance) {
                        closestLine
                    } else {
                        null
                    }
                    onBaseNameSelected(selectedPoint?.subject?.let { getSubjectBaseName(it) })
                }
            }
        ) {
            val paddingLeft = 64.dp.toPx()
            val paddingBottom = 60.dp.toPx()
            val paddingTop = 20.dp.toPx()
            val paddingRight = 32.dp.toPx()

            val chartWidth = size.width - paddingLeft - paddingRight
            val chartHeight = size.height - paddingTop - paddingBottom
            val stepX = chartWidth / max(1, chartData.exams.size - 1)

            // Draw semester backgrounds
            if (chartData.exams.isNotEmpty()) {
                var currentSemesterStartIdx = 0
                var semesterColorIndex = 0
                val bgColors = listOf(
                    surfaceVariant.copy(alpha = 0.5f),
                    Color.Transparent
                )
                
                for (i in 0..chartData.exams.size) {
                    val isLast = i == chartData.exams.size
                    val isNewSemester = !isLast && i > 0 && chartData.exams[i].first != chartData.exams[i - 1].first
                    
                    if (isNewSemester || isLast) {
                        val startX = paddingLeft + stepX * (currentSemesterStartIdx - 0.5f)
                        val endX = if (isLast) {
                            paddingLeft + stepX * (i - 1 + 0.5f)
                        } else {
                            paddingLeft + stepX * (i - 0.5f)
                        }
                        
                        val rectWidth = (endX - startX).coerceAtLeast(0f)
                        if (rectWidth > 0) {
                            drawRect(
                                color = bgColors[semesterColorIndex % bgColors.size],
                                topLeft = Offset(startX, paddingTop),
                                size = androidx.compose.ui.geometry.Size(rectWidth, chartHeight)
                            )
                        }
                        
                        currentSemesterStartIdx = i
                        semesterColorIndex++
                    }
                }
            }

            // Draw Y-axis labels and grid lines
            chartData.yLabels.forEachIndexed { index, value ->
                val y = paddingTop + chartHeight * index / (chartData.yLabels.size - 1)
                val gridStartX = if (chartData.exams.size > 1) paddingLeft - stepX * 0.5f else paddingLeft
                val gridEndX = if (chartData.exams.size > 1) (size.width - paddingRight) + stepX * 0.5f else size.width - paddingRight
                drawLine(
                    color = gridColor,
                    start = Offset(gridStartX, y),
                    end = Offset(gridEndX, y),
                    strokeWidth = 1.dp.toPx()
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = value.toString(),
                    style = TextStyle(color = onSurfaceVariant, fontSize = 10.sp),
                    topLeft = Offset(0f, y - 10.sp.toPx() / 2)
                )
            }

            // Draw X-axis labels
            if (chartData.exams.isNotEmpty()) {
                chartData.exams.forEachIndexed { index, examNode ->
                    val examName = examNode.second
                    val x = paddingLeft + stepX * index
                    
                    // Draw semester vertical divider
                    if (index > 0 && examNode.first != chartData.exams[index - 1].first) {
                        val dividerX = paddingLeft + stepX * (index - 0.5f)
                        drawLine(
                            color = gridColor.copy(alpha = 0.5f),
                            start = Offset(dividerX, paddingTop),
                            end = Offset(dividerX, size.height - paddingBottom),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    
                    // Draw semester label at the top
                    if (index == 0 || examNode.first != chartData.exams[index - 1].first) {
                        drawText(
                            textMeasurer = textMeasurer,
                            text = examNode.first,
                            style = TextStyle(color = onSurfaceVariant, fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                            topLeft = Offset(x, 0f)
                        )
                    }

                    val labelPivot = Offset(x, size.height - paddingBottom + 8.dp.toPx())
                    val textLayoutResult = textMeasurer.measure(
                        text = examName,
                        style = TextStyle(color = onSurfaceVariant, fontSize = 10.sp)
                    )
                    
                    withTransform({
                        rotate(-45f, pivot = labelPivot)
                    }) {
                        drawText(
                            textLayoutResult = textLayoutResult,
                            color = onSurfaceVariant,
                            topLeft = Offset(labelPivot.x - textLayoutResult.size.width, labelPivot.y)
                        )
                    }
                }
            }

            // Group subjects for Z-index sorting and drawing
            val currentSelectedBaseName = selectedBaseName ?: selectedPoint?.subject?.let { getSubjectBaseName(it) }
            val orderedGroups = chartData.groupedSubjects.entries.sortedBy { if (it.key == currentSelectedBaseName) 1 else 0 }

            orderedGroups.forEach { (baseName, keys) ->
                val allPoints = chartData.allPointsMap[baseName] ?: return@forEach
                if (allPoints.isEmpty()) return@forEach
                val firstKey = allPoints.minByOrNull { it.first }?.third ?: keys.first()
                val groupColor = subjectColors[firstKey] ?: Color.Black
                
                val isSelected = baseName == currentSelectedBaseName
                val strokeWidth = if (isSelected) 4.dp.toPx() else 2.dp.toPx()
                val dotRadius = if (isSelected) 6.dp.toPx() else 4.dp.toPx()
                
                // Draw solid lines
                keys.forEach { subjectKey ->
                    val path = Path()
                    var isFirstPoint = true
                    val points = chartData.subjectPoints[subjectKey] ?: emptyList()
                    
                    points.forEachIndexed { index, score ->
                        if (score != null) {
                            val x = paddingLeft + stepX * index
                            val y = paddingTop + chartHeight * (1f - (score - chartData.minScore).toFloat() / (chartData.maxScore - chartData.minScore).toFloat())
                            
                            if (isFirstPoint) {
                                path.moveTo(x, y)
                                isFirstPoint = false
                            } else {
                                if (index > 0 && points[index - 1] == null) {
                                    path.moveTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                }
                            }
                        }
                    }
                    
                    drawPath(
                        path = path,
                        color = groupColor,
                        style = Stroke(width = strokeWidth)
                    )
                }
                
                // Draw dashed lines
                chartData.dashedLines[baseName]?.forEach { (p1, p2, color) ->
                    val x1 = paddingLeft + stepX * p1.first
                    val y1 = paddingTop + chartHeight * (1f - (p1.second - chartData.minScore).toFloat() / (chartData.maxScore - chartData.minScore).toFloat())
                    
                    val x2 = paddingLeft + stepX * p2.first
                    val y2 = paddingTop + chartHeight * (1f - (p2.second - chartData.minScore).toFloat() / (chartData.maxScore - chartData.minScore).toFloat())
                    
                    drawLine(
                        color = color.copy(alpha = 0.5f),
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = strokeWidth,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }
                
                // Draw dots
                keys.forEach { subjectKey ->
                    val points = chartData.subjectPoints[subjectKey] ?: emptyList()
                    
                    points.forEachIndexed { index, score ->
                        if (score != null) {
                            val x = paddingLeft + stepX * index
                            val y = paddingTop + chartHeight * (1f - (score - chartData.minScore).toFloat() / (chartData.maxScore - chartData.minScore).toFloat())
                            
                            drawCircle(
                                color = groupColor,
                                radius = dotRadius,
                                center = Offset(x, y)
                            )
                        }
                    }
                }
            }
            
            // Draw tooltip
            selectedPoint?.let { point ->
                if (point.isPointClick) {
                    val textLayoutResult = textMeasurer.measure(
                        text = "${shortenSubjectName(point.subject)}: ${point.score}",
                        style = TextStyle(color = Color.White, fontSize = 12.sp)
                    )
                    
                    val tooltipPadding = 8.dp.toPx()
                    val tooltipWidth = textLayoutResult.size.width + tooltipPadding * 2
                    val tooltipHeight = textLayoutResult.size.height + tooltipPadding * 2
                    
                    val tooltipX = (point.x - tooltipWidth / 2).coerceIn(0f, size.width - tooltipWidth)
                    val tooltipY = (point.y - tooltipHeight - 12.dp.toPx()).coerceAtLeast(0f)
                    
                    drawRoundRect(
                        color = point.color,
                        topLeft = Offset(tooltipX, tooltipY),
                        size = androidx.compose.ui.geometry.Size(tooltipWidth, tooltipHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                    
                    drawText(
                        textLayoutResult = textLayoutResult,
                        color = Color.White,
                        topLeft = Offset(tooltipX + tooltipPadding, tooltipY + tooltipPadding)
                    )
                }
            }
        }
        }
    }
}
