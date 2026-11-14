package mudmk3.domain.item;

/** 저물대 등급과 칸 수 */
public enum BagGrade {
    LOW("하급", 20),
    MIDDLE("중급", 40),
    HIGH("상급", 80);

    private final String koreanName;
    private final int slots;

    BagGrade(String koreanName, int slots) {
        this.koreanName = koreanName;
        this.slots = slots;
    }

    public String koreanName() {
        return koreanName;
    }

    public int slots() {
        return slots;
    }
}
