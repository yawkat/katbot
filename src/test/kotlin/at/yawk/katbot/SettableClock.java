package at.yawk.katbot;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import lombok.AllArgsConstructor;
import lombok.Setter;

/**
 * @author yawkat
 */
@AllArgsConstructor
@Setter
class SettableClock extends Clock {
    private Instant instant;

    @Override
    public ZoneId getZone() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
