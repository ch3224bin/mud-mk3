package mudmk3.domain.mob;

/** 몹 종류. 육신 종류 보정과 괴뢰의 기혈 보정은 룰북 몹 스탯 종류 보정 표 그대로다 */
public enum MobKind {
    BEAST("요수", 1.2),
    CULTIVATOR("적대 수선자", 1.0),
    GHOST("귀물·음혼", 0.5),
    PUPPET("괴뢰", 1.5);

    private final String koreanName;
    private final double bodyFactor;

    MobKind(String koreanName, double bodyFactor) {
        this.koreanName = koreanName;
        this.bodyFactor = bodyFactor;
    }

    public String koreanName() {
        return koreanName;
    }

    /** 육신 종류 보정 */
    public double bodyFactor() {
        return bodyFactor;
    }

    /** 기혈 종류 보정: 괴뢰 0.8, 나머지 1.0 */
    public double qiBloodFactor() {
        return this == PUPPET ? 0.8 : 1.0;
    }
}
