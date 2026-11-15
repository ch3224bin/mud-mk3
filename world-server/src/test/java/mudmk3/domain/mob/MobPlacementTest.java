package mudmk3.domain.mob;

import org.junit.jupiter.api.Test;

import static mudmk3.domain.mob.MobFixture.rabbit;
import static mudmk3.domain.mob.MobFixture.wolf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MobPlacementTest {

    @Test
    void create() {
        MobType wolf = MobFixture.create(wolf());

        MobPlacement placement = MobPlacement.create(10L, wolf, 3);

        assertThat(placement.getRoomId()).isEqualTo(10L);
        assertThat(placement.getMobType()).isSameAs(wolf);
        assertThat(placement.getCount()).isEqualTo(3);
    }

    @Test
    void createAtBounds() {
        assertThat(MobPlacement.create(10L, MobFixture.create(wolf()), 1).getCount()).isEqualTo(1);
        assertThat(MobPlacement.create(10L, MobFixture.create(wolf()), 10).getCount()).isEqualTo(10);
    }

    @Test
    void update() {
        MobPlacement placement = MobPlacement.create(10L, MobFixture.create(wolf()), 3);
        MobType rabbit = MobFixture.create(rabbit());

        placement.update(rabbit, 5);

        assertThat(placement.getRoomId()).isEqualTo(10L);
        assertThat(placement.getMobType()).isSameAs(rabbit);
        assertThat(placement.getCount()).isEqualTo(5);
    }

    @Test
    void countRangeFail() {
        MobType wolf = MobFixture.create(wolf());

        for (int count : new int[]{0, 11}) {
            assertThatThrownBy(() -> MobPlacement.create(10L, wolf, count))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("몹 수는 1~10으로 정하십시오.");
        }

        MobPlacement placement = MobPlacement.create(10L, wolf, 3);
        MobType rabbit = MobFixture.create(rabbit());
        assertThatThrownBy(() -> placement.update(rabbit, 11))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("몹 수는 1~10으로 정하십시오.");
        // 실패한 고치기는 값을 바꾸지 않는다
        assertThat(placement.getMobType()).isSameAs(wolf);
        assertThat(placement.getCount()).isEqualTo(3);
    }
}
