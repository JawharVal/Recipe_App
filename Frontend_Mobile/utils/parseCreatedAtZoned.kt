package com.example.recipeapp.utils
import android.util.Log
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

fun parseCreatedAtAsZoned(createdAt: String): ZonedDateTime? {
    return try {
        // If the string contains a 'T', use the ISO_LOCAL_DATE_TIME formatter.
        val localDateTime = if (createdAt.contains("T")) {
            LocalDateTime.parse(createdAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } else {
            // Otherwise, use a pattern with a space.
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            LocalDateTime.parse(createdAt, formatter)
        }
        Log.d("RelativeTime", "Parsed LocalDateTime: $localDateTime")
        // Assume the backend time is in America/New_York. Change if needed.
        val serverZone = ZoneId.of("America/New_York")
        val serverZoned = localDateTime.atZone(serverZone)
        val deviceZoned = serverZoned.withZoneSameInstant(ZoneId.systemDefault())
        Log.d("RelativeTime", "Device ZonedDateTime: $deviceZoned")
        deviceZoned
    } catch (e: Exception) {
        Log.e("RelativeTime", "Error parsing createdAt: $createdAt", e)
        null
    }
}
