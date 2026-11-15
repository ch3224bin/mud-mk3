package mudmk3.domain.combat;

import lombok.AccessLevel;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * 캐릭터 한 명의 신통 대기시간과 시전(메모리, 엔티티 아님): 공통 대기시간이 끝나는 시각, 신통마다 재사용 대기시간이 끝나는
 * 시각, 시전 중인 신통. 대기시간은 세계에서 사라져도 남는다(룰북)
 */
@Getter
public final class AbilityTimers {

    private final Long characterId;
    private @Nullable Instant globalReadyAt;
    @Getter(AccessLevel.NONE)
    private final Map<AbilityKey, Instant> cooldowns = new HashMap<>();
    @Getter(AccessLevel.NONE)
    private @Nullable Casting casting;

    public AbilityTimers(Long characterId) {
        this.characterId = requireNonNull(characterId);
    }

    /** 신통마다 재사용 대기시간이 끝나는 시각 */
    public Map<AbilityKey, Instant> getCooldowns() {
        return Collections.unmodifiableMap(cooldowns);
    }

    /** 공통 대기시간의 남은 시간. 끝났으면(끝나는 시각 포함) 빈 값 */
    public Optional<Duration> globalRemaining(Instant now) {
        return remaining(globalReadyAt, now);
    }

    /** 그 신통 재사용 대기시간의 남은 시간. 끝났거나 쓴 적이 없으면 빈 값 */
    public Optional<Duration> cooldownRemaining(AbilityKey key, Instant now) {
        return remaining(cooldowns.get(key), now);
    }

    /** 공통 대기시간을 지금부터 1.5초로 둔다 */
    public void startGlobal(Instant now) {
        globalReadyAt = now.plus(AbilityRules.GLOBAL_COOLDOWN);
    }

    /** 그 신통의 재사용 대기시간을 지금부터 {@code seconds} 초로 둔다 */
    public void startCooldown(AbilityKey key, BigDecimal seconds, Instant now) {
        requireNonNull(key);
        cooldowns.put(key, now.plus(AbilityRules.seconds(seconds)));
    }

    /** 시전을 시작한다 */
    public void startCasting(Casting casting) {
        this.casting = requireNonNull(casting);
    }

    /** 시전 중인 신통 */
    public Optional<Casting> casting() {
        return Optional.ofNullable(casting);
    }

    public void clearCasting() {
        casting = null;
    }

    private static Optional<Duration> remaining(@Nullable Instant readyAt, Instant now) {
        requireNonNull(now);
        if (readyAt == null || !now.isBefore(readyAt)) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(now, readyAt));
    }
}
