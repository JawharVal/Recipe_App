package com.example.recipeapp.utils

import org.threeten.bp.Duration
import org.threeten.bp.ZonedDateTime

fun formatRelativeTimeZoned(createdAt: String?, currentTime: ZonedDateTime): String {
    if (createdAt.isNullOrBlank()) return ""
    val createdTime = parseCreatedAtAsZoned(createdAt) ?: return ""

    val duration = Duration.between(createdTime, currentTime)
    // If for any reason the duration is negative, return "Just now"
    if (duration.isNegative) return "Just now"

    return when {
        duration.toMinutes() < 1 -> "Just now"
        duration.toMinutes() < 60 -> "${duration.toMinutes()} min ago"
        duration.toHours() < 24 -> "${duration.toHours()} hour${if (duration.toHours() > 1) "s" else ""} ago"
        else -> "${duration.toDays()} day${if (duration.toDays() > 1) "s" else ""} ago"
    }
}
