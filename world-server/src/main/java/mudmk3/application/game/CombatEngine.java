package mudmk3.application.game;

import lombok.RequiredArgsConstructor;
import mudmk3.application.character.provided.CharacterVitalsEditor;
import mudmk3.application.cultivation.provided.CultivationFinder;
import mudmk3.application.game.required.AbilityTimersRepository;
import mudmk3.application.game.required.GameMessage;
import mudmk3.application.game.required.GameMessage.NoticeMessage;
import mudmk3.application.game.required.GameMessage.SystemMessage;
import mudmk3.application.game.required.MobInstanceRepository;
import mudmk3.application.game.required.PlayerCombatRepository;
import mudmk3.application.game.required.PresenceRepository;
import mudmk3.application.mob.provided.MobPlacementFinder;
import mudmk3.domain.combat.AbilityTimers;
import mudmk3.domain.combat.AttackNature;
import mudmk3.domain.combat.Casting;
import mudmk3.domain.combat.Corpse;
import mudmk3.domain.combat.Damage;
import mudmk3.domain.combat.MobHit;
import mudmk3.domain.combat.MobInstance;
import mudmk3.domain.combat.PlayerCombat;
import mudmk3.domain.cultivation.StatSheet;
import mudmk3.domain.game.OnlinePlayer;
import mudmk3.domain.item.ItemQuery;
import mudmk3.domain.mob.MobPlacement;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * 몹·교전·시체 메모리 상태를 다룬다(D85·D86). {@link GameService} 가 게임 락과 트랜잭션 안에서 부른다.
 * 서버 틱은 platform.md {@code #### 서버 틱} 순서대로 처리한다: 1 배치 맞추기({@link MobSpawner}) → 법력 회복({@link ManaRecovery}) →
 * 2 행동(시전 발동 포함) → 3 다시 차기(몹은 {@link MobSpawner}) → 4 시체 사라짐({@link CorpseHandler}) → 5 재등장({@link MobSpawner})
 * → 끝으로 세계에 없는 캐릭터의 다 쓴 교전·대기시간 기록 지우기({@link #sweepOffline}).
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
    private final CorpseHandler corpseHandler;
    private final PresenceRepository presenceRepository;
    private final CultivationFinder cultivationFinder;
    private final CharacterVitalsEditor characterVitalsEditor;
    private final CombatTexts combatTexts;
    private final ManaRecovery manaRecovery;
    private final CombatActions combatActions;
    private final AbilityTimersRepository abilityTimersRepository;
    private final AbilityCaster abilityCaster;
    private final MobSpawner mobSpawner;
    private final RoomNotifier roomNotifier;

    /** 틱 하나. send 는 (연결 id, 메시지)로 보낼 것을 모은다. 이번 틱에 진 캐릭터를 돌려준다 */
    CombatOutcome tick(Instant now, BiConsumer<String, GameMessage> send) {
        Map<Long, MobPlacement> placements = mobPlacementFinder.findAll().stream()
                .collect(Collectors.toMap(placement -> requireNonNull(placement.getId()), Function.identity(),
                        (a, b) -> a, LinkedHashMap::new));
        mobSpawner.reconcile(placements, now, send);
        manaRecovery.recover(now);
        List<Long> defeated = new ArrayList<>();
        act(now, send, defeated);
        mobSpawner.refill(now);
        refillPlayers(now, send);
        corpseHandler.removeExpired(now);
        mobSpawner.respawn(placements, now, send);
        sweepOffline(now);
        return new CombatOutcome(defeated);
    }

    /**
     * 틱 끝 정리(L-D4): 세계에 없는 캐릭터의 교전 기록은 교전 상대·다시 차기 예약이 없고 다음 공격 시각이 지나면, 대기시간
     * 기록은 시전이 없고 공통·재사용 대기시간이 모두 끝나면 지운다. 다시 들어오면 새 기록과 같다
     */
    private void sweepOffline(Instant now) {
        for (PlayerCombat combat : playerCombatRepository.findAll()) {
            if (offline(combat.getCharacterId()) && combat.isIdle(now)) {
                playerCombatRepository.delete(combat.getCharacterId());
            }
        }
        for (AbilityTimers timers : abilityTimersRepository.findAll()) {
            if (offline(timers.getCharacterId()) && timers.isIdle(now)) {
                abilityTimersRepository.delete(timers.getCharacterId());
            }
        }
    }

    private boolean offline(Long characterId) {
        return presenceRepository.findByCharacterId(characterId).isEmpty();
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
        MobInstance mob;
        switch (combatActions.findTarget(player, query)) {
            case TargetLookup.Rejected rejected -> {
                send.accept(sessionId, SystemMessage.error(rejected.text()));
                return;
            }
            case TargetLookup.Found found -> mob = found.mob();
        }
        Long characterId = player.getCharacterId();
        PlayerCombat combat = playerCombatRepository.find(characterId).orElseGet(() -> new PlayerCombat(characterId));
        boolean engaged = combat.getMobIds().contains(mob.getId());
        if (engaged && combat.currentTarget().orElse(-1L) == mob.getId()) {
            send.accept(sessionId, SystemMessage.error(combatTexts.alreadyFighting(mob.getProfile().name())));
            return;
        }
        if (!engaged) {
            combatActions.startEngagement(player, combat, mob, now, send);
        }
        combat.target(mob.getId());
        playerCombatRepository.save(combat);
        mobInstanceRepository.save(mob);
        if (combat.readyAt(now)) {
            playerAct(combat, player, mob, now, now, send);
        }
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

    /**
     * 세계에서 빠진다(끊김·종료): 시전을 지우고(D113, 대기시간은 남음) 모든 교전을 끝낸다. 다음 공격 시각은 남겨 다시 접속해도
     * 공격 간격이 줄지 않게 하고(D84), 다시 차기 예약은 지운다(세계 밖에서는 차지 않음 — 들어올 때 {@link #entered} 가 다시 예약)
     */
    void leave(Long characterId, Instant now) {
        abilityTimersRepository.clearCasting(characterId, casting -> true);
        endAll(characterId, now);
        playerCombatRepository.find(characterId).ifPresent(combat -> {
            combat.clearRefill();
            playerCombatRepository.save(combat);
        });
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

    /**
     * 방이 지워졌다: 그 방의 시체와 몹(쓰러진 것 포함)을 지운다(알리지 않음). 그 방 사람은 먼저 {@link #endAll} 로 교전을
     * 끝냈고, 다른 방 사람은 그 몹과 교전 중일 수 없다
     */
    void forgetRoom(Long roomId) {
        corpseHandler.removeInRoom(roomId);
        mobInstanceRepository.deleteAllInRoom(roomId);
    }

    /**
     * 세계에 들어왔다: 법력 회복 기준 시각을 지금으로 두고(끊긴 동안은 차지 않음), 깎인 기혈이 있으면 10초 뒤 다시 차기를 예약한다
     */
    void entered(Long characterId, Instant now) {
        PlayerCombat combat = playerCombatRepository.find(characterId).orElseGet(() -> new PlayerCombat(characterId));
        combat.startManaRegen(now);
        if (characterVitalsEditor.qiBloodLost(characterId) > 0) {
            combat.scheduleRefill(now);
        }
        playerCombatRepository.save(combat);
    }

    /** 그 방의 살아 있는 몹(방 보기 순서, {@link CombatActions#mobsInRoom}) */
    List<MobInstance> mobsInRoom(Long roomId) {
        return combatActions.mobsInRoom(roomId);
    }

    /** 그 방의 시체(쓰러진 순) */
    List<Corpse> corpsesInRoom(Long roomId) {
        return corpseHandler.inRoom(roomId);
    }

    // ---- 2. 행동

    /** 정렬 무리: 교전 중인 행동 */
    private static final int ENGAGED_GROUP = 0;
    /** 정렬 무리: 아직 교전하지 않은 캐릭터의 시전 */
    private static final int NOT_ENGAGED_GROUP = 1;
    /** 정렬 종류: 자동 공격(플레이어·몹)이 시전보다 먼저 */
    private static final int AUTO_ATTACK = 0;
    private static final int CASTING = 1;

    /** 이번 틱에 할 행동 하나: 플레이어(characterId)·몹(mobId)의 자동 공격, 또는 캐릭터의 시전 발동(casting) */
    private record Action(Instant at, int group, long order, int kind, @Nullable Long characterId, @Nullable Long mobId,
                          @Nullable Casting casting) {
    }

    /**
     * 행동 시각이 된 플레이어·몹의 행동과 발동 시각이 된 시전을 정렬 키 (시각, 무리, 순번, 종류)로 하나씩 처리한다(D113, F20).
     * 교전 중인 행동(플레이어·몹의 자동 공격, 이미 교전 중인 캐릭터의 시전)은 무리 0·교전 시작 순번, 아직 교전하지 않은 캐릭터의
     * 시전은 무리 1·시전 시작 순번이다. 넷째 키는 자동 공격이 시전보다 먼저다. 행동마다 처리 직전에 다시 확인해 그 틱에 쓰러진
     * 쪽·교전이 끝난 쪽의 행동은 취소된다. 밀린 행동은 몰아서 하지 않는다(행동자마다 틱에 한 번)
     */
    private void act(Instant now, BiConsumer<String, GameMessage> send, List<Long> defeated) {
        List<Action> due = new ArrayList<>();
        for (PlayerCombat combat : playerCombatRepository.findAll()) {
            if (!combat.isFighting() || !combat.readyAt(now)) continue;
            Instant at = combat.getNextAttackAt() == null ? now : requireNonNull(combat.getNextAttackAt());
            due.add(new Action(at, ENGAGED_GROUP, combat.getEngageOrder(), AUTO_ATTACK, combat.getCharacterId(), null, null));
        }
        for (MobInstance mob : mobInstanceRepository.findAll()) {
            Instant at = mob.getNextActionAt();
            if (!mob.isFighting() || at == null || at.isAfter(now)) continue;
            due.add(new Action(at, ENGAGED_GROUP, mob.getEngageOrder(), AUTO_ATTACK, null, mob.getId(), null));
        }
        for (AbilityTimers timers : abilityTimersRepository.findAll()) {
            Optional<Casting> casting = timers.casting().filter(c -> !c.firesAt().isAfter(now));
            if (casting.isEmpty()) continue;
            Long characterId = timers.getCharacterId();
            Optional<PlayerCombat> combat = playerCombatRepository.find(characterId).filter(PlayerCombat::isFighting);
            due.add(combat.isPresent()
                    ? new Action(casting.get().firesAt(), ENGAGED_GROUP, combat.get().getEngageOrder(), CASTING,
                    characterId, null, casting.get())
                    : new Action(casting.get().firesAt(), NOT_ENGAGED_GROUP, casting.get().seq(), CASTING,
                    characterId, null, casting.get()));
        }
        due.sort(Comparator.comparing(Action::at).thenComparingInt(Action::group).thenComparingLong(Action::order)
                .thenComparingInt(Action::kind));
        for (Action action : due) {
            Casting casting = action.casting();
            if (casting != null) {
                abilityCaster.fire(requireNonNull(action.characterId()), casting, now, send);
            } else if (action.characterId() != null) {
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
        if (mob.isEmpty() || !combatActions.inCombatTogether(player.get(), mob.get())) return;
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
        MobHit hit = combatActions.strike(characterId, mob, power, AttackNature.NONE, now);
        combat.attackedAt(at, now);
        playerCombatRepository.save(combat);
        send.accept(player.getSessionId(), SystemMessage.combat(combatTexts.playerHit(mob.getProfile().name(), hit.damage())));
        if (hit.died()) {
            combatActions.onMobKilled(mob, opponents, characterId, now, send);
        }
    }

    /** 몹 차례: 살아 교전 중이고, 노리는 플레이어가 세계에 있고 같은 방에서 이 몹과 교전 중일 때만 친다 */
    private void mobTurn(long mobId, Instant at, Instant now, BiConsumer<String, GameMessage> send, List<Long> defeated) {
        Optional<MobInstance> found = mobInstanceRepository.findById(mobId);
        if (found.isEmpty() || !found.get().isFighting()) return;
        MobInstance mob = found.get();
        Optional<OnlinePlayer> target = mob.target().flatMap(presenceRepository::findByCharacterId);
        if (target.isEmpty() || !combatActions.inCombatTogether(target.get(), mob)) return;
        mobAct(mob, target.get(), at, now, send, defeated);
    }

    /**
     * 몹의 공격: 피해 = 몹 위력 × 받는 쪽 배율(6차 1) − 육신 × 0.5 → 깎인 기혈을 DB에 씀 → {@code 청모랑이 물어뜯기로 덮친다! (피해 5, 기혈 55/60)}
     * → 기혈이 0 이하면 그 순간 진다
     */
    private void mobAct(MobInstance mob, OnlinePlayer target, Instant at, Instant now, BiConsumer<String, GameMessage> send,
                        List<Long> defeated) {
        Long characterId = target.getCharacterId();
        StatSheet stats = cultivationFinder.stats(characterId);
        double damage = Damage.afterLayers(mob.getProfile().stats().power(),
                Damage.receiverMultiplier(AttackNature.NONE, null), stats.body());
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
     * 졌다(D88 의 CombatEngine 몫): 기혈 1 → 시전을 지움(D113) → 모든 교전 끝(나와 싸우던 몹의 상대 목록에서도 빠짐, 다시 차기 예약) →
     * {@code 기혈이 다했습니다. 눈앞이 캄캄해진다…} 와 같은 방 {@code 한백이 쓰러진다.}(event {@code defeated}).
     * 그 틱의 남은 행동은 교전이 끝나 취소된다. 시작 방으로 옮기기는 {@link GameService} 가 한다
     */
    private void onPlayerDefeated(OnlinePlayer player, int maxQiBlood, Instant now, BiConsumer<String, GameMessage> send) {
        Long characterId = player.getCharacterId();
        characterVitalsEditor.setQiBlood(characterId, 1, maxQiBlood);
        abilityTimersRepository.clearCasting(characterId, casting -> true);
        endAll(characterId, now);
        send.accept(player.getSessionId(), SystemMessage.combat(CombatTexts.DEFEATED));
        roomNotifier.notifyOthers(player,
                NoticeMessage.combat(combatTexts.defeatedNotice(player.getName()), player.getName(), "defeated"), send);
    }

    // ---- 3. 플레이어 다시 차기(몹은 MobSpawner.refill)

    /**
     * 교전 상대가 모두 사라진 지 10초가 된 플레이어는 기혈이 다 찬다(command.md {@code ### 교전 뒤 기혈}). 깎인 것이 있었을 때만
     * {@code 숨을 고르자 기운이 돌아온다. (기혈 60/60)}. 세계에 없으면 채우지 않는다(들어올 때 다시 예약)
     */
    private void refillPlayers(Instant now, BiConsumer<String, GameMessage> send) {
        for (PlayerCombat combat : playerCombatRepository.findAll()) {
            if (!combat.refillDue(now)) continue;
            combat.clearRefill();
            playerCombatRepository.save(combat);
            Long characterId = combat.getCharacterId();
            Optional<OnlinePlayer> player = presenceRepository.findByCharacterId(characterId);
            if (player.isEmpty() || !characterVitalsEditor.restoreQiBlood(characterId)) continue;
            int max = cultivationFinder.stats(characterId).qiBlood();
            send.accept(player.get().getSessionId(), SystemMessage.info(combatTexts.refilled(max)));
        }
    }
}
