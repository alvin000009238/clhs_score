package com.clhs.score.data

import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

fun String.canonicalDisplayValue(): String? {
    val trimmed = trim()
    if (trimmed.isBlank()) return null
    return trimmed.toDoubleOrNull()?.canonicalNumberValue() ?: trimmed
}

fun Double?.canonicalNumberValue(): String? = this?.canonicalNumberValue()

fun Double.canonicalNumberValue(): String {
    if (abs(this - round(this)) < 0.0001) {
        return String.format(Locale.US, "%.0f", this)
    }
    return String.format(Locale.US, "%.2f", this)
        .trimEnd('0')
        .trimEnd('.')
}

fun Double.formatScore(): String {
    return if (this == kotlin.math.floor(this) && !this.isInfinite()) {
        this.toInt().toString()
    } else {
        "%.1f".format(this)
    }
}
