package mudmk3.domain.combat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConditionTest {

    @Test
    void boundaries() {
        assertThat(Condition.of(1000, 1000)).isEqualTo(Condition.HEALTHY);
        assertThat(Condition.of(750, 1000)).isEqualTo(Condition.HEALTHY);
        assertThat(Condition.of(749, 1000)).isEqualTo(Condition.HURT);
        assertThat(Condition.of(400, 1000)).isEqualTo(Condition.HURT);
        assertThat(Condition.of(399.9, 1000)).isEqualTo(Condition.BADLY_HURT);
        assertThat(Condition.of(150, 1000)).isEqualTo(Condition.BADLY_HURT);
        assertThat(Condition.of(149, 1000)).isEqualTo(Condition.DYING);
        assertThat(Condition.of(0, 1000)).isEqualTo(Condition.DYING);
    }

    @Test
    void names() {
        assertThat(Condition.HEALTHY.koreanName()).isEqualTo("멀쩡함");
        assertThat(Condition.HURT.sentence()).isEqualTo("다쳤다.");
        assertThat(Condition.BADLY_HURT.koreanName()).isEqualTo("크게 다침");
        assertThat(Condition.DYING.sentence()).isEqualTo("빈사 상태다.");
    }
}
