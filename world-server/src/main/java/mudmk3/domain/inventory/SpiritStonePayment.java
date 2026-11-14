package mudmk3.domain.inventory;

import mudmk3.domain.item.SpiritStoneGrade;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * 영석 결제: 등급마다 내는 수(하품 → 극품 순)와 하품으로 거슬러 받는 수(룰북 item.md 결제)
 *
 * @param paid   등급마다 내는 수. 내는 등급만 들어 있다
 * @param change 거슬러 받는 하품 영석 수
 */
public record SpiritStonePayment(Map<SpiritStoneGrade, Long> paid, long change) {

    private static final List<SpiritStoneGrade> HIGH_GRADES = List.of(SpiritStoneGrade.HIGH, SpiritStoneGrade.TOP);

    public SpiritStonePayment {
        Map<SpiritStoneGrade, Long> copy = new EnumMap<>(SpiritStoneGrade.class);
        requireNonNull(paid).forEach((grade, count) -> {
            if (requireNonNull(count) < 1) {
                throw new IllegalArgumentException("내는 영석 수는 1 이상이어야 합니다: " + grade);
            }
            copy.put(requireNonNull(grade), count);
        });
        if (change < 0) {
            throw new IllegalArgumentException("거스름은 0 이상이어야 합니다: " + change);
        }
        paid = Collections.unmodifiableMap(copy);
    }

    /**
     * 하품 → 극품 순으로, 그 등급으로 남은 값을 다 낼 수 있으면 필요한 만큼만(올림) 내고 끝낸다. 다 내도 모자라면 그 등급을
     * 모두 내고 다음 등급으로. 낸 값이 가격보다 많으면 남은 값을 하품으로 거슬러 받는다. 가진 영석의 하품 기준 합이 가격보다
     * 적으면 결제하지 않는다(empty)
     */
    public static Optional<SpiritStonePayment> plan(Map<SpiritStoneGrade, Long> held, long price) {
        requireNonNull(held);
        if (price < 1) {
            throw new IllegalArgumentException("값은 1 이상이어야 합니다: " + price);
        }
        if (valueOf(held) < price) return Optional.empty();

        Map<SpiritStoneGrade, Long> paid = new EnumMap<>(SpiritStoneGrade.class);
        long remaining = price;
        for (SpiritStoneGrade grade : SpiritStoneGrade.values()) {
            long count = held.getOrDefault(grade, 0L);
            if (count <= 0) continue;
            long value = Math.multiplyExact(count, grade.value());
            if (value >= remaining) {
                long needed = Math.ceilDiv(remaining, grade.value());
                paid.put(grade, needed);
                remaining -= needed * grade.value();
                break;
            }
            paid.put(grade, count);
            remaining -= value;
        }
        return Optional.of(new SpiritStonePayment(paid, -remaining));
    }

    /** 낸 영석의 하품 기준 합(= 값 + 거스름) */
    public long paidValue() {
        return valueOf(paid);
    }

    /**
     * 상품·극품을 깨는지(룰북 item.md: 상품·극품을 깨야 하면 확인). 깬다는 것은 그 돌에서 거스름이 나오는 것이다.
     * 상품·극품을 값에 그대로 다 쓰면(거스름 0) 깨지 않는다. 중품은 묻지 않는다
     */
    public boolean breaksHighStones() {
        return !brokenHighGrades().isEmpty();
    }

    /**
     * 깨는 상품·극품 등급. 거스름은 마지막으로 낸 등급에서만 나오므로 많아야 하나다
     * (예: 상품 1·극품 1로 20,000 → 상품은 다 쓰이고 극품만 깨져 {@code [TOP]})
     */
    public List<SpiritStoneGrade> brokenHighGrades() {
        if (change == 0) return List.of();
        SpiritStoneGrade last = null;
        for (SpiritStoneGrade grade : paid.keySet()) {
            last = grade;
        }
        return last != null && HIGH_GRADES.contains(last) ? List.of(last) : List.of();
    }

    private static long valueOf(Map<SpiritStoneGrade, Long> stones) {
        long sum = 0;
        for (Map.Entry<SpiritStoneGrade, Long> entry : stones.entrySet()) {
            sum = Math.addExact(sum, Math.multiplyExact(Math.max(0, entry.getValue()), entry.getKey().value()));
        }
        return sum;
    }
}
