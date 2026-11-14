package mudmk3.domain.technique;

/** 신통 대상 */
public enum AbilityTarget {
    SELF("자신"),
    SINGLE("한 대상"),
    ALL_ENEMIES("방의 모든 적");

    private final String koreanName;

    AbilityTarget(String koreanName) {
        this.koreanName = koreanName;
    }

    public String koreanName() {
        return koreanName;
    }
}
