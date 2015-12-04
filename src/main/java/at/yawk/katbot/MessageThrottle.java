package at.yawk.katbot;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Queue;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
public class MessageThrottle {
    private static final int BACKLOG_SIZE = 8;

    private final Clock clock;
    private final Queue<Instant> messageTimes = new ArrayDeque<>(BACKLOG_SIZE);

    public synchronized boolean trySend() {
        // say we have three messages logged.
        // the first message must be older than 7 seconds, the second older than 3 and the last older than 1 second
        // to accept the new message.

        Instant now = clock.instant();
        int index = messageTimes.size() - 1;
        for (Instant time : messageTimes) {
            if (time.isAfter(now.minusSeconds((2L << index) - 1))) {
                return false;
            }
            index--;
        }
        while (messageTimes.size() >= BACKLOG_SIZE) { messageTimes.poll(); }
        messageTimes.add(now);
        return true;
    }
}
