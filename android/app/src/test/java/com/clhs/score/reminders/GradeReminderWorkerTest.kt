package com.clhs.score.reminders

import com.clhs.score.data.SchoolAuthenticationException
import com.clhs.score.data.SchoolException
import com.clhs.score.data.SchoolTransientException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.IOException

class GradeReminderWorkerTest {
    @Test
    fun failurePolicySeparatesAuthenticationTransientAndPermanentErrors() {
        assertEquals(
            ReminderFailureAction.STOP,
            reminderFailureAction(SchoolAuthenticationException()),
        )
        assertEquals(
            ReminderFailureAction.RETRY,
            reminderFailureAction(SchoolTransientException("temporary")),
        )
        assertEquals(ReminderFailureAction.RETRY, reminderFailureAction(IOException("offline")))
        assertEquals(
            ReminderFailureAction.COUNT_FAILURE,
            reminderFailureAction(SchoolException("permanent")),
        )
    }

    @Test
    fun failureMessagesDoNotExposeExceptionDetails() {
        val secret = "student=123456&token=secret"

        ReminderFailureAction.entries.forEach { action ->
            assertFalse(action.message.contains(secret))
        }
    }
}
