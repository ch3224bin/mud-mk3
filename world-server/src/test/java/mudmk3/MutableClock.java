package mudmk3;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static java.util.Objects.requireNonNull;

/** 테스트 시계: {@code 2026-11-14T00:00:00Z} 에서 시작하고 {@link #advance} 로만 간다. 실제로 기다리지 않고 시간을 흘린다 */
public class MutableClock extends Clock {

    public static final Instant START = Instant.parse("2026-11-14T00:00:00Z");

    private volatile Instant now = START;

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    /** 지금 시각에 멈춘 시계(다른 시간대) */
    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(now, zone);
    }

    @Override
    public Instant instant() {
        return now;
    }

    public synchronized void advance(Duration duration) {
        now = now.plus(requireNonNull(duration));
    }

    /** 시작 시각으로 되돌린다. 시계를 움직인 테스트는 끝에 부른다(컨텍스트가 테스트끼리 공유됨) */
    public void reset() {
        now = START;
    }
}
