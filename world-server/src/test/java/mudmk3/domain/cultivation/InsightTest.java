package mudmk3.domain.cultivation;

import mudmk3.domain.shared.Element;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InsightTest {

    @Test
    void floor() {
        Insight insight = new Insight(Map.of(InsightKind.FIRE, 230.9, InsightKind.WOOD, 112.2));

        assertThat(insight.floor(InsightKind.FIRE)).isEqualTo(230);
        assertThat(insight.floor(InsightKind.WOOD)).isEqualTo(112);
        assertThat(insight.amount(InsightKind.FIRE)).isEqualTo(230.9);
    }

    @Test
    void missingKindIsZero() {
        Insight insight = new Insight(Map.of(InsightKind.FIRE, 5.0));

        assertThat(insight.amount(InsightKind.METAL)).isZero();
        assertThat(insight.floor(InsightKind.BODY)).isZero();
        assertThat(insight.amounts()).containsOnlyKeys(InsightKind.values());
    }

    @Test
    void empty() {
        Insight empty = Insight.empty();

        for (InsightKind kind : InsightKind.values()) {
            assertThat(empty.floor(kind)).isZero();
        }
    }

    @Test
    void kindOfElement() {
        assertThat(InsightKind.of(Element.METAL)).isEqualTo(InsightKind.METAL);
        assertThat(InsightKind.of(Element.EARTH)).isEqualTo(InsightKind.EARTH);
        assertThat(InsightKind.BODY.koreanName()).isEqualTo("연체");
    }
}
