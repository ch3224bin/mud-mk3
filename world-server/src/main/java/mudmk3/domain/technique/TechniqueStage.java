package mudmk3.domain.technique;

/**
 * 공법 단계 하나의 값: 필요 깨달음(그 단계에 오르는 데 쓰는 합, 1성은 0), 영근 자질 조건, 스탯 증가.
 * 범위·계열 규칙은 단계 번호를 문구에 넣어야 하므로 {@link Technique} 가 검사한다
 */
public record TechniqueStage(
        int insightRequired,
        int aptitudeRequired,
        int manaBonus,
        int senseBonus,
        int spiritBonus,
        int essenceBonus,
        int bodyBonus,
        int strengthBonus,
        int aptitudeGain
) {
}
