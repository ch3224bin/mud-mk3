package mudmk3.domain.combat;

import lombok.AccessLevel;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static org.springframework.util.Assert.state;

/**
 * 세계에 나타난 몹 하나(메모리 상태, 엔티티 아님). 배치의 한 자리를 차지하고, 쓰러지면 재등장 시각까지 그 자리에 쓰러진 채로 있다.
 * 교전 상대는 교전한 순서로 위협·피해 기록({@link Engagement})을 쌓는다. 교전이 끝난 상대의 피해 기록은 다시 차기 전까지
 * 남는다(처치자가 나갔거나 졌어도 귀속은 바뀌지 않음 — 룰북 처치 보상 귀속, 몹 스탯)
 */
@Getter
public final class MobInstance {

    private static final double FIRST_OPPONENT_THREAT = 1;
    private static final double TARGET_SWITCH_RATIO = 1.1;

    private final long id;
    private final Long placementId;
    private final int slot;
    private final Long roomId;
    private final int placementCount;
    private MobProfile profile;
    private double qiBlood;
    @Getter(AccessLevel.NONE)
    private boolean alive;
    private @Nullable Instant respawnAt;
    @Getter(AccessLevel.NONE)
    private final LinkedHashMap<Long, Engagement> engagements = new LinkedHashMap<>();
    /** 교전이 끝난 상대의 기록. 다시 교전하면 이어 쓰고, 다시 차기·쓰러짐·재등장 때 지운다 */
    @Getter(AccessLevel.NONE)
    private final Map<Long, Engagement> released = new HashMap<>();
    @Getter(AccessLevel.NONE)
    private long dealtSeq;
    @Getter(AccessLevel.NONE)
    private @Nullable Long killerCharacterId;
    private @Nullable Long targetCharacterId;
    private @Nullable Instant nextActionAt;
    private @Nullable Instant refillAt;
    /** 지금 교전을 시작한 순번(같은 시각 행동의 순서, D86). 교전하지 않으면 뜻이 없다 */
    private long engageOrder;

    private MobInstance(long id, Long placementId, int slot, Long roomId, int placementCount, MobProfile profile) {
        this.id = id;
        this.placementId = requireNonNull(placementId);
        this.slot = slot;
        this.roomId = requireNonNull(roomId);
        this.placementCount = placementCount;
        this.profile = requireNonNull(profile);
        this.qiBlood = profile.stats().qiBlood();
        this.alive = true;
    }

    /** 배치 {@code placementId} 의 {@code slot} 자리에 기혈이 다 찬 채로 나타난다 */
    public static MobInstance spawn(long id, Long placementId, int slot, Long roomId, int placementCount,
                                    MobProfile profile, Instant now) {
        requireNonNull(now);
        return new MobInstance(id, placementId, slot, roomId, placementCount, profile);
    }

    /**
     * 교전 상대로 넣는다. 새 상대면 true. 상대가 없던 몹이면 그 상대의 위협은 1(처음 교전한 상대)이고 대상이 되며,
     * 1.5초 뒤 첫 행동을 하고 다시 차기 예약은 취소된다. 교전이 끝났던 상대가 돌아오면 남은 기록을 이어 쓴다
     */
    public boolean engage(Long characterId, Instant now) {
        requireNonNull(characterId);
        requireNonNull(now);
        state(alive, "쓰러진 몹과는 교전할 수 없습니다");
        if (engagements.containsKey(characterId)) {
            return false;
        }
        Engagement engagement = released.remove(characterId);
        if (engagement == null) {
            engagement = Engagement.start(0);
        }
        if (engagements.isEmpty()) {
            engagement = engagement.withThreatAtLeast(FIRST_OPPONENT_THREAT);
            targetCharacterId = characterId;
            nextActionAt = now.plus(CombatRules.MOB_FIRST_ACTION);
            refillAt = null;
        }
        engagements.put(characterId, engagement);
        return true;
    }

    /** 교전을 시작한 순번을 적는다(상대가 없던 몹이 교전에 들어갈 때 부른다) */
    public void recordEngageOrder(long order) {
        this.engageOrder = order;
    }

    /** 교전 상대에서 뺀다. 상대가 모두 사라지면 10초 뒤 다시 차기를 예약하고 행동을 멈춘다 */
    public void disengage(Long characterId, Instant now) {
        requireNonNull(now);
        Engagement engagement = engagements.remove(characterId);
        if (engagement == null) {
            return;
        }
        released.put(characterId, engagement);
        if (characterId.equals(targetCharacterId)) {
            targetCharacterId = null;
        }
        if (engagements.isEmpty()) {
            refillAt = now.plus(CombatRules.REFILL_DELAY);
            nextActionAt = null;
        }
    }

