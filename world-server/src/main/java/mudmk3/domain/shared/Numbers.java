package mudmk3.domain.shared;

import java.math.BigDecimal;
import java.util.Locale;

/** 화면에 보이는 수를 쓴다 */
public final class Numbers {

    private Numbers() {
    }

    /** 1,000 이상이면 쉼표를 찍는다: {@code 9,880} */
    public static String count(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    /** 소수 끝의 0을 지우고 지수 표기 없이 쓴다: {@code 0.75}, {@code 0.8}, {@code 10} */
    public static String decimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
