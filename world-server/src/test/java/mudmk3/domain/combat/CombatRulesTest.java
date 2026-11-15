package mudmk3.domain.combat;

import mudmk3.MutableClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CombatRulesTest {

    private static final Instant T = MutableClock.START;

    @Test
    void nextIsThreeSecondsAfterDue() {
        assertThat(CombatRules.next(T, T.plusMillis(200))).isEqualTo(T.plusSeconds(3));
    }

    @Test
    void nextSkipsMissedActions() {
        assertThat(CombatRules.next(T, T.plusSeconds(7))).isEqualTo(T.plusSeconds(9));
    }

    @Test
    void nextIsAfterNowOnBoundary() {
        assertThat(CombatRules.next(T, T.plusSeconds(3))).isEqualTo(T.plusSeconds(6));
    }

    @Test
    void constants() {
        assertThat(CombatRules.ATTACK_INTERVAL).isEqualTo(Duration.ofSeconds(3));
        assertThat(CombatRules.MOB_FIRST_ACTION).isEqualTo(Duration.ofMillis(1500));
        assertThat(CombatRules.REFILL_DELAY).isEqualTo(Duration.ofSeconds(10));
        assertThat(CombatRules.CORPSE_EXCLUSIVE).isEqualTo(Duration.ofSeconds(60));
        assertThat(CombatRules.CORPSE_LIFETIME).isEqualTo(Duration.ofMinutes(3));
    }
}
