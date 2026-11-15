package mudmk3.domain.combat;

import mudmk3.domain.mob.MobKind;
import mudmk3.domain.shared.Element;
import org.junit.jupiter.api.Test;

import java.util.List;

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
    void receiverMultiplierIsOne() {
        assertThat(Damage.receiverMultiplier(AttackNature.NONE, null)).isEqualTo(1.0);
        assertThat(Damage.receiverMultiplier(new AttackNature(List.of(Element.FIRE), false), MobKind.GHOST)).isEqualTo(1.0);
        assertThat(Damage.receiverMultiplier(new AttackNature(List.of(), true), MobKind.BEAST)).isEqualTo(1.0);
        for (MobKind kind : MobKind.values()) {
            assertThat(Damage.receiverMultiplier(new AttackNature(List.of(Element.WATER, Element.EARTH), false), kind))
                    .isEqualTo(1.0);
        }
    }

    @Test
    void afterLayers() {
        assertThat(Damage.afterLayers(12, 1.0, 8)).isEqualTo(8);
        assertThat(Damage.afterLayers(12, 0.5, 8)).isEqualTo(2);
        assertThat(Damage.afterLayers(3, 1.0, 8)).isZero();
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
