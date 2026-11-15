package mudmk3.domain.combat;

import java.time.Duration;
import java.time.Instant;

import static java.util.Objects.requireNonNull;

/** 5차 교전의 시간 규칙(command.md 공격·서버 틱, 룰북 몹 스탯·시체와 수습) */
public final class CombatRules {

    /** 플레이어·몹이 치는 간격 */
    public static final Duration ATTACK_INTERVAL = Duration.ofSeconds(3);
    /** 교전이 시작된 뒤 몹의 첫 행동까지 */
    public static final Duration MOB_FIRST_ACTION = Duration.ofMillis(1_500);
    /** 교전 상대가 모두 사라진 뒤 기혈이 다 찰 때까지 */
    public static final Duration REFILL_DELAY = Duration.ofSeconds(10);
    /** 처치자만 시체를 수습할 수 있는 시간 */
    public static final Duration CORPSE_EXCLUSIVE = Duration.ofSeconds(60);
    /** 시체가 남는 시간 */
    public static final Duration CORPSE_LIFETIME = Duration.ofMinutes(3);

    private CombatRules() {
    }

    /** 다음 행동 시각 = 그 행동의 시각 + 3초. 그것도 지금 이전(같음 포함)이면 지금 뒤가 될 때까지 3초씩 더한다(밀린 행동을 몰아치지 않음) */
    public static Instant next(Instant due, Instant now) {
        requireNonNull(due);
        requireNonNull(now);
        long steps = 1;
        Duration elapsed = Duration.between(due, now);
        if (!elapsed.isNegative()) {
            steps = elapsed.dividedBy(ATTACK_INTERVAL) + 1;
        }
        return due.plus(ATTACK_INTERVAL.multipliedBy(steps));
    }
}
