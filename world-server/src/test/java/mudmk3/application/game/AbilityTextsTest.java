package mudmk3.application.game;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AbilityTextsTest {

    private final AbilityTexts texts = new AbilityTexts();

    @Test
    void effectSuffix() {
        assertThat(texts.effectSuffix(0.3, 20.5, 25)).isEqualTo("(피해 1 미만, 법력 20/25)");
        assertThat(texts.effectSuffix(7.6)).isEqualTo("(피해 8)");
    }
}
