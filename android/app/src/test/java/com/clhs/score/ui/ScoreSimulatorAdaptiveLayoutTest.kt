package com.clhs.score.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreSimulatorAdaptiveLayoutTest {
    @Test
    fun wideWindowsUseSplitLayout() {
        assertFalse(scoreSimulatorUsesSplitLayout(599.dp))
        assertTrue(scoreSimulatorUsesSplitLayout(600.dp))
        assertTrue(scoreSimulatorUsesSplitLayout(840.dp))
    }

    @Test
    fun splitSummaryCentersInsideSystemBars() {
        assertEquals(
            144,
            scoreSimulatorSummaryOffsetPx(
                usesSplitLayout = true,
                viewportHeightPx = 400,
                cardHeightPx = 120,
                statusBarHeightPx = 24,
                topSpacerHeightPx = 80,
                navigationBarHeightPx = 16,
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 0,
            ),
        )
    }

    @Test
    fun compactSummaryUsesUpdatedInsetsAfterRotation() {
        assertEquals(
            104,
            scoreSimulatorSummaryOffsetPx(
                usesSplitLayout = false,
                viewportHeightPx = 800,
                cardHeightPx = 160,
                statusBarHeightPx = 48,
                topSpacerHeightPx = 104,
                navigationBarHeightPx = 48,
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 0,
            ),
        )
    }
}
