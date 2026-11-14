package mudmk3.domain.item;

/** 저물대·저물탁 등급과 칸 수(룰북 item.md 저물대 · 저물탁). 저물탁은 축기 초기부터 칸이 열린다 */
public enum BagGrade {
    LOW("하급", 20, false),
    MIDDLE("중급", 40, false),
    HIGH("상급", 80, false),
    BRACELET_LOW("하급", 100, true),
    BRACELET_MIDDLE("중급", 200, true),
    BRACELET_HIGH("상급", 400, true);

    private final String koreanName;
    private final int slots;
    private final boolean bracelet;

    BagGrade(String koreanName, int slots, boolean bracelet) {
        this.koreanName = koreanName;
        this.slots = slots;
        this.bracelet = bracelet;
    }

    /** 등급 이름만: {@code 하급} */
    public String koreanName() {
        return koreanName;
    }

    /** 저물대·저물탁을 붙인 이름: {@code 하급 저물대}, {@code 하급 저물탁} */
    public String displayName() {
        return koreanName + (bracelet ? " 저물탁" : " 저물대");
    }

    public int slots() {
        return slots;
    }

    public boolean isBracelet() {
        return bracelet;
    }
}
