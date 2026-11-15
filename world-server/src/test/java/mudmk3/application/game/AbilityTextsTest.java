package mudmk3.application.game;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AbilityTextsTest {

    private final AbilityTexts texts = new AbilityTexts();

    @Test
    void remaining() {
        assertThat(AbilityTexts.remaining(Duration.ofMillis(300))).isEqualTo("0.3초");
        assertThat(AbilityTexts.remaining(Duration.ofMillis(1_350))).isEqualTo("1.4초");
        assertThat(AbilityTexts.remaining(Duration.ofMillis(1))).isEqualTo("0.1초");
        assertThat(AbilityTexts.remaining(Duration.ofMillis(59_950))).isEqualTo("1분");
        assertThat(AbilityTexts.remaining(Duration.ofMillis(61_200))).isEqualTo("1분 2초");
    }

    @Test
    void effectSuffix() {
        assertThat(texts.effectSuffix(0.3, 20.5, 25)).isEqualTo("(피해 1 미만, 법력 20/25)");
        assertThat(texts.effectSuffix(7.6)).isEqualTo("(피해 8)");
    }
}
