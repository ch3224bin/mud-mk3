package mudmk3.application.mob.provided;

/** 지운 몹 유형과 함께 지운 배치 수(삭제 기록용) */
public record DeletedMobType(Long id, String name, int placementCount) {
}
