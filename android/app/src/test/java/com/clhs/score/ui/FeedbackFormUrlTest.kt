package com.clhs.score.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackFormUrlTest {
    @Test
    fun allowsOnlyGoogleFormsHttpsUrls() {
        assertTrue(isAllowedFeedbackFormUrl("https://forms.gle/AbCdEf"))
        assertTrue(isAllowedFeedbackFormUrl("https://docs.google.com/forms/d/e/example/viewform"))

        assertFalse(isAllowedFeedbackFormUrl(""))
        assertFalse(isAllowedFeedbackFormUrl("http://forms.gle/AbCdEf"))
        assertFalse(isAllowedFeedbackFormUrl("https://forms.gle.evil.example/AbCdEf"))
        assertFalse(isAllowedFeedbackFormUrl("https://docs.google.com/document/d/example"))
        assertFalse(isAllowedFeedbackFormUrl("https://forms.gle@evil.example/AbCdEf"))
    }
}
