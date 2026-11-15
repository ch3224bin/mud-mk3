package mudmk3.domain.combat;

import mudmk3.domain.cultivation.StatSheet;
import mudmk3.domain.shared.Element;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class AbilityPowerTest {

    private static final BigDecimal BASE = new BigDecimal("2");
    private static final BigDecimal COEFFICIENT = new BigDecimal("1.2");

    @Test
    void elemental() {
        assertThat(AbilityPower.elemental(BASE, COEFFICIENT, 5, 1.0, 1.5)).isCloseTo(12, within(1e-9)); // 룰북 예
        assertThat(AbilityPower.elemental(BASE, COEFFICIENT, 34, 1.0, 1.5)).isCloseTo(64.2, within(1e-9));
        assertThat(AbilityPower.elemental(BASE, COEFFICIENT, 5, 0.9, 1.5)).isCloseTo(11.1, within(1e-9));
    }

    @Test
    void bodyRefining() {
        assertThat(AbilityPower.bodyRefining(new BigDecimal("5"), new BigDecimal("2.2"), 3)).isCloseTo(11.6, within(1e-9));
    }

    @Test
    void aptitudeMultiplier() {
        StatSheet fire50 = stats(Map.of(Element.FIRE, 50.0));

        assertThat(AbilityPower.aptitudeMultiplier(List.of(Element.FIRE), fire50)).isCloseTo(1.5, within(1e-9));
        assertThat(AbilityPower.aptitudeMultiplier(List.of(Element.FIRE, Element.EARTH), fire50)).isCloseTo(1.25, within(1e-9));
        assertThat(AbilityPower.aptitudeMultiplier(List.of(), fire50)).isEqualTo(1.0);
        assertThat(AbilityPower.aptitudeMultiplier(List.of(Element.FIRE), stats(Map.of(Element.WATER, 60.0)))).isEqualTo(1.0);
    }

    private static StatSheet stats(Map<Element, Double> aptitudes) {
        return new StatSheet(60, 60, 25, 25, 5, 5, 5, 6, 3, aptitudes);
    }
}
