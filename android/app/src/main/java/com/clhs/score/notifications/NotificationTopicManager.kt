package com.clhs.score.notifications

import com.google.firebase.messaging.FirebaseMessaging

interface TopicSubscriptionClient {
    fun subscribe(topic: String)
    fun unsubscribe(topic: String)
}

object FirebaseTopicSubscriptionClient : TopicSubscriptionClient {
    override fun subscribe(topic: String) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
    }

    override fun unsubscribe(topic: String) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
    }
}

class NotificationTopicManager(
    private val client: TopicSubscriptionClient = FirebaseTopicSubscriptionClient,
) {
    fun setNotificationsEnabled(enabled: Boolean) {
        TOPICS.forEach { topic ->
            if (enabled) {
                client.subscribe(topic)
            } else {
                client.unsubscribe(topic)
            }
        }
    }

    companion object {
        const val GENERAL_TOPIC = "general"
        const val APP_UPDATES_TOPIC = "app_updates"
        val TOPICS = listOf(GENERAL_TOPIC, APP_UPDATES_TOPIC)
    }
}
