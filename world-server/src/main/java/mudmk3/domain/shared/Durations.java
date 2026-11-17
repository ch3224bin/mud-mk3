package mudmk3.domain.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

import static java.util.Objects.requireNonNull;

/** 화면에 보이는 시간을 룰북 출력 문체로 쓴다 */
public final class Durations {

    private static final BigDecimal MINUTE = BigDecimal.valueOf(60);
    private static final BigDecimal HOUR = BigDecimal.valueOf(3_600);
    private static final long NANOS_PER_TENTH = 100_000_000L;
    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    private static final long MINUTE_NANOS = 60 * NANOS_PER_SECOND;

    private Durations() {
    }

    /**
     * 60초 미만은 초(끝 0을 지움: {@code 3초}, {@code 1.5초}), 60초 이상은 분과 남은 초({@code 1분}, {@code 7분 12초},
     * {@code 1분 1.5초}), 3,600초 이상은 시간과 남은 분({@code 1시간}, {@code 1시간 5분} — 초는 버림)
     */
    public static String korean(BigDecimal seconds) {
        requireNonNull(seconds);
        if (seconds.compareTo(MINUTE) < 0) {
            return Numbers.decimal(seconds) + "초";
        }
        if (seconds.compareTo(HOUR) < 0) {
            BigDecimal minutes = seconds.divide(MINUTE, 0, RoundingMode.DOWN);
            BigDecimal rest = seconds.subtract(minutes.multiply(MINUTE));
            return minutes.toPlainString() + "분" + (rest.signum() == 0 ? "" : " " + Numbers.decimal(rest) + "초");
        }
        BigDecimal hours = seconds.divide(HOUR, 0, RoundingMode.DOWN);
        BigDecimal minutes = seconds.subtract(hours.multiply(HOUR)).divide(MINUTE, 0, RoundingMode.DOWN);
        return hours.toPlainString() + "시간" + (minutes.signum() == 0 ? "" : " " + minutes.toPlainString() + "분");
    }

    /**
     * 남은 시간(D112): 60초 미만은 0.1초 단위로 올림({@code 0.0초} 가 보이지 않게), 60초 이상은 1초 단위로 올림한 뒤
     * {@link #korean} 문체
     */
    public static String remaining(Duration duration) {
        long nanos = Math.max(0, duration.toNanos());
        if (nanos < MINUTE_NANOS) {
            return korean(BigDecimal.valueOf(Math.ceilDiv(nanos, NANOS_PER_TENTH), 1));
        }
        return korean(BigDecimal.valueOf(Math.ceilDiv(nanos, NANOS_PER_SECOND)));
    }
}