    /**
     * 교전 상대의 공격을 받는다. 기혈은 육신 층을 거친 피해만큼, 위협·피해 기록은 층을 거치기 전 위력만큼 는다.
     * 쓰러지면 처치자를 정해 두고 교전 상대·기록을 모두 비운 뒤 재등장을 예약한다(쓰러지기 전 상대는 부르는 쪽이 미리 읽는다)
     */
    public MobHit receive(Long characterId, double power, Instant now) {
        requireNonNull(now);
        state(alive, "쓰러진 몹은 맞지 않습니다");
        Engagement engagement = engagements.get(characterId);
        state(engagement != null, "교전 상대가 아닙니다: " + characterId);
        double attackPower = Math.max(0, power);
        engagements.put(characterId, requireNonNull(engagement).add(attackPower, dealtSeq++));
        double damage = Damage.afterBody(attackPower, profile.stats().body());
        qiBlood = Math.max(0, qiBlood - damage);
        if (qiBlood > 0) {
            return new MobHit(damage, false);
        }
        killerCharacterId = topDealer().orElse(null);
        alive = false;
        clearRecords();
        refillAt = null;
        respawnAt = now.plus(Duration.ofSeconds(profile.respawnSeconds()));
        return new MobHit(damage, true);
    }

    /**
     * 공격할 상대: 위협이 가장 높은 상대(같으면 먼저 교전한 쪽). 지금 대상이 있으면 새 상대의 위협이 지금 대상의 1.1배를 넘을
     * 때만 바꾸고, 대상이 빠졌으면 남은 상대 중 위협이 가장 높은 상대로 바꾼다(룰북 몹의 대상 고르기)
     */
    public Optional<Long> target() {
        Map.Entry<Long, Engagement> top = null;
        for (Map.Entry<Long, Engagement> entry : engagements.entrySet()) {
            if (top == null || entry.getValue().threat() > top.getValue().threat()) {
                top = entry;
            }
        }
        if (top == null) {
            targetCharacterId = null;
            return Optional.empty();
        }
        Engagement current = targetCharacterId == null ? null : engagements.get(targetCharacterId);
        if (current == null || top.getValue().threat() > current.threat() * TARGET_SWITCH_RATIO) {
            targetCharacterId = top.getKey();
        }
        return Optional.ofNullable(targetCharacterId);
    }

    /** 처치자(쓰러진 뒤에는 쓰러질 때 정한 사람): 피해가 가장 많은 상대, 같으면 먼저 피해를 준 쪽. 교전이 끝난 상대의 기록도 센다 */
    public Optional<Long> killer() {
        if (!alive) {
            return Optional.ofNullable(killerCharacterId);
        }
        return topDealer();
    }

    /** 지금 교전 상대(교전한 순서) */
    public Set<Long> opponentIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(engagements.keySet()));
    }

    /** 지금 교전 상대의 기록 */
    public Optional<Engagement> engagementOf(Long characterId) {
        return Optional.ofNullable(engagements.get(characterId));
    }

    public boolean isAlive() {
        return alive;
    }

    /** 살아 있고 교전 상대가 있다 */
    public boolean isFighting() {
        return alive && !engagements.isEmpty();
    }

    public Condition condition() {
        return Condition.of(qiBlood, profile.stats().qiBlood());
    }

    /** 다시 차기 시각이 되었으면 기혈을 다 채우고 위협·피해 기록을 지운다 */
    public void refillIfDue(Instant now) {
        if (refillAt == null || now.isBefore(refillAt)) {
            return;
        }
        qiBlood = profile.stats().qiBlood();
        clearRecords();
        refillAt = null;
    }

    public boolean respawnDue(Instant now) {
        return !alive && respawnAt != null && !now.isBefore(respawnAt);
    }

    /** 쓰러졌던 자리에 지금 정의({@code profile})로 기혈이 다 찬 채로 다시 나타난다 */
    public void respawn(MobProfile newProfile, Instant now) {
        requireNonNull(now);
        state(!alive, "살아 있는 몹은 다시 나타나지 않습니다");
        this.profile = requireNonNull(newProfile);
        this.qiBlood = newProfile.stats().qiBlood();
        this.alive = true;
        this.respawnAt = null;
        this.killerCharacterId = null;
        clearRecords();
        this.refillAt = null;
    }

    /** {@code due} 에 할 행동을 했다. 다음은 {@link CombatRules#next} */
    public void actedAt(Instant due, Instant now) {
        nextActionAt = CombatRules.next(due, now);
    }

    private Optional<Long> topDealer() {
        Map.Entry<Long, Engagement> top = null;
        for (Map.Entry<Long, Engagement> entry : allRecords().entrySet()) {
            Engagement engagement = entry.getValue();
            if (!engagement.hasDealt()) continue;
            if (top == null || engagement.dealt() > top.getValue().dealt()
                    || (engagement.dealt() == top.getValue().dealt()
                    && engagement.firstDealtSeq() < top.getValue().firstDealtSeq())) {
                top = entry;
            }
        }
        return top == null ? Optional.empty() : Optional.of(top.getKey());
    }

    private Map<Long, Engagement> allRecords() {
        Map<Long, Engagement> all = new LinkedHashMap<>(released);
        all.putAll(engagements);
        return all;
    }

    private void clearRecords() {
        engagements.clear();
        released.clear();
        targetCharacterId = null;
        nextActionAt = null;
    }
}
