package mudmk3.domain.technique;

/** 신통이 소모하는 스탯 */
public enum CostStat {
    NONE("없음"),
    MANA("법력"),
    SENSE("의식"),
    ESSENCE("정혈");

    private final String koreanName;

    CostStat(String koreanName) {
        this.koreanName = koreanName;
    }

    public String koreanName() {
        return koreanName;
    }
}
