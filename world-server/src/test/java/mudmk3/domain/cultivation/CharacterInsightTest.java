package mudmk3.domain.cultivation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CharacterInsightTest {

    @Test
    void addZeroOk() {
        CharacterInsight insight = CharacterInsight.create(1L, InsightKind.FIRE, 2.5);

        insight.add(0);
        insight.add(1.25);

        assertThat(insight.getAmount()).isEqualTo(3.75);
    }

    @Test
    void addRejectsNegative() {
        CharacterInsight insight = CharacterInsight.create(1L, InsightKind.FIRE, 2.5);

        assertThatThrownBy(() -> insight.add(-0.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("깨달음은 0 이상 유한한 값만 더합니다: -0.1");
        assertThat(insight.getAmount()).isEqualTo(2.5);
    }

    @Test
    void addRejectsNaN() {
        CharacterInsight insight = CharacterInsight.create(1L, InsightKind.FIRE, 2.5);

        assertThatThrownBy(() -> insight.add(Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(insight.getAmount()).isEqualTo(2.5);
    }

    @Test
    void addRejectsInfinity() {
        CharacterInsight insight = CharacterInsight.create(1L, InsightKind.FIRE, 2.5);

        assertThatThrownBy(() -> insight.add(Double.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(insight.getAmount()).isEqualTo(2.5);
    }
}
