package mudmk3.domain.combat;

import java.time.Duration;

/** 신통 대기시간 규칙(룰북 대기시간) */
public final class AbilityRules {

    /** 공통 대기시간: 어떤 신통이든 쓰고 나면 1.5초 동안 다른 신통을 쓸 수 없다 */
    public static final Duration GLOBAL_COOLDOWN = Duration.ofMillis(1500);

    private AbilityRules() {
    }
}
