package mudmk3.domain.combat;

import mudmk3.MutableClock;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerCombatTest {

    private static final Instant T = MutableClock.START;

    @Test
    void engageInOrder() {
        PlayerCombat combat = new PlayerCombat(7L);
        combat.engage(3);
        combat.engage(1);
        combat.engage(3);

        assertThat(combat.getCharacterId()).isEqualTo(7L);
        assertThat(combat.getMobIds()).containsExactly(3L, 1L);
        assertThat(combat.isFighting()).isTrue();
    }

    @Test
    void currentTargetFallsBackToFirst() {
        PlayerCombat combat = new PlayerCombat(7L);

        assertThat(combat.currentTarget()).isEmpty();

        combat.engage(3);
        combat.engage(1);

        assertThat(combat.currentTarget()).contains(3L);

        combat.target(1);

        assertThat(combat.currentTarget()).contains(1L);

        combat.disengage(1, T);

        assertThat(combat.getTargetMobId()).isNull();
        assertThat(combat.currentTarget()).contains(3L);
    }

    @Test
    void readyAfterInterval() {
        PlayerCombat combat = new PlayerCombat(7L);

        assertThat(combat.readyAt(T)).isTrue();

        combat.attackedAt(T, T);

        assertThat(combat.getNextAttackAt()).isEqualTo(T.plusSeconds(3));
        assertThat(combat.readyAt(T.plusMillis(2_900))).isFalse();
        assertThat(combat.readyAt(T.plusSeconds(3))).isTrue();
    }

    @Test
    void intervalStaysAfterCombatEnds() {
        PlayerCombat combat = new PlayerCombat(7L);
        combat.engage(3);
        combat.attackedAt(T, T);

        combat.endAll(T.plusSeconds(1));
        combat.engage(4);

        assertThat(combat.readyAt(T.plusSeconds(2))).isFalse();
    }

    @Test
    void disengageLastSchedulesRefill() {
        PlayerCombat combat = new PlayerCombat(7L);
        combat.engage(3);
        combat.engage(4);
        combat.disengage(3, T);

        assertThat(combat.getRefillAt()).isNull();

        combat.disengage(4, T);

        assertThat(combat.isFighting()).isFalse();
        assertThat(combat.getRefillAt()).isEqualTo(T.plusSeconds(10));
    }

    @Test
    void endAllSchedulesRefill() {
        PlayerCombat combat = new PlayerCombat(7L);
        combat.engage(3);
        combat.engage(4);
        combat.target(4);

        combat.endAll(T);

        assertThat(combat.getMobIds()).isEmpty();
        assertThat(combat.getTargetMobId()).isNull();
        assertThat(combat.getRefillAt()).isEqualTo(T.plusSeconds(10));
    }

    @Test
    void endAllWithoutOpponentsKeepsRefill() {
        PlayerCombat combat = new PlayerCombat(7L);
        combat.scheduleRefill(T);

        combat.endAll(T.plusSeconds(5));

        assertThat(combat.getRefillAt()).isEqualTo(T.plusSeconds(10));
    }

    @Test
    void refillDue() {
        PlayerCombat combat = new PlayerCombat(7L);

        assertThat(combat.refillDue(T)).isFalse();

        combat.scheduleRefill(T);

        assertThat(combat.refillDue(T.plusMillis(9_900))).isFalse();
        assertThat(combat.refillDue(T.plusSeconds(10))).isTrue();

        combat.clearRefill();

        assertThat(combat.refillDue(T.plusSeconds(10))).isFalse();
    }

    @Test
    void engageCancelsRefill() {
        PlayerCombat combat = new PlayerCombat(7L);
        combat.scheduleRefill(T);

        combat.engage(3);

        assertThat(combat.getRefillAt()).isNull();
    }

    @Test
    void manaRegenSecondsNullIsZero() {
        PlayerCombat combat = new PlayerCombat(7L);

        assertThat(combat.getManaRegenAt()).isNull();
        assertThat(combat.manaRegenSeconds(T)).isZero();
        assertThat(combat.getManaRegenAt()).isEqualTo(T);
    }

    @Test
    void manaRegenSecondsClampedToOne() {
        PlayerCombat combat = new PlayerCombat(7L);
        combat.startManaRegen(T);

        assertThat(combat.manaRegenSeconds(T.plusSeconds(5))).isEqualTo(1);
        assertThat(combat.getManaRegenAt()).isEqualTo(T.plusSeconds(5));
        assertThat(combat.manaRegenSeconds(T.plusSeconds(4))).isZero();
    }

    @Test
    void manaRegenSecondsHalf() {
        PlayerCombat combat = new PlayerCombat(7L);
        combat.startManaRegen(T);

        assertThat(combat.manaRegenSeconds(T.plusMillis(500))).isEqualTo(0.5);
        assertThat(combat.manaRegenSeconds(T.plusMillis(1_500))).isEqualTo(1);
    }

    @Test
    void delayAttackUntilKeepsLater() {
        PlayerCombat combat = new PlayerCombat(7L);

        combat.delayAttackUntil(T.plusSeconds(2));
        assertThat(combat.getNextAttackAt()).isEqualTo(T.plusSeconds(2));

        combat.attackedAt(T.plusSeconds(2), T.plusSeconds(2)); // 다음 5초
        combat.delayAttackUntil(T.plusSeconds(4));
        assertThat(combat.getNextAttackAt()).isEqualTo(T.plusSeconds(5));

        combat.delayAttackUntil(T.plusSeconds(6));
        assertThat(combat.getNextAttackAt()).isEqualTo(T.plusSeconds(6));
        assertThat(combat.readyAt(T.plusSeconds(5))).isFalse();
    }
}
