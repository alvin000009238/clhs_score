package com.clhs.score.ui.schedule

import com.clhs.score.data.ScheduleChange
import com.clhs.score.data.ScheduleChangeType
import com.clhs.score.data.ScheduleItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleGridTest {
    @Test
    fun consecutiveIdenticalCoursesShareOneCell() {
        val items = listOf(
            lesson(period = 1),
            lesson(period = 2),
            lesson(period = 3, classroom = "不同教室"),
            lesson(period = 4),
        )
        val changes = listOf(
            ScheduleChange(
                type = ScheduleChangeType.MODIFIED,
                dayOfWeek = 1,
                period = 4,
                weekItem = items.last(),
            ),
        )

        val cells = scheduleGridCells(items, changes, dayOfWeek = 1, periods = 1..4)

        assertEquals(listOf(1, 3, 4), cells.map { it.period })
        assertEquals(listOf(2, 1, 1), cells.map { it.periodCount })
    }

    @Test
    fun extraPeriodsRemainVisible() {
        val cells = scheduleGridCells(
            items = listOf(lesson(period = 9)),
            changes = emptyList(),
            dayOfWeek = 1,
        )

        assertEquals(9, cells.last().period)
        assertEquals("國文", cells.last().item?.subjectName)
    }

    private fun lesson(period: Int, classroom: String = "101") =
        ScheduleItem(
            dayOfWeek = 1,
            period = period,
            subjectName = "國文",
            teacherName = "王老師",
            classroom = classroom,
        )
}
