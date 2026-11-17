package mudmk3.domain.combat;

import mudmk3.MutableClock;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbilityTimersTest {

    private static final Instant T = MutableClock.START;
    private static final AbilityKey FIREBALL = new AbilityKey(1L, "화탄술");
    private static final AbilityKey BOLT = new AbilityKey(1L, "낙뢰술");

    @Test
    void globalRemaining() {
        AbilityTimers timers = new AbilityTimers(7L);
        assertThat(timers.globalRemaining(T)).isEmpty();

        timers.startGlobal(T);

        assertThat(timers.getGlobalReadyAt()).isEqualTo(T.plus(AbilityRules.GLOBAL_COOLDOWN));
        assertThat(timers.globalRemaining(T.plusMillis(1200))).contains(Duration.ofMillis(300));
        assertThat(timers.globalRemaining(T.plusMillis(1500))).isEmpty();
    }

    @Test
    void cooldownRemaining() {
        AbilityTimers timers = new AbilityTimers(7L);

        timers.startCooldown(FIREBALL, new BigDecimal("3"), T);
        timers.startCooldown(BOLT, new BigDecimal("4.5"), T.plusSeconds(1));

        assertThat(timers.cooldownRemaining(FIREBALL, T.plusMillis(1200))).contains(Duration.ofMillis(1800));
        assertThat(timers.cooldownRemaining(BOLT, T.plusMillis(1200))).contains(Duration.ofMillis(4300));
        assertThat(timers.cooldownRemaining(FIREBALL, T.plusSeconds(3))).isEmpty();
        assertThat(timers.cooldownRemaining(new AbilityKey(2L, "화탄술"), T)).isEmpty();
        assertThat(timers.getCooldowns()).containsKeys(FIREBALL, BOLT);
    }

    @Test
    void startCooldownNullFails() {
        AbilityTimers timers = new AbilityTimers(7L);

        assertThatThrownBy(() -> timers.startCooldown(FIREBALL, null, T)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void castingClear() {
        AbilityTimers timers = new AbilityTimers(7L);
        Casting casting = new Casting(1L, "화탄술", 3, T.plusSeconds(2), 1);

        timers.startCasting(casting);
        assertThat(timers.casting()).contains(casting);
        assertThat(casting.key()).isEqualTo(FIREBALL);

        timers.clearCasting();
        assertThat(timers.casting()).isEmpty();
        assertThat(timers.getCharacterId()).isEqualTo(7L);
    }

    @Test
    void castingStart() {
        Casting casting = Casting.start(BOLT, 3, new BigDecimal("2.5"), T, 4);

        assertThat(casting).isEqualTo(new Casting(1L, "낙뢰술", 3, T.plusMillis(2500), 4));
    }

    @Test
    void isIdle() {
        AbilityTimers timers = new AbilityTimers(7L);
        assertThat(timers.isIdle(T)).isTrue();

        timers.startGlobal(T);
        timers.startCooldown(FIREBALL, new BigDecimal("3"), T);
        assertThat(timers.isIdle(T.plusMillis(1499))).isFalse();
        assertThat(timers.isIdle(T.plusMillis(1500))).isFalse();
        assertThat(timers.isIdle(T.plusSeconds(3))).isTrue();

        timers.startCasting(new Casting(1L, "화탄술", 3, T.plusSeconds(5), 1));
        assertThat(timers.isIdle(T.plusSeconds(60))).isFalse();
        timers.clearCasting();
        assertThat(timers.isIdle(T.plusSeconds(60))).isTrue();
    }
}
