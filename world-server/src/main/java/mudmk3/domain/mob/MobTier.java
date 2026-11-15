package mudmk3.domain.mob;

import org.jspecify.annotations.Nullable;

import java.util.OptionalDouble;
import java.util.OptionalInt;

/** 몹 체급. 계수·보상 배율·재등장 기준은 룰북 몹 스탯 체급 표와 재등장 기준 그대로다 */
public enum MobTier {
    NORMAL("일반", 0.7, 0.6, 1.0, 1, 90, null),
    STRONG("강적", 1.0, 0.8, 1.0, 2, 180, "강적"),
    ELITE("정예", 2.0, 0.6, 1.0, 5, 600, "정예"),
    /** 공격 계수는 몹마다 정하고(0.4~0.8) 재등장도 꼭 정한다 */
    BOSS("두목", 10.0, 0, 1.2, 20, 0, "두목");

    private final String koreanName;
    private final double qiBloodFactor;
    /** 0 이면 몹마다 정한다(두목) */
    private final double attackFactor;
    private final double bodyFactor;
    private final int rewardMultiplier;
    /** 0 이면 기준이 없다(두목) */
    private final int defaultRespawnSeconds;
    private final @Nullable String badge;

    MobTier(String koreanName, double qiBloodFactor, double attackFactor, double bodyFactor, int rewardMultiplier,
            int defaultRespawnSeconds, @Nullable String badge) {
        this.koreanName = koreanName;
        this.qiBloodFactor = qiBloodFactor;
        this.attackFactor = attackFactor;
        this.bodyFactor = bodyFactor;
        this.rewardMultiplier = rewardMultiplier;
        this.defaultRespawnSeconds = defaultRespawnSeconds;
        this.badge = badge;
    }

    /** {@code 일반}, {@code 강적}, {@code 정예}, {@code 두목} */
    public String koreanName() {
        return koreanName;
    }

    /** 기혈 계수 */
    public double qiBloodFactor() {
        return qiBloodFactor;
    }

    /** 공격 계수. 두목은 몹마다 정하므로 비어 있다 */
    public OptionalDouble attackFactor() {
        return attackFactor == 0 ? OptionalDouble.empty() : OptionalDouble.of(attackFactor);
    }

    /** 육신 계수 */
    public double bodyFactor() {
        return bodyFactor;
    }

    /** 드롭 보상 배율 */
    public int rewardMultiplier() {
        return rewardMultiplier;
    }

    /** 체급 기준 재등장(초). 두목은 기준이 없어 비어 있다 */
    public OptionalInt defaultRespawnSeconds() {
        return defaultRespawnSeconds == 0 ? OptionalInt.empty() : OptionalInt.of(defaultRespawnSeconds);
    }

    /** 방 보기에 붙이는 표시. 일반은 없다 */
    public @Nullable String badge() {
        return badge;
    }
}
