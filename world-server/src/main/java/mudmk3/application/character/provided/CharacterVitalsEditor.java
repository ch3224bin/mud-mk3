package mudmk3.application.character.provided;

/**
 * 캐릭터의 현재 기혈·법력을 깎고 채운다.
 * 없는 캐릭터면 {@code IllegalArgumentException("캐릭터를 찾을 수 없습니다. id: " + id)}
 */
public interface CharacterVitalsEditor {

    /** 깎인 기혈(소수 그대로). 비어 있는 기존 행은 0 */
    double qiBloodLost(Long characterId);

    /** 피해만큼 깎고(최대를 넘게 깎이지 않음) 남은 현재 기혈을 돌려준다 */
    double takeDamage(Long characterId, double damage, int maxQiBlood);

    /** 현재 기혈을 그 값(0~최대)으로 맞춘다 */
    void setQiBlood(Long characterId, double current, int maxQiBlood);

    /** 깎인 기혈이 있었으면 0으로 쓰고 true, 이미 가득이면 false */
    boolean restoreQiBlood(Long characterId);

    /** 깎인 법력(소수 그대로). 비어 있는 기존 행은 0 */
    double manaLost(Long characterId);

    /** 쓴 만큼 깎고(최대를 넘게 깎이지 않음) 남은 현재 법력을 돌려준다 */
    double spendMana(Long characterId, double amount, int maxMana);

    /** 깎인 법력이 있었으면 그만큼 채워 쓰고(0 아래로 내려가지 않음) true, 이미 가득이면 쓰지 않고 false */
    boolean recoverMana(Long characterId, double amount);
}
