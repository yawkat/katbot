package at.yawk.katbot;

import java.time.Instant;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * @author yawkat
 */
public class MessageThrottleTest {
    @Test
    public void test() {
        // we hit 0,1,3,7

        SettableClock clock = new SettableClock(Instant.ofEpochSecond(0));
        MessageThrottle throttle = new MessageThrottle(clock);
        assertTrue(throttle.trySend());
        assertFalse(throttle.trySend());
        clock.setInstant(Instant.ofEpochMilli(900));
        assertFalse(throttle.trySend());
        clock.setInstant(Instant.ofEpochSecond(1));
        assertTrue(throttle.trySend());
        clock.setInstant(Instant.ofEpochSecond(2));
        assertFalse(throttle.trySend());
        clock.setInstant(Instant.ofEpochSecond(3));
        assertTrue(throttle.trySend());
        clock.setInstant(Instant.ofEpochSecond(4));
        assertFalse(throttle.trySend());
        clock.setInstant(Instant.ofEpochSecond(5));
        assertFalse(throttle.trySend());
        clock.setInstant(Instant.ofEpochSecond(6));
        assertFalse(throttle.trySend());
        clock.setInstant(Instant.ofEpochSecond(7));
        assertTrue(throttle.trySend());
    }
}