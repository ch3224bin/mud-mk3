package mudmk3.domain.technique;

/** 신통을 쓸 수 있는 때 */
public enum UsableWhen {
    COMBAT_ONLY("전투 중만"),
    ANYTIME("전투 밖에도");

    private final String koreanName;

    UsableWhen(String koreanName) {
        this.koreanName = koreanName;
    }

    public String koreanName() {
        return koreanName;
    }
}
