package mudmk3.application.game;

import lombok.RequiredArgsConstructor;
import mudmk3.application.character.provided.CharacterFinder;
import mudmk3.application.game.required.GameMessage;
import mudmk3.application.game.required.GameMessage.NoticeMessage;
import mudmk3.application.game.required.GameMessage.SystemMessage;
import mudmk3.application.game.required.MobInstanceRepository;
import mudmk3.application.game.required.PlayerCombatRepository;
import mudmk3.application.game.required.PresenceRepository;
import mudmk3.application.npc.provided.NpcFinder;
import mudmk3.domain.combat.AttackNature;
import mudmk3.domain.combat.MobHit;
import mudmk3.domain.combat.MobInstance;
import mudmk3.domain.combat.PlayerCombat;
import mudmk3.domain.game.OnlinePlayer;
import mudmk3.domain.item.ItemNames;
import mudmk3.domain.item.ItemQuery;
import mudmk3.domain.npc.Npc;
import mudmk3.domain.shared.Josa;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

/**
 * 5차 {@code 공격} 과 신통이 함께 쓰는 교전 경로(D110, 이슈 #15): 칠 몹 찾기, 교전 시작, 몹 치기, 몹 처치, 같은 방 알림.
 * 교전 시작 순번과 시전 시작 순번도 여기서 매긴다. {@link GameService} 의 게임 락과 트랜잭션 안에서만 불린다
 */
@Component
@RequiredArgsConstructor
class CombatActions {

    private final MobInstanceRepository mobInstanceRepository;
    private final PlayerCombatRepository playerCombatRepository;
    private final CorpseHandler corpseHandler;
    private final PresenceRepository presenceRepository;
    private final NpcFinder npcFinder;
    private final CharacterFinder characterFinder;
    private final CombatTexts combatTexts;

    /** 교전 시작 순번(같은 시각 행동의 순서). 늘기만 한다 */
    private final AtomicLong engageOrders = new AtomicLong();
    /** 시전 시작 순번(F20). 늘기만 한다 */
    private final AtomicLong castingSeqs = new AtomicLong();

    /**
     * 같은 방에서 칠 몹을 찾는다(5차 {@code 공격} 찾기 순서): 몹(방 보기 순서, 이름·번호)이 없으면 NPC(2차 {@code 대화}
     * 찾기), 플레이어(이름 앞부분) 순으로 보고 거절 문구를 고른다
     */
    TargetLookup findTarget(OnlinePlayer player, ItemQuery query) {
        Optional<MobInstance> found = ItemNames.select(mobsInRoom(player.getRoomId()), mob -> mob.getProfile().name(), query);
        if (found.isPresent()) {
            return new TargetLookup.Found(found.get());
        }
        return new TargetLookup.Rejected(notAttackableTarget(player, ItemCommandHandler.inputOf(query)));
    }

    private String notAttackableTarget(OnlinePlayer player, String input) {
        List<Npc> npcs = npcFinder.findAllInRoom(player.getRoomId());
        Optional<Npc> npc = npcs.stream().filter(n -> n.isCalled(input)).findFirst()
                .or(() -> npcs.stream().filter(n -> n.nameStartsWith(input)).findFirst());
        if (npc.isPresent()) {
            return combatTexts.notAttackable(npc.get().getName());
        }
        String lower = input.toLowerCase(Locale.ROOT);
        boolean isPlayer = presenceRepository.findAllInRoom(player.getRoomId()).stream()
                .anyMatch(other -> other.getName().toLowerCase(Locale.ROOT).startsWith(lower));
        if (isPlayer) {
            return CombatTexts.ATTACK_PLAYER;
        }
        return "이곳에 '" + input + "'" + Josa.eunNeun(input) + " 없습니다.";
    }

    /**
     * 그 방의 살아 있는 몹을 방 보기 순서로: 묶음 키 (몹 유형, 교전 중)가 처음 나온 순서대로 묶음을 놓고,
     * 묶음 안은 (배치 id, 자리) 순. 이름·번호로 고를 때도 이 순서다(D86·D89)
     */
    List<MobInstance> mobsInRoom(Long roomId) {
        Map<GroupKey, List<MobInstance>> groups = new LinkedHashMap<>();
        for (MobInstance mob : mobInstanceRepository.findAllInRoom(roomId)) {
            groups.computeIfAbsent(GroupKey.of(mob), key -> new ArrayList<>()).add(mob);
        }
        return groups.values().stream().flatMap(List::stream).toList();
    }

