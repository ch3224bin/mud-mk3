package mudmk3.application.game;

import mudmk3.domain.combat.CombatFixture;
import mudmk3.domain.combat.MobInstance;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MobGroupKeyTest {

    private static final Instant NOW = Instant.parse("2026-11-16T00:00:00Z");

    @Test
    void sameTypeAndSameFightingShareKey() {
        MobInstance first = MobInstance.spawn(1, 100L, 0, 10L, 2, CombatFixture.wolfProfile(), NOW);
        MobInstance second = MobInstance.spawn(2, 100L, 1, 10L, 2, CombatFixture.wolfProfile(), NOW);

        assertThat(MobGroupKey.of(first)).isEqualTo(MobGroupKey.of(second));
        assertThat(MobGroupKey.of(first)).isEqualTo(new MobGroupKey(2L, false));
    }

    @Test
    void differentFightingSplitsKey() {
        MobInstance idle = MobInstance.spawn(1, 100L, 0, 10L, 2, CombatFixture.wolfProfile(), NOW);
        MobInstance fighting = MobInstance.spawn(2, 100L, 1, 10L, 2, CombatFixture.wolfProfile(), NOW);
        fighting.engage(7L, NOW);

        assertThat(MobGroupKey.of(fighting)).isEqualTo(new MobGroupKey(2L, true));
        assertThat(MobGroupKey.of(idle)).isNotEqualTo(MobGroupKey.of(fighting));
    }

    @Test
    void differentTypeSplitsKey() {
        MobInstance wolf = MobInstance.spawn(1, 100L, 0, 10L, 1, CombatFixture.wolfProfile(), NOW);
        MobInstance rabbit = MobInstance.spawn(2, 101L, 0, 10L, 1, CombatFixture.rabbitProfile(), NOW);

        assertThat(MobGroupKey.of(wolf)).isNotEqualTo(MobGroupKey.of(rabbit));
    }
}
