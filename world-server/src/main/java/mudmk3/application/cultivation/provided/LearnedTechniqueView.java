package mudmk3.application.cultivation.provided;

/** 익힌 공법 보기 값: 공법 id·이름, 지금 단계, 공법의 단계 수 */
public record LearnedTechniqueView(Long techniqueId, String name, int stage, int stageCount) {
}
