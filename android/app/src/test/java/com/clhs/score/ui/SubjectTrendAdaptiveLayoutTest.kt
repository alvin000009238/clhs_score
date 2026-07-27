package com.clhs.score.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubjectTrendAdaptiveLayoutTest {
    @Test
    fun wideWindowsUseSplitLayout() {
        assertFalse(subjectTrendUsesSplitLayout(599.dp))
        assertTrue(subjectTrendUsesSplitLayout(600.dp))
        assertTrue(subjectTrendUsesSplitLayout(840.dp))
    }
}
