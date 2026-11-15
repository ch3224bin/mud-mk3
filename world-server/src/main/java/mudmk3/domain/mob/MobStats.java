package mudmk3.domain.mob;

import mudmk3.domain.shared.Realm;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static java.util.Objects.requireNonNull;

/**
 * 몹 스탯(입력하지 않고 계산): 기혈 최대치·육신·공격 위력. 식은 룰북 몹 스탯(admin-tool.md 몹 MobType 스탯 줄) 그대로이고
 * 기준값은 기준 경지의 경지 몫이다
 */
public record MobStats(int qiBlood, int body, int power) {

    private static final BigDecimal TEN = BigDecimal.TEN;
    private static final BigDecimal P_BASE = new BigDecimal("3");
    private static final BigDecimal P_PER_SPIRIT = new BigDecimal("1.8");

    /**
     * 기혈 = 경지 육신 × 10 × 체급 기혈 계수 × 종류 기혈 보정, 육신 = 경지 육신 × 체급 육신 계수 × 종류 보정,
     * 위력 = (3 + 1.8 × 경지 영력) × 공격 계수(두목은 {@code bossAttackCoefficient}, 다른 체급은 체급 값이고 인자는 보지 않는다).
     * 계수는 소수라 정확한 십진 값으로 곱한 뒤 마지막에 반올림한다(룰북, {@code Math.round} 와 같은 결과)
     */
    public static MobStats of(MobKind kind, Realm realm, MobTier tier, @Nullable BigDecimal bossAttackCoefficient) {
        requireNonNull(kind);
        requireNonNull(realm);
        requireNonNull(tier);
        BigDecimal attackFactor;
        if (tier.attackFactor().isPresent()) {
            attackFactor = decimal(tier.attackFactor().getAsDouble());
        } else if (bossAttackCoefficient != null) {
            attackFactor = bossAttackCoefficient;
        } else {
            throw new IllegalArgumentException("두목의 공격 계수가 없습니다");
        }
        BigDecimal body = BigDecimal.valueOf(realm.body());
        BigDecimal qiBlood = body.multiply(TEN).multiply(decimal(tier.qiBloodFactor())).multiply(decimal(kind.qiBloodFactor()));
        BigDecimal mobBody = body.multiply(decimal(tier.bodyFactor())).multiply(decimal(kind.bodyFactor()));
        BigDecimal power = P_BASE.add(P_PER_SPIRIT.multiply(BigDecimal.valueOf(realm.spirit()))).multiply(attackFactor);
        return new MobStats(round(qiBlood), round(mobBody), round(power));
    }

    /** 계수 상수는 십진으로 적은 값({@code 1.2}) 그대로 쓴다 */
    private static BigDecimal decimal(double factor) {
        return BigDecimal.valueOf(factor);
    }

    private static int round(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP).intValueExact();
    }
}
