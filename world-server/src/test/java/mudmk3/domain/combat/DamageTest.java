package mudmk3.domain.combat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DamageTest {

    @Test
    void gauge() {
        assertThat(Damage.gauge(0.4)).isEqualTo(1);
        assertThat(Damage.gauge(19.8)).isEqualTo(19);
        assertThat(Damage.gauge(0)).isZero();
    }
}
