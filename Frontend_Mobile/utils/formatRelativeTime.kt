package com.example.recipeapp.utils

import org.threeten.bp.Duration
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import parseCreatedAt



fun formatRelativeTime(createdAt: String?, currentTime: LocalDateTime): String {
    if (createdAt.isNullOrBlank()) return ""
    // parseCreatedAt returns an OffsetDateTime
    val createdTime = parseCreatedAt(createdAt) ?: return ""

    // Convert the OffsetDateTime to ZonedDateTime, then to the device's local time
    val createdTimeLocal = createdTime.toZonedDateTime()
        .withZoneSameInstant(ZoneId.systemDefault())
        .toLocalDateTime()

    val duration = Duration.between(createdTimeLocal, currentTime)
    if (duration.isNegative) return "Just now"

    return when {
        duration.toMinutes() < 1 -> "Just now"
        duration.toMinutes() < 60 -> "${duration.toMinutes()} min ago"
        duration.toHours() < 24 -> "${duration.toHours()} hour${if (duration.toHours() > 1) "s" else ""} ago"
        else -> "${duration.toDays()} day${if (duration.toDays() > 1) "s" else ""} ago"
    }
}
