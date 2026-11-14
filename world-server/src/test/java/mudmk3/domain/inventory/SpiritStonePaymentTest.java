package mudmk3.domain.inventory;

import mudmk3.domain.item.SpiritStoneGrade;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static mudmk3.domain.item.SpiritStoneGrade.HIGH;
import static mudmk3.domain.item.SpiritStoneGrade.LOW;
import static mudmk3.domain.item.SpiritStoneGrade.MIDDLE;
import static mudmk3.domain.item.SpiritStoneGrade.TOP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpiritStonePaymentTest {

    @Test
    void rulebookExample() {
        SpiritStonePayment payment = SpiritStonePayment.plan(Map.of(LOW, 50L, MIDDLE, 2L), 120).orElseThrow();

        assertThat(payment.paid()).isEqualTo(Map.of(LOW, 50L, MIDDLE, 1L));
        assertThat(payment.paid().keySet()).containsExactly(LOW, MIDDLE);
        assertThat(payment.change()).isEqualTo(30);
        assertThat(payment.paidValue()).isEqualTo(150);
        assertThat(payment.breaksHighStones()).isFalse();
        assertThat(payment.brokenHighGrades()).isEmpty();
    }

    @Test
    void exactLow() {
        SpiritStonePayment payment = SpiritStonePayment.plan(Map.of(LOW, 15L), 15).orElseThrow();

        assertThat(payment.paid()).isEqualTo(Map.of(LOW, 15L));
        assertThat(payment.change()).isZero();
    }

    @Test
    void lowEnoughStopsThere() {
        SpiritStonePayment payment = SpiritStonePayment.plan(Map.of(LOW, 50L, MIDDLE, 2L), 15).orElseThrow();

        assertThat(payment.paid()).isEqualTo(Map.of(LOW, 15L));
        assertThat(payment.change()).isZero();
    }

    @Test
    void middleOnly() {
        SpiritStonePayment payment = SpiritStonePayment.plan(Map.of(MIDDLE, 1L), 3).orElseThrow();

        assertThat(payment.paid()).isEqualTo(Map.of(MIDDLE, 1L));
        assertThat(payment.change()).isEqualTo(97);
    }

    @Test
    void notEnough() {
        assertThat(SpiritStonePayment.plan(Map.of(LOW, 9L), 15)).isEmpty();
        assertThat(SpiritStonePayment.plan(Map.of(), 1)).isEmpty();
    }

    @Test
    void breakHigh() {
        SpiritStonePayment payment = SpiritStonePayment.plan(Map.of(HIGH, 1L), 120).orElseThrow();

        assertThat(payment.paid()).isEqualTo(Map.of(HIGH, 1L));
        assertThat(payment.change()).isEqualTo(9_880);
        assertThat(payment.breaksHighStones()).isTrue();
        assertThat(payment.brokenHighGrades()).containsExactly(HIGH);
    }

    @Test
    void breakHighAndTop() {
        SpiritStonePayment payment = SpiritStonePayment.plan(Map.of(LOW, 5L, HIGH, 1L, TOP, 1L), 20_000).orElseThrow();

        assertThat(payment.paid()).isEqualTo(Map.of(LOW, 5L, HIGH, 1L, TOP, 1L));
        assertThat(payment.change()).isEqualTo(1_010_005 - 20_000);
        assertThat(payment.brokenHighGrades()).containsExactly(HIGH, TOP);
    }

    @Test
    void priceBelowOneFail() {
        assertThatThrownBy(() -> SpiritStonePayment.plan(Map.of(LOW, 1L), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void paidIsUnmodifiable() {
        SpiritStonePayment payment = SpiritStonePayment.plan(Map.of(LOW, 1L), 1).orElseThrow();

        assertThatThrownBy(() -> payment.paid().put(SpiritStoneGrade.MIDDLE, 1L))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
