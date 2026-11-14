package mudmk3.domain.technique;

/** 신통 종류. 선언 순서가 관리 툴 선택지 순서다 */
public enum AbilityKind {
    ATTACK("공격"),
    AREA_ATTACK("범위 공격"),
    DEFENSE("방어"),
    EVASION("회피"),
    HEALING("치료"),
    ESCAPE("둔술"),
    DETECTION("탐지"),
    STEALTH("은신"),
    CONCEALMENT("렴기"),
    BUFF("보조");

    private final String koreanName;

    AbilityKind(String koreanName) {
        this.koreanName = koreanName;
    }

    public String koreanName() {
        return koreanName;
    }
}
