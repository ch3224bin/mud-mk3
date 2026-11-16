package mudmk3.application.game;

import lombok.RequiredArgsConstructor;
import mudmk3.application.game.required.GameMessage;
import mudmk3.application.game.required.GameMessage.NoticeMessage;
import mudmk3.application.game.required.MobInstanceRepository;
import mudmk3.application.game.required.PlayerCombatRepository;
import mudmk3.application.game.required.PresenceRepository;
import mudmk3.domain.combat.MobInstance;
import mudmk3.domain.combat.MobProfile;
import mudmk3.domain.game.OnlinePlayer;
import mudmk3.domain.mob.MobPlacement;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import static java.util.Objects.requireNonNull;

/**
 * 몹이 나타나고 차는 틱 단계(platform.md {@code #### 서버 틱}): 1 배치 맞추기, 3 몹 다시 차기, 5 재등장.
 * {@link CombatEngine#tick} 이 순서대로 부른다. {@link GameService} 의 게임 락과 트랜잭션 안에서만 불린다
 */
@Component
@RequiredArgsConstructor
class MobSpawner {

    private final MobInstanceRepository mobInstanceRepository;
    private final PlayerCombatRepository playerCombatRepository;
    private final PresenceRepository presenceRepository;
    private final CombatTexts combatTexts;

    /** 몹 id. 서버가 돌아가는 동안 늘기만 한다 */
    private final AtomicLong mobIds = new AtomicLong();

    /**
     * 1. 배치 맞추기: 배치가 없어졌거나 서명(방, 몹 유형, 수)이 바뀐 몹을 지우고(교전 중이던 플레이어는 교전에서 뺌, 알림 없음),
     * 배치의 빈 자리에 지금 정의로 몹을 나타나게 한다
     */
    void reconcile(Map<Long, MobPlacement> placements, Instant now, BiConsumer<String, GameMessage> send) {
        Map<Long, Set<Integer>> filledSlots = new LinkedHashMap<>();
        for (MobInstance mob : mobInstanceRepository.findAll()) {
            MobPlacement placement = placements.get(mob.getPlacementId());
            if (placement == null || !matches(mob, placement)) {
                remove(mob, now);
                continue;
            }
            filledSlots.computeIfAbsent(mob.getPlacementId(), id -> new HashSet<>()).add(mob.getSlot());
        }
        for (MobPlacement placement : placements.values()) {
            Long placementId = requireNonNull(placement.getId());
            Set<Integer> filled = filledSlots.getOrDefault(placementId, Set.of());
            for (int slot = 0; slot < placement.getCount(); slot++) {
                if (filled.contains(slot)) continue;
                MobInstance mob = MobInstance.spawn(mobIds.incrementAndGet(), placementId, slot,
                        placement.getRoomId(), placement.getCount(), MobProfile.of(placement.getMobType()), now);
                mobInstanceRepository.save(mob);
                notifyAppeared(mob, send);
            }
        }
    }

    private static boolean matches(MobInstance mob, MobPlacement placement) {
        return mob.getRoomId().equals(placement.getRoomId())
                && mob.getProfile().mobTypeId().equals(placement.getMobType().getId())
                && mob.getPlacementCount() == placement.getCount();
    }

    private void remove(MobInstance mob, Instant now) {
        for (Long characterId : mob.opponentIds()) {
            playerCombatRepository.find(characterId).ifPresent(combat -> {
                combat.disengage(mob.getId(), now);
                playerCombatRepository.save(combat);
            });
        }
        mobInstanceRepository.delete(mob.getId());
    }

    /** 3. 몹 다시 차기: 교전 상대가 모두 사라진 지 10초가 된 몹은 기혈이 다 차고 위협·피해 기록을 지운다 */
    void refill(Instant now) {
        for (MobInstance mob : mobInstanceRepository.findAll()) {
            if (mob.isAlive()) {
                mob.refillIfDue(now);
            }
        }
    }

    /** 5. 재등장: 재등장 시각이 된 몹을 배치의 지금 정의로 같은 자리에 기혈이 다 찬 채 나타나게 한다 */
    void respawn(Map<Long, MobPlacement> placements, Instant now, BiConsumer<String, GameMessage> send) {
        for (MobInstance mob : mobInstanceRepository.findAll()) {
            if (!mob.respawnDue(now)) continue;
            // 배치 맞추기가 서명이 다른 몹을 이미 지웠으므로 배치가 있다
            MobPlacement placement = requireNonNull(placements.get(mob.getPlacementId()));
            mob.respawn(MobProfile.of(placement.getMobType()), now);
            mobInstanceRepository.save(mob);
            notifyAppeared(mob, send);
        }
    }

    /** 그 방 사람에게 {@code 청모랑이 나타난다.}(event {@code respawn}) */
    private void notifyAppeared(MobInstance mob, BiConsumer<String, GameMessage> send) {
        String name = mob.getProfile().name();
        GameMessage notice = NoticeMessage.combat(combatTexts.appears(name), name, "respawn");
        for (OnlinePlayer player : presenceRepository.findAllInRoom(mob.getRoomId())) {
            send.accept(player.getSessionId(), notice);
        }
    }
}
