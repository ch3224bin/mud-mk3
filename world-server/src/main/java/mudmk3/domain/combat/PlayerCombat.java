package mudmk3.domain.combat;

import lombok.AccessLevel;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * 플레이어 한 명의 교전 상태(메모리, 엔티티 아님): 교전한 순서의 몹 id, 노리는 몹, 다음 공격 시각(3초 간격, 교전이 끝나도
 * 남아 다른 몹을 쳐도 간격이 줄지 않음), 기혈 다시 차기 시각, 법력 회복 기준 시각
 */
@Getter
public final class PlayerCombat {

    private final Long characterId;
    @Getter(AccessLevel.NONE)
    private final List<Long> mobIds = new ArrayList<>();
    private @Nullable Long targetMobId;
    private @Nullable Instant nextAttackAt;
    private @Nullable Instant refillAt;
    /** 법력 회복을 마지막으로 셈한 시각. 세계에 들어올 때 정하고 법력 회복 틱마다 옮긴다 */
    private @Nullable Instant manaRegenAt;
    /** 지금 교전을 시작한 순번(같은 시각 행동의 순서, D86). 교전하지 않으면 뜻이 없다 */
    private long engageOrder;

    public PlayerCombat(Long characterId) {
        this.characterId = requireNonNull(characterId);
    }

    /** 교전 상대(교전한 순서) */
    public List<Long> getMobIds() {
        return Collections.unmodifiableList(mobIds);
    }

    /** 교전 상대로 넣는다(이미 있으면 그대로). 다시 교전하면 다시 차기는 취소된다 */
    public void engage(long mobId) {
        if (!mobIds.contains(mobId)) {
            mobIds.add(mobId);
        }
        refillAt = null;
    }

    /** 교전을 시작한 순번을 적는다(교전하지 않던 플레이어가 교전에 들어갈 때 부른다) */
    public void recordEngageOrder(long order) {
        this.engageOrder = order;
    }

    /** 노리는 몹을 정한다 */
    public void target(long mobId) {
        targetMobId = mobId;
    }

    /** 교전 상대에서 뺀다. 상대가 모두 사라지면 10초 뒤 다시 차기를 예약한다 */
    public void disengage(long mobId, Instant now) {
        requireNonNull(now);
        if (!mobIds.remove(Long.valueOf(mobId))) {
            return;
        }
        if (targetMobId != null && targetMobId == mobId) {
            targetMobId = null;
        }
        if (mobIds.isEmpty()) {
            scheduleRefill(now);
        }
    }

    /** 모든 교전을 끝낸다. 교전 중이었으면 10초 뒤 다시 차기를 예약한다(아니면 예약은 그대로) */
    public void endAll(Instant now) {
        requireNonNull(now);
        targetMobId = null;
        if (mobIds.isEmpty()) {
            return;
        }
        mobIds.clear();
        scheduleRefill(now);
    }

    /** 칠 몹: 노리는 몹이 교전 상대에 있으면 그 몹, 아니면 가장 먼저 교전한 몹 */
    public Optional<Long> currentTarget() {
        if (targetMobId != null && mobIds.contains(targetMobId)) {
            return Optional.of(targetMobId);
        }
        return mobIds.stream().findFirst();
    }

    /** 앞 공격에서 3초가 지났다(친 적이 없으면 늘 참) */
    public boolean readyAt(Instant now) {
        return nextAttackAt == null || !now.isBefore(nextAttackAt);
    }

    /** {@code due} 에 할 공격을 했다. 다음은 {@link CombatRules#next} */
    public void attackedAt(Instant due, Instant now) {
        nextAttackAt = CombatRules.next(due, now);
    }

    public boolean isFighting() {
        return !mobIds.isEmpty();
    }

    /** 10초 뒤 기혈 다시 차기를 예약한다 */
    public void scheduleRefill(Instant now) {
        refillAt = now.plus(CombatRules.REFILL_DELAY);
    }

    public boolean refillDue(Instant now) {
        return refillAt != null && !now.isBefore(refillAt);
    }

    public void clearRefill() {
        refillAt = null;
    }

    /** 법력 회복 기준 시각을 지금으로 둔다(세계에 들어올 때) */
    public void startManaRegen(Instant now) {
        manaRegenAt = requireNonNull(now);
    }

    /**
     * 지난 기준 시각부터 흐른 초를 0~1초로 잘라 돌려주고 기준 시각을 지금으로 옮긴다. 기준 시각이 없으면 0초
     */
    public double manaRegenSeconds(Instant now) {
        requireNonNull(now);
        Instant from = manaRegenAt;
        manaRegenAt = now;
        if (from == null) {
            return 0;
        }
        return Math.clamp(Duration.between(from, now).toNanos() / 1e9, 0.0, 1.0);
    }
}
