package incident.server.utils

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Gets the current time as an Instant.
 */
fun currentInstant(): Instant {
    return Clock.System.now()
}

/**
 * Returns the system's default TimeZone.
 */
fun defaultTimeZone(): TimeZone {
    return TimeZone.currentSystemDefault()
}

/**
 * Converts an Instant to LocalDateTime using the default timezone.
 */
fun Instant.toDefaultLocalDateTime(): LocalDateTime {
    return this.toLocalDateTime(defaultTimeZone())
}

/**
 * Converts a LocalDateTime to an Instant using the default timezone.
 */
fun LocalDateTime.toDefaultInstant(): Instant {
    return this.toInstant(defaultTimeZone())
}

/**
 * Gets the current time as a LocalDateTime in the default timezone.
 */
fun currentLocalDateTime(): LocalDateTime {
    return currentInstant().toDefaultLocalDateTime()
}
