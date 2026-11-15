package mudmk3.domain.combat;

/** 몹이 한 번 맞은 결과: 육신 층을 거친 피해와 쓰러졌는지 */
public record MobHit(double damage, boolean died) {
}
