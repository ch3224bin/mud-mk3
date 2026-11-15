package mudmk3.domain.combat;

import java.math.BigDecimal;
import java.time.Instant;

import static java.util.Objects.requireNonNull;

/**
 * 시전 중인 신통 하나(메모리): 공법 id·신통 이름·대상 몹 id만 들고, 발동 때 정의를 DB에서 다시 읽는다.
 * {@code seq} 는 시전을 시작한 순번
 */
public record Casting(Long techniqueId, String abilityName, long mobId, Instant firesAt, long seq) {

    public Casting {
        requireNonNull(techniqueId);
        requireNonNull(abilityName);
        requireNonNull(firesAt);
    }

    /** 지금 시작해 {@code castSeconds} 초 뒤에 발동하는 시전 */
    public static Casting start(AbilityKey key, long mobId, BigDecimal castSeconds, Instant now, long seq) {
        return new Casting(key.techniqueId(), key.abilityName(), mobId, now.plus(AbilityRules.seconds(castSeconds)), seq);
    }

    public AbilityKey key() {
        return new AbilityKey(techniqueId, abilityName);
    }
}
