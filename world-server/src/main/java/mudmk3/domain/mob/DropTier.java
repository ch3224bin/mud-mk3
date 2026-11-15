package mudmk3.domain.mob;

/** 드롭 계층과 룰북 기본 확률(보상 배율을 곱하기 전). 선언 순서가 굴림·수습 표시 순서다 */
public enum DropTier {
    ADVANCED("고급", 0.08),
    RARE("희귀", 0.005),
    TREASURE("진귀", 0.0004);

    private final String koreanName;
    private final double baseChance;

    DropTier(String koreanName, double baseChance) {
        this.koreanName = koreanName;
        this.baseChance = baseChance;
    }

    public String koreanName() {
        return koreanName;
    }

    /** 일반 몹 한 번 쓰러질 때의 확률 */
    public double baseChance() {
        return baseChance;
    }
}
