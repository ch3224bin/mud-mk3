package mudmk3.domain.item;

import java.math.BigDecimal;

/** 상점 값 계산(룰북 item.md 상점에서 사고팔기). 3차 물건은 모두 하계라 판매가 배율 1, 매입 배율 1.0 이다 */
public final class Pricing {

    private Pricing() {
    }

    /** 상점이 값을 정하지 않았을 때의 판매가 = 기준가 */
    public static long defaultSellPrice(ItemType itemType) {
        return itemType.getBaseValue();
    }

    /** 개당 매입가 = 기준가 × 매입 비율 / 100 (사지 않는 종류는 0) */
    public static BigDecimal unitBuyPrice(ItemType itemType) {
        return BigDecimal.valueOf(itemType.getBaseValue() * rate(itemType), 2);
    }

    /** 여러 개의 매입가 합계(하품 영석, 내림) */
    public static long buyTotal(ItemType itemType, long count) {
        return Math.multiplyExact(Math.multiplyExact(itemType.getBaseValue(), rate(itemType)), count) / 100;
    }

    /** 불변식: 매입가 &lt; 판매가 × 0.5, 정수로 {@code 2 × 기준가 × 비율 < 100 × 판매가}. 사지 않는 종류는 늘 맞다 */
    public static boolean isFairSellPrice(ItemType itemType, long price) {
        long rate = rate(itemType);
        if (rate == 0) return true;
        return 2 * itemType.getBaseValue() * rate < 100 * price;
    }

    private static long rate(ItemType itemType) {
        return itemType.getCategory().buyRatePercent();
    }
}
