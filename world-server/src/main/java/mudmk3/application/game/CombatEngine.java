package mudmk3.application.game;

import lombok.RequiredArgsConstructor;
import mudmk3.application.character.provided.CharacterFinder;
import mudmk3.application.character.provided.CharacterVitalsEditor;
import mudmk3.application.cultivation.provided.CultivationFinder;
import mudmk3.application.game.required.CorpseRepository;
import mudmk3.application.game.required.GameMessage;
import mudmk3.application.game.required.GameMessage.NoticeMessage;
import mudmk3.application.game.required.GameMessage.SystemMessage;
import mudmk3.application.game.required.MobInstanceRepository;
import mudmk3.application.game.required.PlayerCombatRepository;
import mudmk3.application.game.required.PresenceRepository;
import mudmk3.application.mob.provided.MobPlacementFinder;
import mudmk3.application.npc.provided.NpcFinder;
import mudmk3.domain.combat.Corpse;
import mudmk3.domain.combat.Damage;
import mudmk3.domain.combat.MobHit;
import mudmk3.domain.combat.MobInstance;
import mudmk3.domain.combat.MobProfile;
import mudmk3.domain.combat.PlayerCombat;
import mudmk3.domain.cultivation.StatSheet;
import mudmk3.domain.game.OnlinePlayer;
import mudmk3.domain.item.ItemNames;
import mudmk3.domain.item.ItemQuery;
import mudmk3.domain.mob.MobPlacement;
import mudmk3.domain.npc.Npc;
import mudmk3.domain.shared.Josa;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * 몹·교전·시체 메모리 상태를 다룬다(D85·D86). {@link GameService} 가 게임 락과 트랜잭션 안에서 부른다.
 * 서버 틱은 platform.md {@code #### 서버 틱} 순서대로 처리한다: 1 배치 맞추기 → 2 행동 → 3 다시 차기 → 4 시체 사라짐 → 5 재등장.
 * <p>
 * 틱이 중간에 실패하면 트랜잭션(깎인 기혈 쓰기)은 롤백되지만 메모리 상태는 되돌리지 않고 보낼 줄도 버려진다
 * ({@link GameService} 의 1차 한계). 기혈 쓰기는 몹 행동 하나에서 메모리를 바꾸기 전에 하므로, 어긋나도 "몹이 친 것이 DB에
 * 남지 않음"(플레이어 쪽 이득) 또는 "플레이어의 일격·처치만 메모리에 남음" 쪽이라 해롭지 않다(받아들임)
 */
@Component
@RequiredArgsConstructor
class CombatEngine {

    private final MobPlacementFinder mobPlacementFinder;
    private final MobInstanceRepository mobInstanceRepository;
    private final PlayerCombatRepository playerCombatRepository;
    private final CorpseRepository corpseRepository;
    private final PresenceRepository presenceRepository;
    private final NpcFinder npcFinder;
    private final CharacterFinder characterFinder;
    private final CultivationFinder cultivationFinder;
    private final CharacterVitalsEditor characterVitalsEditor;
    private final CombatTexts combatTexts;

    /** 몹 id. 서버가 돌아가는 동안 늘기만 한다 */
    private final AtomicLong mobIds = new AtomicLong();
    /** 교전 시작 순번(같은 시각 행동의 순서). 늘기만 한다 */
    private final AtomicLong engageOrders = new AtomicLong();

    /** 틱 하나. send 는 (연결 id, 메시지)로 보낼 것을 모은다. 이번 틱에 진 캐릭터를 돌려준다 */
    CombatOutcome tick(Instant now, BiConsumer<String, GameMessage> send) {
        Map<Long, MobPlacement> placements = mobPlacementFinder.findAll().stream()
                .collect(Collectors.toMap(placement -> requireNonNull(placement.getId()), Function.identity(),
                        (a, b) -> a, LinkedHashMap::new));
        reconcile(placements, now, send);
        List<Long> defeated = new ArrayList<>();
        act(now, send, defeated);
        refillMobs(now);
        respawnMobs(placements, now, send);
        return new CombatOutcome(defeated);
    }

    // ---- 공격 명령

    /**
     * {@code 공격 <대상>}(command.md {@code ### 공격}): 같은 방에서 몹 → NPC → 플레이어 순으로 찾는다. 몹이면 아직 교전하지 않은
     * 몹과 교전을 시작하고 노린다(교전 중인 몹이면 노리는 상대만 바꾼다). 앞 공격에서 3초가 지났으면 그 순간 한 번 친다
     */
    void attack(OnlinePlayer player, @Nullable ItemQuery query, Instant now, BiConsumer<String, GameMessage> send) {
        String sessionId = player.getSessionId();
        if (query == null) {
            send.accept(sessionId, SystemMessage.error(CombatTexts.ATTACK_NO_ARGUMENT));
            return;
        }
        Long roomId = player.getRoomId();
        Optional<MobInstance> found = ItemNames.select(mobsInRoom(roomId), mob -> mob.getProfile().name(), query);
        if (found.isEmpty()) {
            send.accept(sessionId, SystemMessage.error(notAttackableTarget(player, ItemCommandHandler.inputOf(query))));
            return;
        }
        MobInstance mob = found.get();
        String mobName = mob.getProfile().name();
        Long characterId = player.getCharacterId();
        PlayerCombat combat = playerCombatRepository.find(characterId).orElseGet(() -> new PlayerCombat(characterId));
        boolean engaged = combat.getMobIds().contains(mob.getId());
        if (engaged && combat.currentTarget().orElse(-1L) == mob.getId()) {
            send.accept(sessionId, SystemMessage.error(combatTexts.alreadyFighting(mobName)));
            return;
        }
        if (!engaged) {
            engage(combat, mob, now);
            send.accept(sessionId, SystemMessage.combat(combatTexts.engaged(mobName)));
            notifyOthers(player, combatTexts.engagedNotice(player.getName(), mobName), "engage", send);
        }
        combat.target(mob.getId());
        playerCombatRepository.save(combat);
        mobInstanceRepository.save(mob);
        if (combat.readyAt(now)) {
            playerAct(combat, player, mob, now, now, send);
        }
    }

    /** 몹이 아니면 NPC(2차 {@code 대화} 찾기), 플레이어(이름 전체·앞부분) 순으로 보고 거절 문구를 고른다 */
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

    /** 서로의 교전 상대 목록에 넣는다. 교전하지 않던 쪽은 교전 시작 순번을 받는다(플레이어가 먼저) */
    private void engage(PlayerCombat combat, MobInstance mob, Instant now) {
        if (!combat.isFighting()) {
            combat.recordEngageOrder(engageOrders.incrementAndGet());
        }
        if (!mob.isFighting()) {
            mob.recordEngageOrder(engageOrders.incrementAndGet());
        }
        mob.engage(combat.getCharacterId(), now);
        combat.engage(mob.getId());
    }

    // ---- 교전 상태

    /** 교전 상대가 있는가(이동·종료·막히는 명령 검사, D86) */
    boolean isFighting(Long characterId) {
        return playerCombatRepository.find(characterId).map(PlayerCombat::isFighting).orElse(false);
    }

    /** {@code 상태} 의 전투 줄. 교전하지 않으면 빈 문자열 */
    String statusLine(Long characterId) {
        List<MobInstance> opponents = playerCombatRepository.find(characterId).stream()
                .flatMap(combat -> combat.getMobIds().stream())
                .flatMap(mobId -> mobInstanceRepository.findById(mobId).stream())
                .toList();
        return combatTexts.statusLine(opponents);
    }

    /** 세계에서 빠진다(끊김·종료): 모든 교전을 끝내고 교전 상태를 지운다 */
    void leave(Long characterId, Instant now) {
        endAll(characterId, now);
        playerCombatRepository.delete(characterId);
    }

    /** 다른 방으로 옮겨진다(방 삭제): 모든 교전을 끝낸다. 교전 중이었으면 10초 뒤 다시 차기를 예약한다 */
    void endAll(Long characterId, Instant now) {
        playerCombatRepository.find(characterId).ifPresent(combat -> {
            for (Long mobId : combat.getMobIds()) {
                mobInstanceRepository.findById(mobId).ifPresent(mob -> {
                    mob.disengage(characterId, now);
                    mobInstanceRepository.save(mob);
                });
            }
            combat.endAll(now);
            playerCombatRepository.save(combat);
        });
    }

    /** 세계에 들어왔다: 깎인 기혈이 있으면 10초 뒤 다시 차기를 예약한다 */
    void entered(Long characterId, Instant now) {
        if (characterVitalsEditor.qiBloodLost(characterId) <= 0) {
            return;
        }
        PlayerCombat combat = playerCombatRepository.find(characterId).orElseGet(() -> new PlayerCombat(characterId));
        combat.scheduleRefill(now);
        playerCombatRepository.save(combat);
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

    /** 그 방의 시체(쓰러진 순) */
    List<Corpse> corpsesInRoom(Long roomId) {
        return corpseRepository.findAllInRoom(roomId);
    }

    // ---- 1. 배치 맞추기

    /**
     * 배치가 없어졌거나 서명(방, 몹 유형, 수)이 바뀐 몹을 지우고(교전 중이던 플레이어는 교전에서 뺌, 알림 없음),
     * 배치의 빈 자리에 지금 정의로 몹을 나타나게 한다
     */
    private void reconcile(Map<Long, MobPlacement> placements, Instant now, BiConsumer<String, GameMessage> send) {
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

    // ---- 2. 행동

    /**
     * 행동 시각이 된 플레이어·몹의 행동을 (시각, 교전 시작 순번) 순으로 하나씩 처리한다. 행동마다 처리 직전에 다시 확인해
     * 그 틱에 쓰러진 쪽·교전이 끝난 쪽의 행동은 취소된다. 밀린 행동은 몰아서 하지 않는다(행동자마다 틱에 한 번)
     */
    private void act(Instant now, BiConsumer<String, GameMessage> send, List<Long> defeated) {
        List<Action> due = new ArrayList<>();
        for (PlayerCombat combat : playerCombatRepository.findAll()) {
            if (!combat.isFighting() || !combat.readyAt(now)) continue;
            Instant at = combat.getNextAttackAt() == null ? now : requireNonNull(combat.getNextAttackAt());
            due.add(new Action(at, combat.getEngageOrder(), combat.getCharacterId(), null));
        }
        for (MobInstance mob : mobInstanceRepository.findAll()) {
            Instant at = mob.getNextActionAt();
            if (!mob.isFighting() || at == null || at.isAfter(now)) continue;
            due.add(new Action(at, mob.getEngageOrder(), null, mob.getId()));
        }
        due.sort(Comparator.comparing(Action::at).thenComparingLong(Action::order));
        for (Action action : due) {
            if (action.characterId() != null) {
                playerTurn(action.characterId(), action.at(), now, send);
            } else {
                mobTurn(requireNonNull(action.mobId()), action.at(), now, send, defeated);
            }
        }
    }

    /** 플레이어 차례: 아직 세계에 있고 교전 중이며 칠 몹이 살아 같은 방에 있을 때만 친다 */
    private void playerTurn(Long characterId, Instant at, Instant now, BiConsumer<String, GameMessage> send) {
        Optional<PlayerCombat> found = playerCombatRepository.find(characterId);
        Optional<OnlinePlayer> player = presenceRepository.findByCharacterId(characterId);
        if (found.isEmpty() || player.isEmpty() || !found.get().isFighting()) return;
        PlayerCombat combat = found.get();
        Optional<MobInstance> mob = combat.currentTarget().flatMap(mobInstanceRepository::findById);
        if (mob.isEmpty() || !inCombatTogether(player.get(), mob.get())) return;
        playerAct(combat, player.get(), mob.get(), at, now, send);
    }

    /**
     * 플레이어의 일격: 위력 = 3 + 1.8 × 영력(D86) → 몹이 받음 → {@code 청모랑에게 일격을 날린다. (피해 8)} → 쓰러졌으면 처치.
     * 처치 때 알릴 교전 상대는 받기 전에 읽는다(쓰러지면 몹의 상대 목록이 비므로)
     */
    private void playerAct(PlayerCombat combat, OnlinePlayer player, MobInstance mob, Instant at, Instant now,
                           BiConsumer<String, GameMessage> send) {
        Long characterId = player.getCharacterId();
        double power = Damage.playerPower(cultivationFinder.stats(characterId).spirit());
        Set<Long> opponents = mob.opponentIds();
        MobHit hit = mob.receive(characterId, power, now);
        combat.attackedAt(at, now);
        playerCombatRepository.save(combat);
        mobInstanceRepository.save(mob);
        send.accept(player.getSessionId(), SystemMessage.combat(combatTexts.playerHit(mob.getProfile().name(), hit.damage())));
        if (hit.died()) {
            onMobKilled(mob, opponents, characterId, now, send);
        }
    }

    /**
     * 몹이 쓰러졌다: 처치자 {@code 청모랑을 쓰러뜨렸습니다.}, 교전하던 다른 사람 {@code 청모랑이 쓰러졌다.}, 같은 방의 교전하지 않은
     * 사람 {@code 한백이 청모랑을 쓰러뜨렸다.}(event {@code kill}) → 교전하던 플레이어마다 그 몹과의 교전을 끝낸다.
     * 처치자는 피해가 가장 많은 사람이라 마지막 일격을 한 사람과 다를 수 있다
     */
    private void onMobKilled(MobInstance mob, Set<Long> opponents, Long strikerId, Instant now,
                             BiConsumer<String, GameMessage> send) {
        String mobName = mob.getProfile().name();
        Long killerId = mob.killer().orElse(strikerId);
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
    }

    /** 몹 차례: 살아 교전 중이고, 노리는 플레이어가 세계에 있고 같은 방에서 이 몹과 교전 중일 때만 친다 */
    private void mobTurn(long mobId, Instant at, Instant now, BiConsumer<String, GameMessage> send, List<Long> defeated) {
        Optional<MobInstance> found = mobInstanceRepository.findById(mobId);
        if (found.isEmpty() || !found.get().isFighting()) return;
        MobInstance mob = found.get();
        Optional<OnlinePlayer> target = mob.target().flatMap(presenceRepository::findByCharacterId);
        if (target.isEmpty() || !inCombatTogether(target.get(), mob)) return;
        mobAct(mob, target.get(), at, now, send, defeated);
    }

    /**
     * 몹의 공격: 피해 = 몹 위력 − 육신 × 0.5 → 깎인 기혈을 DB에 씀 → {@code 청모랑이 물어뜯기로 덮친다! (피해 5, 기혈 55/60)}
     * → 기혈이 0 이하면 그 순간 진다
     */
    private void mobAct(MobInstance mob, OnlinePlayer target, Instant at, Instant now, BiConsumer<String, GameMessage> send,
                        List<Long> defeated) {
        Long characterId = target.getCharacterId();
        StatSheet stats = cultivationFinder.stats(characterId);
        double damage = Damage.afterBody(mob.getProfile().stats().power(), stats.body());
        double remaining = characterVitalsEditor.takeDamage(characterId, damage, stats.qiBlood());
        mob.actedAt(at, now);
        mobInstanceRepository.save(mob);
        send.accept(target.getSessionId(),
                SystemMessage.combat(combatTexts.mobHit(mob.getProfile(), damage, remaining, stats.qiBlood())));
        if (remaining <= 0) {
            onPlayerDefeated(target, stats.qiBlood(), now, send);
            defeated.add(characterId);
        }
    }

    /**
     * 졌다(D88 의 CombatEngine 몫): 기혈 1 → 모든 교전 끝(나와 싸우던 몹의 상대 목록에서도 빠짐, 다시 차기 예약) →
     * {@code 기혈이 다했습니다. 눈앞이 캄캄해진다…} 와 같은 방 {@code 한백이 쓰러진다.}(event {@code defeated}).
     * 그 틱의 남은 행동은 교전이 끝나 취소된다. 시작 방으로 옮기기는 {@link GameService} 가 한다
     */
    private void onPlayerDefeated(OnlinePlayer player, int maxQiBlood, Instant now, BiConsumer<String, GameMessage> send) {
        Long characterId = player.getCharacterId();
        characterVitalsEditor.setQiBlood(characterId, 1, maxQiBlood);
        endAll(characterId, now);
        send.accept(player.getSessionId(), SystemMessage.combat(CombatTexts.DEFEATED));
        notifyOthers(player, combatTexts.defeatedNotice(player.getName()), "defeated", send);
    }

    /** 플레이어와 몹이 같은 방에서 서로 교전 중이다 */
    private boolean inCombatTogether(OnlinePlayer player, MobInstance mob) {
        return mob.isAlive() && mob.getRoomId().equals(player.getRoomId())
                && mob.opponentIds().contains(player.getCharacterId())
                && playerCombatRepository.find(player.getCharacterId())
                .map(combat -> combat.getMobIds().contains(mob.getId())).orElse(false);
    }

    /** 같은 방 다른 사람에게 {@code notice}(tone {@code combat}) */
    private void notifyOthers(OnlinePlayer actor, String text, String event, BiConsumer<String, GameMessage> send) {
        NoticeMessage notice = NoticeMessage.combat(text, actor.getName(), event);
        presenceRepository.findAllInRoom(actor.getRoomId()).stream()
                .filter(other -> !other.getCharacterId().equals(actor.getCharacterId()))
                .forEach(other -> send.accept(other.getSessionId(), notice));
    }

    /** 이번 틱에 할 행동 하나: 플레이어(characterId) 또는 몹(mobId) */
    private record Action(Instant at, long order, @Nullable Long characterId, @Nullable Long mobId) {
    }

    // ---- 3. 다시 차기

    /** 교전 상대가 모두 사라진 지 10초가 된 몹은 기혈이 다 차고 위협·피해 기록을 지운다 */
    private void refillMobs(Instant now) {
        for (MobInstance mob : mobInstanceRepository.findAll()) {
            if (mob.isAlive()) {
                mob.refillIfDue(now);
            }
        }
    }

    // ---- 5. 재등장

    /** 재등장 시각이 된 몹을 배치의 지금 정의로 같은 자리에 기혈이 다 찬 채 나타나게 한다 */
    private void respawnMobs(Map<Long, MobPlacement> placements, Instant now, BiConsumer<String, GameMessage> send) {
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

    /** 방 보기 묶음 키 */
    private record GroupKey(Long mobTypeId, boolean fighting) {
        static GroupKey of(MobInstance mob) {
            return new GroupKey(mob.getProfile().mobTypeId(), mob.isFighting());
        }
    }
}
