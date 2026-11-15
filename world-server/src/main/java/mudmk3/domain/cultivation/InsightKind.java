package mudmk3.domain.cultivation;

import mudmk3.domain.shared.Element;

/** 깨달음 갈래: 오행 다섯과 연체. 선언 순서가 보이는 순서다 */
public enum InsightKind {
    METAL("금"),
    WOOD("목"),
    WATER("수"),
    FIRE("화"),
    EARTH("토"),
    BODY("연체");

    private final String koreanName;

    InsightKind(String koreanName) {
        this.koreanName = koreanName;
    }

    public String koreanName() {
        return koreanName;
    }

    /** 그 오행의 깨달음 갈래 */
    public static InsightKind of(Element element) {
        return switch (element) {
            case METAL -> METAL;
            case WOOD -> WOOD;
            case WATER -> WATER;
            case FIRE -> FIRE;
            case EARTH -> EARTH;
        };
    }
}
