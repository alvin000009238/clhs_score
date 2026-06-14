package com.clhs.score.analytics

interface AnalyticsLogger {
    fun logEvent(name: String, parameters: Map<String, Any?> = emptyMap())
}

object NoOpAnalyticsLogger : AnalyticsLogger {
    override fun logEvent(name: String, parameters: Map<String, Any?>) = Unit
}
