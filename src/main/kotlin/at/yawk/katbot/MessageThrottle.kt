package at.yawk.katbot

import java.time.Clock
import java.time.Instant
import java.util.*

private const val BACKLOG_SIZE = 32

private const val THRESHOLD_SAME = 60 // 60 seconds per same message
private const val THRESHOLD_DIFFERENT = 7 // 7 seconds per same message

/**
 * @author yawkat
 */
class MessageThrottle(val clock: Clock) {
    private val events: Queue<Event> = ArrayDeque(BACKLOG_SIZE)

    @Synchronized
    fun trySend(message: String): Boolean {
        // say we have three messages logged.
        // the first message must be older than 7 seconds, the second older than 3 and the last older than 1 second
        // to accept the new message.

        val now = clock.instant()
        for (event in events) {
            val threshold = if (event.message == message) THRESHOLD_SAME else THRESHOLD_DIFFERENT
            if (event.time.isAfter(now.minusSeconds(threshold.toLong()))) {
                return false
            }
        }
        // remove old backlog
        while (events.size >= BACKLOG_SIZE) {
            events.poll()
        }
        events.add(Event(now, message))
        return true
    }
}

internal data class Event(val time: Instant, val message: String)