    /**
     * 아직 교전하지 않던 몹과 교전을 시작한다: 서로의 교전 상대 목록에 넣고(교전하지 않던 쪽은 교전 시작 순번을 받음 —
     * 플레이어가 먼저) {@code 청모랑과 싸움이 벌어진다.}, 같은 방 다른 사람에게 {@code 한백이 청모랑과 싸우기 시작한다.}
     * (event {@code engage})
     */
    void startEngagement(OnlinePlayer player, PlayerCombat combat, MobInstance mob, Instant now,
                         BiConsumer<String, GameMessage> send) {
        if (!combat.isFighting()) {
            combat.recordEngageOrder(engageOrders.incrementAndGet());
        }
        if (!mob.isFighting()) {
            mob.recordEngageOrder(engageOrders.incrementAndGet());
        }
        mob.engage(combat.getCharacterId(), now);
        combat.engage(mob.getId());
        playerCombatRepository.save(combat);
        mobInstanceRepository.save(mob);
        String mobName = mob.getProfile().name();
        send.accept(player.getSessionId(), SystemMessage.combat(combatTexts.engaged(mobName)));
        notifyOthers(player, combatTexts.engagedNotice(player.getName(), mobName), "engage", send);
    }

    /** 몹이 교전 상대 {@code characterId} 의 공격을 받는다(받는 쪽 배율 → 육신 층 → 기혈) */
    MobHit strike(Long characterId, MobInstance mob, double power, AttackNature nature, Instant now) {
        MobHit hit = mob.receive(characterId, power, nature, now);
        mobInstanceRepository.save(mob);
        return hit;
    }

    /**
     * 몹이 쓰러졌다: 그 자리에 시체를 남기고(드롭은 지금 굴림, D87) 처치자 {@code 청모랑을 쓰러뜨렸습니다.},
     * 교전하던 다른 사람 {@code 청모랑이 쓰러졌다.}, 같은 방의 교전하지 않은 사람 {@code 한백이 청모랑을 쓰러뜨렸다.}
     * (event {@code kill}) → 교전하던 플레이어마다 그 몹과의 교전을 끝낸다 → 맨 끝에 그 몹의 깨달음 한 상대 상한을 지운다
     * (쓰러뜨린 일격의 깨달음을 쌓은 뒤, F1).
     * 처치자는 피해가 가장 많은 사람이라 마지막 일격을 한 사람과 다를 수 있다. {@code opponents} 는 쓰러지기 전에 읽은
     * 교전 상대다(쓰러지면 몹의 상대 목록이 비므로)
     */
    void onMobKilled(MobInstance mob, Set<Long> opponents, Long strikerId, Instant now,
                     BiConsumer<String, GameMessage> send) {
        String mobName = mob.getProfile().name();
        Long killerId = mob.killer().orElse(strikerId);
        corpseHandler.leaveCorpse(mob, killerId, now);
        String killerName = presenceRepository.findByCharacterId(killerId).map(OnlinePlayer::getName)
                .orElseGet(() -> characterFinder.find(killerId).getName().value());
        NoticeMessage killedNotice = NoticeMessage.combat(combatTexts.killedNotice(killerName, mobName), killerName, "kill");
        for (OnlinePlayer other : presenceRepository.findAllInRoom(mob.getRoomId())) {
            Long otherId = other.getCharacterId();
            if (otherId.equals(killerId)) {
                send.accept(other.getSessionId(), SystemMessage.combat(combatTexts.killed(mobName)));
            } else if (opponents.contains(otherId)) {
                send.accept(other.getSessionId(), SystemMessage.combat(combatTexts.fell(mobName)));
            } else {
                send.accept(other.getSessionId(), killedNotice);
            }
        }
        for (Long opponentId : opponents) {
            playerCombatRepository.find(opponentId).ifPresent(combat -> {
                combat.disengage(mob.getId(), now);
                playerCombatRepository.save(combat);
            });
        }
        mob.clearInsightCaps();
        mobInstanceRepository.save(mob);
    }

    /** 플레이어와 몹이 같은 방에서 서로 교전 중이다 */
    boolean inCombatTogether(OnlinePlayer player, MobInstance mob) {
        return mob.isAlive() && mob.getRoomId().equals(player.getRoomId())
                && mob.opponentIds().contains(player.getCharacterId())
                && playerCombatRepository.find(player.getCharacterId())
                .map(combat -> combat.getMobIds().contains(mob.getId())).orElse(false);
    }

    /** 같은 방 다른 사람에게 {@code notice}(tone {@code combat}) */
    void notifyOthers(OnlinePlayer actor, String text, String event, BiConsumer<String, GameMessage> send) {
        NoticeMessage notice = NoticeMessage.combat(text, actor.getName(), event);
        presenceRepository.findAllInRoom(actor.getRoomId()).stream()
                .filter(other -> !other.getCharacterId().equals(actor.getCharacterId()))
                .forEach(other -> send.accept(other.getSessionId(), notice));
    }

    /** 다음 시전 시작 순번(같은 시각에 발동하는 아직 교전하지 않은 캐릭터의 시전 순서) */
    long nextCastingSeq() {
        return castingSeqs.incrementAndGet();
    }

    /** 방 보기 묶음 키 */
    private record GroupKey(Long mobTypeId, boolean fighting) {
        static GroupKey of(MobInstance mob) {
            return new GroupKey(mob.getProfile().mobTypeId(), mob.isFighting());
        }
    }
}
