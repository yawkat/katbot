package at.yawk.katbot;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Queue;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
public class MessageThrottle {
    private static final int BACKLOG_SIZE = 32;

    private static final int THRESHOLD_SAME = 60; // 60 seconds per same message
    private static final int THRESHOLD_DIFFERENT = 7; // 7 seconds per same message

    private final Clock clock;
    private final Queue<Event> events = new ArrayDeque<>(BACKLOG_SIZE);

    public synchronized boolean trySend(String message) {
        // say we have three messages logged.
        // the first message must be older than 7 seconds, the second older than 3 and the last older than 1 second
        // to accept the new message.

        Instant now = clock.instant();
        for (Event event : events) {
            int threshold = event.message.equals(message) ? THRESHOLD_SAME : THRESHOLD_DIFFERENT;
            if (event.time.isAfter(now.minusSeconds(threshold))) {
                return false;
            }
        }
        // remove old backlog
        while (events.size() >= BACKLOG_SIZE) { events.poll(); }
        events.add(new Event(now, message));
        return true;
    }

    @Value
    private static class Event {
        private final Instant time;
        private final String message;
    }
}
