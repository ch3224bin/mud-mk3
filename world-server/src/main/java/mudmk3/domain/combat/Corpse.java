package mudmk3.domain.combat;

import lombok.AccessLevel;
import lombok.Getter;
import mudmk3.domain.mob.DropLine;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * 쓰러진 몹이 남긴 시체(메모리, 엔티티 아님). 드롭은 쓰러질 때 정해져 {@code remaining} 에 있고 수습한 만큼 빠진다.
 * 처음 60초는 처치자만 수습할 수 있고 3분 뒤 남은 것과 함께 사라진다(룰북 시체와 수습)
 */
@Getter
public final class Corpse {

    private final long id;
    private final Long roomId;
    private final String mobName;
    private final @Nullable Long killerCharacterId;
    private final Instant diedAt;
    @Getter(AccessLevel.NONE)
    private final List<DropLine> remaining;

    public Corpse(long id, Long roomId, String mobName, @Nullable Long killerCharacterId, Instant diedAt,
                  List<DropLine> remaining) {
        this.id = id;
        this.roomId = requireNonNull(roomId);
        this.mobName = requireNonNull(mobName);
        this.killerCharacterId = killerCharacterId;
        this.diedAt = requireNonNull(diedAt);
        this.remaining = new ArrayList<>(remaining);
    }

    /** {@code 청모랑의 시체} */
    public String name() {
        return mobName + "의 시체";
    }

    /** 남은 드롭(쓰러질 때 정한 순서) */
    public List<DropLine> getRemaining() {
        return Collections.unmodifiableList(remaining);
    }

    /** 처치자이거나 쓰러진 지 60초가 지났다 */
    public boolean collectibleBy(Long characterId, Instant now) {
        return characterId.equals(killerCharacterId) || lockedFor(now).isZero();
    }

    /** 누구나 수습할 수 있게 될 때까지 남은 시간(지났으면 0) */
    public Duration lockedFor(Instant now) {
        Duration left = Duration.between(now, diedAt.plus(CombatRules.CORPSE_EXCLUSIVE));
        return left.isNegative() ? Duration.ZERO : left;
    }

    /** 쓰러진 지 3분이 지났다 */
    public boolean expired(Instant now) {
        return !now.isBefore(diedAt.plus(CombatRules.CORPSE_LIFETIME));
    }

    public boolean isEmpty() {
        return remaining.isEmpty();
    }

    /** {@code index} 줄에서 {@code count} 개를 거둔다. 다 거둔 줄은 빠진다 */
    public void takeFrom(int index, int count) {
        DropLine line = remaining.get(index);
        if (count < 1 || count > line.count()) {
            throw new IllegalArgumentException("거둘 수는 1~" + line.count() + "이어야 합니다: " + count);
        }
        if (count == line.count()) {
            remaining.remove(index);
        } else {
            remaining.set(index, new DropLine(line.itemTypeId(), line.name(), line.count() - count));
        }
    }
}
