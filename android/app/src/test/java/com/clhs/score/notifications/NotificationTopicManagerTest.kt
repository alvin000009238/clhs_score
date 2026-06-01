package com.clhs.score.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationTopicManagerTest {
    @Test
    fun enablingNotificationsSubscribesToAppTopics() {
        val client = RecordingTopicSubscriptionClient()
        val manager = NotificationTopicManager(client)

        manager.setNotificationsEnabled(true)

        assertEquals(
            listOf(
                "subscribe:general",
                "subscribe:app_updates",
            ),
            client.events,
        )
    }

    @Test
    fun disablingNotificationsUnsubscribesFromAppTopics() {
        val client = RecordingTopicSubscriptionClient()
        val manager = NotificationTopicManager(client)

        manager.setNotificationsEnabled(false)

        assertEquals(
            listOf(
                "unsubscribe:general",
                "unsubscribe:app_updates",
            ),
            client.events,
        )
    }

    private class RecordingTopicSubscriptionClient : TopicSubscriptionClient {
        val events = mutableListOf<String>()

        override fun subscribe(topic: String) {
            events += "subscribe:$topic"
        }

        override fun unsubscribe(topic: String) {
            events += "unsubscribe:$topic"
        }
    }
}
