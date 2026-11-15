package mudmk3.domain.combat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SpiritOutputTest {

    @Test
    void ratio() {
        assertThat(SpiritOutput.ratio(25, 25)).isEqualTo(1.0);
        assertThat(SpiritOutput.ratio(5, 25)).isEqualTo(1.0); // 20%
        assertThat(SpiritOutput.ratio(2.5, 25)).isCloseTo(0.75, within(1e-9));
        assertThat(SpiritOutput.ratio(1.25, 25)).isCloseTo(0.625, within(1e-9));
        assertThat(SpiritOutput.ratio(0, 25)).isEqualTo(0.5);
        assertThat(SpiritOutput.ratio(0, 0)).isEqualTo(1.0);
    }
}
