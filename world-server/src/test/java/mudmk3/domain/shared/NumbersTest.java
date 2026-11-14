package mudmk3.domain.shared;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class NumbersTest {

    @Test
    void count() {
        assertThat(Numbers.count(9880)).isEqualTo("9,880");
        assertThat(Numbers.count(999)).isEqualTo("999");
        assertThat(Numbers.count(1_000_000)).isEqualTo("1,000,000");
        assertThat(Numbers.count(0)).isEqualTo("0");
    }

    @Test
    void decimal() {
        assertThat(Numbers.decimal(new BigDecimal("0.75"))).isEqualTo("0.75");
        assertThat(Numbers.decimal(new BigDecimal("10.00"))).isEqualTo("10");
        assertThat(Numbers.decimal(new BigDecimal("0.80"))).isEqualTo("0.8");
        assertThat(Numbers.decimal(new BigDecimal("0.00"))).isEqualTo("0");
        assertThat(Numbers.decimal(new BigDecimal("1000000.00"))).isEqualTo("1000000");
    }
}
