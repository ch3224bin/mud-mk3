package mudmk3.domain.shared;

import java.util.OptionalInt;

/**
 * 경지. 선언 순서가 경지 순서다. 경지 몫 스탯과 필요 수위는 platform.md 경지 몫 스탯 표를 그대로 옮긴 상수다
 * (식으로 계산하지 않는다). 기혈 = 육신 × 10, 법력 = 영력 × 5 는 스탯 계산이 구한다
 */
public enum Realm {
    MORTAL("범인", 1, 1, 0, 10, 0),
    QI_1("연기 1층", 6, 3, 5, 20, 190),
    QI_2("연기 2층", 7, 4, 6, 22, 290),
    QI_3("연기 3층", 8, 4, 7, 24, 420),
    QI_4("연기 4층", 9, 5, 8, 26, 610),
    QI_5("연기 5층", 10, 5, 9, 28, 880),
    QI_6("연기 6층", 11, 6, 10, 30, 1_200),
    QI_7("연기 7층", 12, 6, 11, 32, 1_700),
    QI_8("연기 8층", 13, 7, 12, 34, 2_400),
    QI_9("연기 9층", 14, 7, 13, 36, 3_300),
    QI_10("연기 10층", 15, 8, 14, 38, 4_500),
    QI_11("연기 11층", 16, 8, 15, 40, 6_100),
    QI_12("연기 12층", 17, 9, 16, 42, 8_300),
    QI_13("연기 13층", 18, 9, 17, 44, 11_000),
    FOUNDATION_EARLY("축기 초기", 36, 18, 34, 88, 47_000),
    FOUNDATION_MIDDLE("축기 중기", 42, 21, 40, 103, 67_000),
    FOUNDATION_LATE("축기 후기", 48, 24, 45, 117, 90_000),
    FOUNDATION_PEAK("축기 대원만", 54, 27, 51, 132, 120_000);

    private final String koreanName;
    private final int body;
    private final int strength;
    private final int spirit;
    private final int senseEssence;
    /** 0 이면 필요 수위 없음(범인) */
    private final int requiredCultivation;

    Realm(String koreanName, int body, int strength, int spirit, int senseEssence, int requiredCultivation) {
        this.koreanName = koreanName;
        this.body = body;
        this.strength = strength;
        this.spirit = spirit;
        this.senseEssence = senseEssence;
        this.requiredCultivation = requiredCultivation;
    }

    /** {@code 범인}, {@code 연기 1층}, {@code 축기 대원만} */
    public String koreanName() {
        return koreanName;
    }

    /** 경지 몫 육신 */
    public int body() {
        return body;
    }

    /** 경지 몫 근력 */
    public int strength() {
        return strength;
    }

    /** 경지 몫 영력 */
    public int spirit() {
        return spirit;
    }

    /** 경지 몫 의식·정혈(같은 값) */
    public int senseEssence() {
        return senseEssence;
    }

    /** 이 경지의 필요 수위. 범인은 비어 있다 */
    public OptionalInt requiredCultivation() {
        return requiredCultivation == 0 ? OptionalInt.empty() : OptionalInt.of(requiredCultivation);
    }

    /** 이 경지가 다른 경지보다 낮은가 */
    public boolean isBelow(Realm other) {
        return ordinal() < other.ordinal();
    }

    /** 저물탁 칸이 열리는 경지인가(축기 초기 이상) */
    public boolean opensBracelet() {
        return !isBelow(FOUNDATION_EARLY);
    }
}
