package mudmk3.domain.combat;

import mudmk3.MutableClock;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MobInstanceTest {

    private static final Instant T = MutableClock.START;
    private static final Long A = 1L;
    private static final Long B = 2L;

    @Test
    void spawn() {
        MobInstance mob = CombatFixture.wolf(T);

        assertThat(mob.getId()).isEqualTo(1);
        assertThat(mob.getPlacementId()).isEqualTo(100L);
        assertThat(mob.getSlot()).isZero();
        assertThat(mob.getRoomId()).isEqualTo(10L);
        assertThat(mob.getPlacementCount()).isEqualTo(1);
        assertThat(mob.getProfile().name()).isEqualTo("청모랑");
        assertThat(mob.getQiBlood()).isEqualTo(49);
        assertThat(mob.isAlive()).isTrue();
        assertThat(mob.isFighting()).isFalse();
        assertThat(mob.condition()).isEqualTo(Condition.HEALTHY);
        assertThat(mob.target()).isEmpty();
        assertThat(mob.getNextActionAt()).isNull();
    }

    @Test
    void profileOf() {
        MobProfile profile = CombatFixture.wolfProfile();

        assertThat(profile.mobTypeId()).isEqualTo(2L);
        assertThat(profile.description()).isEqualTo("청모랑의 묘사다.");
        assertThat(profile.attackName()).isEqualTo("물어뜯기");
        assertThat(profile.stats().qiBlood()).isEqualTo(49);
        assertThat(profile.stats().body()).isEqualTo(8);
        assertThat(profile.respawnSeconds()).isEqualTo(90);
        assertThat(profile.rankName()).isEqualTo("1급");
    }

    @Test
    void firstEngageSchedulesAfterOnePointFive() {
        MobInstance mob = CombatFixture.wolf(T);

        assertThat(mob.engage(A, T)).isTrue();
        assertThat(mob.engage(A, T.plusSeconds(1))).isFalse();

        assertThat(mob.getNextActionAt()).isEqualTo(T.plusMillis(1500));
        assertThat(mob.isFighting()).isTrue();
        assertThat(mob.opponentIds()).containsExactly(A);
    }

    @Test
    void secondEngageKeepsSchedule() {
        MobInstance mob = CombatFixture.wolf(T);
        mob.engage(A, T);

        assertThat(mob.engage(B, T.plusSeconds(1))).isTrue();

        assertThat(mob.getNextActionAt()).isEqualTo(T.plusMillis(1500));
        assertThat(mob.opponentIds()).containsExactly(A, B);
    }

    @Test
    void firstOpponentThreatOne() {
        MobInstance mob = CombatFixture.wolf(T);
        mob.engage(A, T);
        mob.engage(B, T);

        assertThat(mob.engagementOf(A)).hasValueSatisfying(e -> assertThat(e.threat()).isEqualTo(1));
        assertThat(mob.engagementOf(B)).hasValueSatisfying(e -> assertThat(e.threat()).isZero());
        assertThat(mob.target()).contains(A);
    }

    @Test
    void receiveAppliesBody() {
        MobInstance mob = CombatFixture.wolf(T);
        mob.engage(A, T);

        MobHit hit = mob.receive(A, 12, T);

        assertThat(hit).isEqualTo(new MobHit(8, false));
        assertThat(mob.getQiBlood()).isEqualTo(41);
        assertThat(mob.engagementOf(A)).hasValueSatisfying(e -> {
            assertThat(e.threat()).isEqualTo(13);
            assertThat(e.dealt()).isEqualTo(12);
        });
        assertThat(mob.condition()).isEqualTo(Condition.HEALTHY);

        mob.receive(A, 12, T); // 기혈 33, 67%

        assertThat(mob.condition()).isEqualTo(Condition.HURT);
    }

    @Test
    void receiveFromNonOpponentFail() {
        MobInstance mob = CombatFixture.wolf(T);

        assertThatThrownBy(() -> mob.receive(A, 12, T)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void targetSwitchNeedsOnePointOneTimes() {
        MobInstance mob = CombatFixture.wolf(T);
        mob.engage(A, T);
        mob.engage(B, T);
        mob.receive(A, 9, T);   // A 위협 10
        mob.receive(B, 11, T);  // B 위협 11

        assertThat(mob.target()).contains(A);

        mob.receive(B, 0.1, T); // B 위협 11.1

        assertThat(mob.target()).contains(B);
        assertThat(mob.getTargetCharacterId()).isEqualTo(B);
    }

    @Test
    void targetFallsBackWhenRemoved() {
        MobInstance mob = CombatFixture.wolf(T);
        mob.engage(A, T);
        mob.engage(B, T);
        mob.engage(3L, T);
        mob.receive(3L, 5, T);
        mob.receive(B, 6, T);

        mob.disengage(A, T);

        assertThat(mob.target()).contains(B);
    }

    @Test
    void killerTopDamageTieEarlier() {
        MobInstance mob = CombatFixture.wolf(T);
        mob.engage(A, T);
        mob.engage(B, T);
        mob.receive(B, 10, T);
        mob.receive(A, 10, T);

        assertThat(mob.killer()).contains(B);

        mob.receive(A, 1, T);

        assertThat(mob.killer()).contains(A);
    }

    @Test
    void killerKeepsDamageOfLeftOpponent() {
        MobInstance mob = CombatFixture.wolf(T);
        mob.engage(A, T);
        mob.engage(B, T);
        mob.receive(A, 30, T);  // 피해 26, 기혈 23
        mob.disengage(A, T);

        MobHit hit = mob.receive(B, 28, T); // 피해 24

        assertThat(hit.died()).isTrue();
        assertThat(mob.killer()).contains(A);
    }

    @Test
    void killerEmptyWithoutDamage() {
        MobInstance mob = CombatFixture.wolf(T);
        mob.engage(A, T);

        assertThat(mob.killer()).isEmpty();
    }

    @Test
    void diesAndSchedulesRespawn() {
        MobInstance mob = CombatFixture.wolf(T);
        mob.engage(A, T);
        mob.engage(B, T);
        mob.receive(B, 10, T);

        MobHit hit = mob.receive(A, 100, T.plusSeconds(5));

        assertThat(hit.died()).isTrue();
        assertThat(hit.damage()).isEqualTo(96);
        assertThat(mob.isAlive()).isFalse();
        assertThat(mob.getQiBlood()).isZero();
        assertThat(mob.isFighting()).isFalse();
        assertThat(mob.opponentIds()).isEmpty();
        assertThat(mob.target()).isEmpty();
        assertThat(mob.killer()).contains(A);
        assertThat(mob.getNextActionAt()).isNull();
        assertThat(mob.getRefillAt()).isNull();
        assertThat(mob.getRespawnAt()).isEqualTo(T.plusSeconds(95));
        assertThat(mob.respawnDue(T.plusSeconds(94))).isFalse();
        assertThat(mob.respawnDue(T.plusSeconds(95))).isTrue();
    }

    @Test
    void engageDeadFail() {
        MobInstance mob = CombatFixture.wolf(T);
        mob.engage(A, T);
        mob.receive(A, 200, T);

        assertThatThrownBy(() -> mob.engage(A, T)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void disengageLastSchedulesRefill() {
        MobInstance mob = CombatFixture.wolf(T);
        mob.engage(A, T);
        mob.engage(B, T);
        mob.disengage(A, T.plusSeconds(2));

        assertThat(mob.getRefillAt()).isNull();

        mob.disengage(B, T.plusSeconds(3));

        assertThat(mob.isFighting()).isFalse();
        assertThat(mob.getRefillAt()).isEqualTo(T.plusSeconds(13));
        assertThat(mob.getNextActionAt()).isNull();
        assertThat(mob.target()).isEmpty();
    }

    @Test
    void refillAfterTenSeconds() {
        MobInstance mob = CombatFixture.wolf(T);
        mob.engage(A, T);
        mob.receive(A, 12, T);
        mob.disengage(A, T);

        mob.refillIfDue(T.plusMillis(9_900));

        assertThat(mob.getQiBlood()).isEqualTo(41);
        assertThat(mob.killer()).contains(A);

        mob.refillIfDue(T.plusSeconds(10));

        assertThat(mob.getQiBlood()).isEqualTo(49);
        assertThat(mob.getRefillAt()).isNull();
        assertThat(mob.killer()).isEmpty();
        mob.engage(B, T.plusSeconds(11));
        mob.engage(A, T.plusSeconds(11));
        assertThat(mob.engagementOf(A)).hasValueSatisfying(e -> {
            assertThat(e.threat()).isZero();
            assertThat(e.dealt()).isZero();
        });
    }

    @Test
    void reengageCancelsRefill() {
        MobInstance mob = CombatFixture.wolf(T);
        mob.engage(A, T);
        mob.receive(A, 12, T);
        mob.disengage(A, T);

        mob.engage(B, T.plusSeconds(5));
        mob.refillIfDue(T.plusSeconds(10));

        assertThat(mob.getRefillAt()).isNull();
        assertThat(mob.getQiBlood()).isEqualTo(41);
        assertThat(mob.getNextActionAt()).isEqualTo(T.plusMillis(6_500));
        assertThat(mob.engagementOf(B)).hasValueSatisfying(e -> assertThat(e.threat()).isEqualTo(1));
    }

    @Test
    void reengageRestoresRecord() {
        MobInstance mob = CombatFixture.wolf(T);
        mob.engage(A, T);
        mob.receive(A, 12, T);
        mob.disengage(A, T);

        mob.engage(A, T.plusSeconds(5));

        assertThat(mob.engagementOf(A)).hasValueSatisfying(e -> {
            assertThat(e.threat()).isEqualTo(13);
            assertThat(e.dealt()).isEqualTo(12);
        });
    }

    @Test
    void respawnWithNewProfile() {
        MobInstance mob = CombatFixture.wolf(T);
        mob.engage(A, T);
        mob.receive(A, 200, T);

        mob.respawn(CombatFixture.rabbitProfile(), T.plusSeconds(90));

        assertThat(mob.isAlive()).isTrue();
        assertThat(mob.getProfile().name()).isEqualTo("들토끼");
        assertThat(mob.getQiBlood()).isEqualTo(42);
        assertThat(mob.getRespawnAt()).isNull();
        assertThat(mob.killer()).isEmpty();
        assertThat(mob.isFighting()).isFalse();
    }

    @Test
    void respawnAliveFail() {
        MobInstance mob = CombatFixture.wolf(T);

        assertThatThrownBy(() -> mob.respawn(CombatFixture.rabbitProfile(), T))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void actedAtSchedulesNext() {
        MobInstance mob = CombatFixture.wolf(T);
        mob.engage(A, T);

        mob.actedAt(T.plusMillis(1500), T.plusSeconds(2));

        assertThat(mob.getNextActionAt()).isEqualTo(T.plusMillis(4500));
    }
}
