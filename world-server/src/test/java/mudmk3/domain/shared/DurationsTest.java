package mudmk3.domain.shared;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DurationsTest {

    @Test
    void seconds() {
        assertThat(korean("3")).isEqualTo("3초");
        assertThat(korean("3.0")).isEqualTo("3초");
        assertThat(korean("1.5")).isEqualTo("1.5초");
        assertThat(korean("0.1")).isEqualTo("0.1초");
        assertThat(korean("59.9")).isEqualTo("59.9초");
    }

    @Test
    void minutes() {
        assertThat(korean("60")).isEqualTo("1분");
        assertThat(korean("60.0")).isEqualTo("1분");
        assertThat(korean("432")).isEqualTo("7분 12초");
        assertThat(korean("61.5")).isEqualTo("1분 1.5초");
        assertThat(korean("3599.9")).isEqualTo("59분 59.9초");
    }

    @Test
    void hours() {
        assertThat(korean("3600")).isEqualTo("1시간");
        assertThat(korean("3600.0")).isEqualTo("1시간");
        assertThat(korean("3900")).isEqualTo("1시간 5분");
        assertThat(korean("3659.5")).isEqualTo("1시간");
        assertThat(korean("7260")).isEqualTo("2시간 1분");
    }

    private static String korean(String seconds) {
        return Durations.korean(new BigDecimal(seconds));
    }
}
