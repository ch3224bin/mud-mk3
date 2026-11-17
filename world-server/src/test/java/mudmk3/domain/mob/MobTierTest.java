package mudmk3.domain.mob;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MobTierTest {

    @Test
    void bossHasNoAttackFactorNorRespawnDefault() {
        assertThat(MobTier.BOSS.attackFactor()).isEmpty();
        assertThat(MobTier.BOSS.defaultRespawnSeconds()).isEmpty();
    }

    @Test
    void normalFactorsAndRespawn() {
        assertThat(MobTier.NORMAL.attackFactor()).hasValue(0.6);
        assertThat(MobTier.NORMAL.defaultRespawnSeconds()).hasValue(90);
    }
}
