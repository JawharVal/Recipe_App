import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

import org.threeten.bp.OffsetDateTime

fun parseCreatedAt(createdAt: String): OffsetDateTime? {
    return try {
        OffsetDateTime.parse(createdAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    } catch (e: Exception) {
        e.printStackTrace() // Log the error for debugging
        null
    }
}
