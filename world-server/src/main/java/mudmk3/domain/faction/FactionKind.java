package mudmk3.domain.faction;

/** 성·종문의 종류. 선언 순서가 목록 순서다 */
public enum FactionKind {
    CASTLE("성"),
    SECT("종문");

    private final String koreanName;

    FactionKind(String koreanName) {
        this.koreanName = koreanName;
    }

    public String koreanName() {
        return koreanName;
    }
}
