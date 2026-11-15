package mudmk3.domain.combat;

import mudmk3.MutableClock;
import mudmk3.domain.mob.DropLine;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorpseTest {

    private static final Instant T = MutableClock.START;
    private static final Long KILLER = 1L;
    private static final Long OTHER = 2L;

    private static Corpse corpse() {
        return new Corpse(5, 10L, "청모랑", KILLER, T, List.of(
                new DropLine(11L, "청모랑 가죽", 1),
                new DropLine(12L, "늑대 이빨", 2)));
    }

    @Test
    void name() {
        assertThat(corpse().name()).isEqualTo("청모랑의 시체");
    }

    @Test
    void killerOnlyFirstSixtySeconds() {
        Corpse corpse = corpse();

        assertThat(corpse.collectibleBy(KILLER, T)).isTrue();
        assertThat(corpse.collectibleBy(OTHER, T.plusSeconds(59))).isFalse();
        assertThat(corpse.collectibleBy(OTHER, T.plusSeconds(60))).isTrue();
    }

    @Test
    void lockedForRemaining() {
        Corpse corpse = corpse();

        assertThat(corpse.lockedFor(T.plusSeconds(26))).isEqualTo(Duration.ofSeconds(34));
        assertThat(corpse.lockedFor(T.plusSeconds(70))).isEqualTo(Duration.ZERO);
    }

    @Test
    void expiresAfterThreeMinutes() {
        Corpse corpse = corpse();

        assertThat(corpse.expired(T.plusSeconds(179))).isFalse();
        assertThat(corpse.expired(T.plusSeconds(180))).isTrue();
    }

    @Test
    void takeFrom() {
        Corpse corpse = corpse();

        corpse.takeFrom(1, 1);

        assertThat(corpse.getRemaining()).containsExactly(
                new DropLine(11L, "청모랑 가죽", 1), new DropLine(12L, "늑대 이빨", 1));

        corpse.takeFrom(0, 1);

        assertThat(corpse.getRemaining()).containsExactly(new DropLine(12L, "늑대 이빨", 1));
        assertThat(corpse.isEmpty()).isFalse();

        corpse.takeFrom(0, 1);

        assertThat(corpse.isEmpty()).isTrue();
    }

    @Test
    void takeFromTooManyFail() {
        Corpse corpse = corpse();

        assertThatThrownBy(() -> corpse.takeFrom(0, 2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> corpse.takeFrom(0, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emptyCorpse() {
        Corpse corpse = new Corpse(6, 10L, "청모랑", null, T, List.of());

        assertThat(corpse.isEmpty()).isTrue();
        assertThat(corpse.collectibleBy(OTHER, T)).isFalse();
    }
}
