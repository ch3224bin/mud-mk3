package mudmk3.domain.combat;

import mudmk3.domain.cultivation.StatSheet;
import mudmk3.domain.shared.Element;

import java.math.BigDecimal;
import java.util.List;

/** 공격 신통의 위력(룰북 공격력, command.md 신통 쓰기 성공 4) */
public final class AbilityPower {

    private static final double PERCENT = 100;

    private AbilityPower() {
    }

    /** 오행 신통 위력 = (기본 위력 + 영력 × 법력 발휘 × 계수) × 영근 배율 */
    public static double elemental(BigDecimal base, BigDecimal coefficient, int spirit, double spiritRatio,
                                   double aptitudeMultiplier) {
        return (base.doubleValue() + spirit * spiritRatio * coefficient.doubleValue()) * aptitudeMultiplier;
    }

    /** 연체 신통 위력 = 기본 위력 + 근력 × 계수(영력 제한·영근 배율 없음) */
    public static double bodyRefining(BigDecimal base, BigDecimal coefficient, int strength) {
        return base.doubleValue() + strength * coefficient.doubleValue();
    }

    /**
     * 영근 배율 = 1 + (신통 속성마다 그 오행 자질의 평균) ÷ 100. 영근에 없는 오행은 자질 0, 속성이 없으면 1
     */
    public static double aptitudeMultiplier(List<Element> elements, StatSheet stats) {
        if (elements.isEmpty()) return 1.0;
        double sum = elements.stream().mapToDouble(stats::aptitude).sum();
        return 1 + sum / elements.size() / PERCENT;
    }
}
