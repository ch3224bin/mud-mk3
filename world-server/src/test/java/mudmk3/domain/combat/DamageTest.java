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

    @Test
    void afterBody() {
        assertThat(Damage.afterBody(12, 6)).isEqualTo(9);
        assertThat(Damage.afterBody(5, 18)).isZero();
    }

    @Test
    void display() {
        assertThat(Damage.display(0)).isEqualTo("0");
        assertThat(Damage.display(0.3)).isEqualTo("1 미만");
        assertThat(Damage.display(0.5)).isEqualTo("1");
        assertThat(Damage.display(8.6)).isEqualTo("9");
    }

    @Test
    void playerPower() {
        assertThat(Damage.playerPower(5)).isEqualTo(12);
        assertThat(Damage.playerPower(0)).isEqualTo(3);
    }
}
