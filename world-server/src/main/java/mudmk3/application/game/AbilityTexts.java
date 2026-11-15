package mudmk3.application.game;

import mudmk3.domain.combat.Damage;
import mudmk3.domain.shared.Durations;
import mudmk3.domain.shared.Josa;
import mudmk3.domain.shared.Numbers;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * 신통 쓰기의 거절 문구와 신통 교전 줄을 그린다(command.md {@code ### 신통 쓰기}·{@code ### 신통 교전 메시지}).
 * 문구만 맡고 상태를 바꾸지 않는다
 */
@Component
class AbilityTexts {

    static final String NO_SPIRIT = "영력이 없어 술법을 펼칠 수 없습니다.";
    static final String INSIGHT_CAPPED = "같은 상대에게서는 더 깨닫는 것이 없습니다.";

    private static final long NANOS_PER_TENTH = 100_000_000L;
    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    private static final long MINUTE_NANOS = 60 * NANOS_PER_SECOND;

    /**
     * 남은 시간(D112): 60초 미만은 0.1초 단위로 올림({@code 0.0초} 가 보이지 않게), 60초 이상은 1초 단위로 올림한 뒤
     * 4차 시간 문체({@link Durations#korean})
     */
    static String remaining(Duration duration) {
        long nanos = Math.max(0, duration.toNanos());
        if (nanos < MINUTE_NANOS) {
            return Durations.korean(BigDecimal.valueOf(Math.ceilDiv(nanos, NANOS_PER_TENTH), 1));
        }
        return Durations.korean(BigDecimal.valueOf(Math.ceilDiv(nanos, NANOS_PER_SECOND)));
    }

    /** {@code 화탄술은 아직 깨치지 못한 신통입니다.} */
    String notLearned(String abilityName) {
        return Josa.withEunNeun(abilityName) + " 아직 깨치지 못한 신통입니다.";
    }

    /** {@code 어풍결은 아직 쓸 수 없는 신통입니다.} */
    String unusable(String abilityName) {
        return Josa.withEunNeun(abilityName) + " 아직 쓸 수 없는 신통입니다.";
    }

    /** {@code 누구에게 쓰시겠습니까? (예: 화탄술 늑대)} — 예의 신통 이름은 친 이름 */
    String noTarget(String typedName) {
        return "누구에게 쓰시겠습니까? (예: " + typedName + " 늑대)";
    }

    /** {@code 아직 기운을 고르는 중입니다. (0.3초)} */
    String globalCooldown(Duration remaining) {
        return "아직 기운을 고르는 중입니다. (" + remaining(remaining) + ")";
    }

    /** {@code 화탄술은 아직 쓸 수 없습니다. (1.4초)} */
    String cooldown(String abilityName, Duration remaining) {
        return Josa.withEunNeun(abilityName) + " 아직 쓸 수 없습니다. (" + remaining(remaining) + ")";
    }

    /** {@code 법력이 모자랍니다. (필요 5, 남은 것 3)} — 필요는 소모량 그대로(L6-3), 남은 것은 게이지 정수 */
    String notEnoughMana(BigDecimal cost, double manaCurrent) {
        return "법력이 모자랍니다. (필요 " + Numbers.decimal(cost) + ", 남은 것 " + Damage.gauge(manaCurrent) + ")";
    }

    /** 법력을 쓰지 않은 이펙트 줄의 꼬리: {@code (피해 8)} */
    String effectSuffix(double damage) {
        return "(피해 " + Damage.display(damage) + ")";
    }

    /** 법력을 쓴 이펙트 줄의 꼬리: {@code (피해 8, 법력 20/25)} — 법력은 소모한 뒤의 현재/최대 */
    String effectSuffix(double damage, double manaCurrent, int manaMax) {
        return "(피해 " + Damage.display(damage) + ", 법력 " + Damage.gauge(manaCurrent) + "/" + manaMax + ")";
    }

    /** 이펙트 줄: {@code {이펙트 문장} (피해 8, 법력 20/25)} */
    String effectLine(String renderedLine, String suffix) {
        return renderedLine + " " + suffix;
    }
}
