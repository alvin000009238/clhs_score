package com.clhs.score.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class GradesPagerNavigationTest {
    @Test
    fun interruptedPagerAnimationStillSettlesSelectedPage() {
        assertTrue(
            pagerNeedsSettling(
                currentPage = 0,
                currentPageOffsetFraction = 0.25f,
                destination = 0,
            ),
        )
    }
}
