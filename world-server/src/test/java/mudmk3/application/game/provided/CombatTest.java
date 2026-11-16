package mudmk3.application.game.provided;

import jakarta.persistence.EntityManager;
import mudmk3.ApplicationServiceTest;
import mudmk3.MutableClock;
import mudmk3.application.account.provided.AccountLogin;
import mudmk3.application.character.provided.CharacterCreator;
import mudmk3.application.character.provided.CharacterFinder;
import mudmk3.application.character.provided.CharacterMover;
import mudmk3.application.character.provided.CharacterRealmEditor;
import mudmk3.application.character.provided.CharacterVitalsEditor;
import mudmk3.application.character.required.CharacterRepository;
import mudmk3.application.faction.provided.FactionEditor;
import mudmk3.application.floor.provided.FloorPlacementEditor;
import mudmk3.application.game.GameService;
import mudmk3.application.game.RecordingGameMessageSender;
import mudmk3.application.game.required.GameMessage.NoticeMessage;
import mudmk3.application.game.required.GameMessage.RoomMessage;
import mudmk3.application.game.required.GameMessage.StatusMessage;
import mudmk3.application.game.required.GameMessage.SystemMessage;
import mudmk3.application.game.required.MobInstanceRepository;
import mudmk3.application.game.required.PlayerCombatRepository;
import mudmk3.application.inventory.provided.InventoryEditor;
import mudmk3.application.inventory.provided.InventoryFinder;
import mudmk3.application.item.provided.ItemTypeEditor;
import mudmk3.application.item.provided.ItemTypeFinder;
import mudmk3.application.mob.provided.MobPlacementEditor;
import mudmk3.application.mob.provided.MobTypeEditor;
import mudmk3.application.npc.provided.NpcEditor;
import mudmk3.application.room.provided.RoomEditor;
import mudmk3.application.world.provided.WorldSettingsEditor;
import mudmk3.application.zone.provided.ZoneEditor;
import mudmk3.domain.combat.MobInstance;
import mudmk3.domain.combat.PlayerCombat;
import mudmk3.domain.mob.MobKind;
import mudmk3.domain.mob.MobTier;
import mudmk3.domain.mob.MobTypeSaveInfo;
import mudmk3.domain.shared.Realm;
import org.junit.jupiter.api.Test;

import java.util.List;

import static mudmk3.domain.mob.MobFixture.boss;
import static mudmk3.domain.mob.MobFixture.info;
import static mudmk3.domain.mob.MobFixture.snake;
import static mudmk3.domain.mob.MobFixture.wolf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/** {@code 공격} 과 틱의 교전 처리(D86): 행동 순서·취소·밀림, 위협, 처치 줄, 끊김, 상태 전투 줄 */
@ApplicationServiceTest
class CombatTest extends BaseGameTest {

    private static final String WOLF_HITS_FIRST = "청모랑이 물어뜯기로 덮친다! (피해 5, 기혈 55/60)";
    private static final String HIT_WOLF = "청모랑에게 일격을 날린다. (피해 8)";

    private final MobInstanceRepository mobInstanceRepository;
    private final PlayerCombatRepository playerCombatRepository;
    private final CharacterVitalsEditor characterVitalsEditor;

    CombatTest(
            GameConnector gameConnector,
            GameService gameService,
            RecordingGameMessageSender sender,
            AccountLogin accountLogin,
            CharacterCreator characterCreator,
            CharacterFinder characterFinder,
            CharacterMover characterMover,
            CharacterRepository characterRepository,
            CharacterRealmEditor characterRealmEditor,
            ZoneEditor zoneEditor,
            RoomEditor roomEditor,
            WorldSettingsEditor worldSettingsEditor,
            FactionEditor factionEditor,
            NpcEditor npcEditor,
            ItemTypeEditor itemTypeEditor,
            ItemTypeFinder itemTypeFinder,
            FloorPlacementEditor floorPlacementEditor,
            InventoryFinder inventoryFinder,
            InventoryEditor inventoryEditor,
            MobTypeEditor mobTypeEditor,
            MobPlacementEditor mobPlacementEditor,
            MutableClock clock,
            EntityManager entityManager,
            MobInstanceRepository mobInstanceRepository,
            PlayerCombatRepository playerCombatRepository,
            CharacterVitalsEditor characterVitalsEditor
    ) {
        super(gameConnector, gameService, sender, accountLogin, characterCreator, characterFinder, characterMover,
                characterRepository, characterRealmEditor, zoneEditor, roomEditor, worldSettingsEditor, factionEditor, npcEditor,
                itemTypeEditor, itemTypeFinder, floorPlacementEditor, inventoryFinder, inventoryEditor, mobTypeEditor,
                mobPlacementEditor, clock, entityManager);
        this.mobInstanceRepository = mobInstanceRepository;
        this.playerCombatRepository = playerCombatRepository;
        this.characterVitalsEditor = characterVitalsEditor;
    }

    // ---- 대상 찾기

    @Test
    void attackArgumentMissing() {
        GameMap map = prepareMap();
        enterAs("a", "한백", Realm.QI_1);
        placeMob(map, wolf(), 1);
        sender.clear();

        gameConnector.handle("a", "공격");

        assertThat(sender.messagesTo("a"))
                .containsExactly(SystemMessage.error("누구를 공격하시겠습니까? (예: 공격 늑대)"));
    }

    @Test
    void attackNotHere() {
        GameMap map = prepareMap();
        enterAs("a", "한백", Realm.QI_1);
        placeMob(map, wolf(), 1);
        sender.clear();

        gameConnector.handle("a", "공격 곰");
        gameConnector.handle("a", "공격 2.청모랑");

        assertThat(sender.messagesTo("a")).containsExactly(
                SystemMessage.error("이곳에 '곰'은 없습니다."),
                SystemMessage.error("이곳에 '2.청모랑'은 없습니다."));
    }

    @Test
    void attackNpcFail() {
        GameMap map = prepareMap();
        prepareNpcs(map);
        enterAs("a", "한백", Realm.QI_1);
        sender.clear();

        gameConnector.handle("a", "공격 왕씨");
        gameConnector.handle("a", "공격 왕");

        assertThat(sender.messagesTo("a")).containsExactly(
                SystemMessage.error("왕씨는 공격할 수 있는 상대가 아닙니다."),
                SystemMessage.error("왕씨는 공격할 수 있는 상대가 아닙니다."));
    }

    @Test
    void attackPlayerFail() {
        prepareMap();
        enterAs("a", "한백", Realm.QI_1);
        enter("b", "한립");
        sender.clear();

        gameConnector.handle("a", "공격 한립");
        gameConnector.handle("a", "공격 한");

        assertThat(sender.messagesTo("a")).containsExactly(
                SystemMessage.error("다른 사람과는 아직 싸울 수 없습니다."),
                SystemMessage.error("다른 사람과는 아직 싸울 수 없습니다."));
    }

    @Test
    void attackPrefersMobOverNpc() {
        GameMap map = prepareMap();
        prepareNpcs(map);
        enterAs("a", "한백", Realm.QI_1);
        placeMob(map, info("왕두꺼비", MobKind.BEAST, Realm.QI_2, MobTier.NORMAL, "혀 치기", null, null), 1);
        sender.clear();

        gameConnector.handle("a", "공격 왕");

        assertThat(combatLines("a")).first().isEqualTo("왕두꺼비와 싸움이 벌어진다.");
    }

    // ---- 교전 시작과 행동 시각

    @Test
    void attackStartsAndHitsImmediately() {
        GameMap map = prepareMap();
        enterAs("a", "한백", Realm.QI_1);
        enter("b", "한립");
        placeMob(map, wolf(), 1);
        sender.clear();

        gameConnector.handle("a", "공격 청모랑");

        assertThat(sender.messagesTo("a")).containsExactly(
                SystemMessage.combat("청모랑과 싸움이 벌어진다."),
                SystemMessage.combat(HIT_WOLF));
        assertThat(sender.messagesTo("b"))
                .containsExactly(NoticeMessage.combat("한백이 청모랑과 싸우기 시작한다.", "한백", "engage"));
        MobInstance mob = onlyMob(map.entranceId());
        assertThat(mob.getQiBlood()).isEqualTo(41);
        assertThat(mob.isFighting()).isTrue();
    }

    @Test
    void mobHitsAfterOnePointFive() {
        GameMap map = prepareMap();
        Long hanbaek = enterAs("a", "한백", Realm.QI_1);
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "공격 청모랑");
        sender.clear();

        advance(1);
        tick();
        assertThat(sender.messagesTo("a")).isEmpty();

        advance(1);
        tick();
        assertThat(sender.messagesTo("a")).containsExactly(SystemMessage.combat(WOLF_HITS_FIRST));
        assertThat(characterVitalsEditor.qiBloodLost(characterIdOf(hanbaek))).isEqualTo(5);
    }

    @Test
    void playerAutoAttacksEveryThreeSeconds() {
        GameMap map = prepareMap();
        enterAs("a", "한백", Realm.QI_1);
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "공격 청모랑");
        sender.clear();

        advanceAndTick(2.5);
        assertThat(combatLines("a")).containsExactly(WOLF_HITS_FIRST);

        advanceAndTick(0.5);
        assertThat(combatLines("a")).containsExactly(WOLF_HITS_FIRST, HIT_WOLF);

        advanceAndTick(3);
        assertThat(combatLines("a")).filteredOn(HIT_WOLF::equals).hasSize(2);
        assertThat(onlyMob(map.entranceId()).getQiBlood()).isEqualTo(25);
    }

    @Test
    void attackAgainSameTarget() {
        GameMap map = prepareMap();
        enterAs("a", "한백", Realm.QI_1);
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "공격 청모랑");
        sender.clear();

        gameConnector.handle("a", "공격 청모랑");

        assertThat(sender.messagesTo("a")).containsExactly(SystemMessage.error("이미 청모랑과 싸우고 있습니다."));
    }

    @Test
    void attackSecondMobSwitchesTargetWithoutExtraHit() {
        GameMap map = prepareMap();
        Long hanbaek = enterAs("a", "한백", Realm.QI_1);
        placeMob(map, wolf(), 2);
        gameConnector.handle("a", "공격 청모랑");
        sender.clear();

        // 교전 중인 첫 몹이 방 보기 앞줄이라 교전하지 않은 둘째가 2.청모랑
        gameConnector.handle("a", "공격 2.청모랑");

        assertThat(sender.messagesTo("a")).containsExactly(SystemMessage.combat("청모랑과 싸움이 벌어진다."));
        List<MobInstance> wolves = mobInstanceRepository.findAllInRoom(map.entranceId());
        assertThat(wolves).extracting(MobInstance::getQiBlood).containsExactly(41.0, 49.0);
        assertThat(wolves).allMatch(MobInstance::isFighting);

        sender.clear();
        advanceAndTick(3);

        assertThat(combatLines("a")).filteredOn(HIT_WOLF::equals).hasSize(1);
        assertThat(mobInstanceRepository.findAllInRoom(map.entranceId()))
                .extracting(MobInstance::getQiBlood).containsExactly(41.0, 41.0);
        assertThat(playerCombatRepository.find(characterIdOf(hanbaek)).orElseThrow().getMobIds())
                .containsExactly(wolves.get(0).getId(), wolves.get(1).getId());
    }

    @Test
    void attackEngagedMobSwitchesTargetSilently() {
        GameMap map = prepareMap();
        enterAs("a", "한백", Realm.QI_1);
        placeMob(map, wolf(), 2);
        gameConnector.handle("a", "공격 청모랑");
        gameConnector.handle("a", "공격 2.청모랑");
        sender.clear();

        gameConnector.handle("a", "공격 청모랑");

        assertThat(sender.messagesTo("a")).isEmpty();
        advanceAndTick(3);
        assertThat(mobInstanceRepository.findAllInRoom(map.entranceId()))
                .extracting(MobInstance::getQiBlood).containsExactly(33.0, 49.0);
    }

    @Test
    void attackOnCooldownWaits() {
        GameMap map = prepareMap();
        enterAs("a", "한백", Realm.FOUNDATION_EARLY);
        placeMob(map, wolf(), 2);
        gameConnector.handle("a", "공격 청모랑");
        assertThat(combatLines("a")).containsExactly(
                "청모랑과 싸움이 벌어진다.", "청모랑에게 일격을 날린다. (피해 60)", "청모랑을 쓰러뜨렸습니다.");
        sender.clear();

        gameConnector.handle("a", "공격 청모랑");
        assertThat(combatLines("a")).containsExactly("청모랑과 싸움이 벌어진다.");

        advanceAndTick(2.5);
        assertThat(combatLines("a")).doesNotContain("청모랑에게 일격을 날린다. (피해 60)");

        advanceAndTick(0.5);
        assertThat(combatLines("a")).endsWith("청모랑에게 일격을 날린다. (피해 60)", "청모랑을 쓰러뜨렸습니다.");
    }

    // ---- 처치

    @Test
    void killEndsCombat() {
        GameMap map = prepareMap();
        Long hanbaek = enterAs("a", "한백", Realm.QI_1);
        enter("b", "한립");
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "공격 청모랑");
        sender.clear();

        advanceAndTick(18);

        assertThat(combatLines("a")).filteredOn(HIT_WOLF::equals).hasSize(6);
        assertThat(combatLines("a")).last().isEqualTo("청모랑을 쓰러뜨렸습니다.");
        assertThat(sender.messagesTo("b"))
                .containsExactly(NoticeMessage.combat("한백이 청모랑을 쓰러뜨렸다.", "한백", "kill"));
        assertThat(mobInstanceRepository.findAllInRoom(map.entranceId())).isEmpty();
        assertThat(playerCombatRepository.find(characterIdOf(hanbaek)).orElseThrow().isFighting()).isFalse();

        sender.clear();
        gameConnector.handle("a", "북");
        assertThat(sender.lastTo("a", RoomMessage.class).room().id()).isEqualTo(map.wellId());
    }

    @Test
    void coFighterSeesFell() {
        GameMap map = prepareMap();
        enterAs("a", "한백", Realm.QI_1);
        Long hanlip = enterAs("b", "한립", Realm.QI_1);
        enter("c", "여소");
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "공격 청모랑");
        gameConnector.handle("b", "공격 청모랑");
        sender.clear();

        // 둘이 3초마다 8씩: 0초 33, 3초 17, 6초 1, 9초에 먼저 교전한 한백이 먼저 쳐 쓰러뜨림(피해 48 > 36)
        advanceAndTick(9);

        assertThat(combatLines("a")).last().isEqualTo("청모랑을 쓰러뜨렸습니다.");
        assertThat(combatLines("b")).last().isEqualTo("청모랑이 쓰러졌다.");
        assertThat(combatLines("b")).filteredOn(HIT_WOLF::equals).hasSize(2);
        assertThat(sender.messagesTo("c"))
                .containsExactly(NoticeMessage.combat("한백이 청모랑을 쓰러뜨렸다.", "한백", "kill"));
        assertThat(playerCombatRepository.find(characterIdOf(hanlip)).orElseThrow().isFighting()).isFalse();
    }

    /** 처치자는 피해가 가장 많은 사람이다: 마지막 일격이 아니어도, 이미 세계를 떠났어도 바뀌지 않는다(룰북 처치 보상 귀속) */
    @Test
    void killerKeptWhenTopDealerLeft() {
        GameMap map = prepareMap();
        enterAs("a", "한백", Realm.QI_13);
        enterAs("b", "한립", Realm.QI_1);
        enter("c", "여소");
        placeMob(map, snake(), 1); // 기혈 80, 육신 10
        gameConnector.handle("a", "공격 적안 독사"); // 28.6
        gameConnector.handle("b", "공격 적안 독사"); // 7 → 44.4
        advanceAndTick(3); // 한백 28.6, 한립 7 → 8.8 (위력 합 한백 67.2)
        gameConnector.disconnect("a");
        sender.clear();

        advanceAndTick(6); // 한립 6초 7 → 1.8, 9초 7 → 쓰러짐 (위력 합 한립 48)

        assertThat(combatLines("b")).endsWith("적안 독사에게 일격을 날린다. (피해 7)", "적안 독사가 쓰러졌다.");
        assertThat(sender.messagesTo("c"))
                .containsExactly(NoticeMessage.combat("한백이 적안 독사를 쓰러뜨렸다.", "한백", "kill"));
    }

    // ---- 위협과 대상

    @Test
    void mobSwitchesToRemainingPlayer() {
        GameMap map = prepareMap();
        Long hanbaek = enterAs("a", "한백", Realm.QI_1);
        enterAs("b", "한립", Realm.QI_1);
        enter("c", "여소");
        placeMob(map, boss(), 1);
        characterVitalsEditor.setQiBlood(characterIdOf(hanbaek), 10, 60);
        gameConnector.handle("a", "공격 쌍두"); // 위협 1 + 12
        gameConnector.handle("b", "공격 쌍두"); // 위협 12 — 1.1배를 넘지 않아 한백을 노림
        sender.clear();

        advanceAndTick(1.5);

        assertThat(combatLines("a")).containsExactly(
                "쌍두 흑망이 두 머리 물기로 덮친다! (피해 16, 기혈 0/60)",
                "기혈이 다했습니다. 눈앞이 캄캄해진다…",
                "마을 어귀에서 깨어났습니다. (기혈 1/60)");
        assertThat(sender.messagesTo("b")).containsExactly(NoticeMessage.combat("한백이 쓰러진다.", "한백", "defeated"));
        assertThat(sender.messagesTo("c")).containsExactly(NoticeMessage.combat("한백이 쓰러진다.", "한백", "defeated"));
        assertThat(characterVitalsEditor.qiBloodLost(characterIdOf(hanbaek))).isEqualTo(59);
        assertThat(playerCombatRepository.find(characterIdOf(hanbaek)).orElseThrow().isFighting()).isFalse();
        assertThat(onlyMob(map.entranceId()).opponentIds()).doesNotContain(characterIdOf(hanbaek));

        sender.clear();
        advanceAndTick(3);

        assertThat(combatLines("b")).contains("쌍두 흑망이 두 머리 물기로 덮친다! (피해 16, 기혈 44/60)");
        assertThat(combatLines("a")).isEmpty();
    }

    @Test
    void disconnectEndsEngagement() {
        GameMap map = prepareMap();
        Long hanbaek = enterAs("a", "한백", Realm.QI_1);
        Long hanlip = enterAs("b", "한립", Realm.QI_1);
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "공격 청모랑");
        gameConnector.handle("b", "공격 청모랑");
        sender.clear();

        gameConnector.disconnect("a");

        PlayerCombat left = playerCombatRepository.find(characterIdOf(hanbaek)).orElseThrow();
        assertThat(left.isFighting()).isFalse();
        assertThat(left.getRefillAt()).isNull();
        assertThat(onlyMob(map.entranceId()).opponentIds()).containsExactly(characterIdOf(hanlip));

        advanceAndTick(1.5);
        assertThat(combatLines("b")).containsExactly(WOLF_HITS_FIRST);

        gameConnector.disconnect("b");
        MobInstance mob = onlyMob(map.entranceId());
        assertThat(mob.isFighting()).isFalse();
        assertThat(mob.getQiBlood()).isEqualTo(33);

        advanceAndTick(9.5);
        assertThat(onlyMob(map.entranceId()).getQiBlood()).isEqualTo(33);
        advanceAndTick(0.5);
        assertThat(onlyMob(map.entranceId()).getQiBlood()).isEqualTo(49);
    }

    /** 끊고 바로 다시 접속해도 공격 간격은 줄지 않는다: 처음 일격(0초) 뒤 3초 전에는 다시 치지 않는다 */
    @Test
    void reconnectKeepsAttackCooldown() {
        GameMap map = prepareMap();
        Long hanbaek = enterAs("a", "한백", Realm.QI_1);
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "공격 청모랑");
        advance(1);
        gameConnector.disconnect("a");
        connect("a2", hanbaek);
        sender.clear();

        gameConnector.handle("a2", "공격 청모랑");

        assertThat(combatLines("a2")).containsExactly("청모랑과 싸움이 벌어진다.");
        assertThat(onlyMob(map.entranceId()).getQiBlood()).isEqualTo(41);

        advanceAndTick(1.5); // 2.5초: 아직 아님(몹은 새 교전이라 1.5초 뒤 침)
        assertThat(combatLines("a2")).doesNotContain(HIT_WOLF);

        advanceAndTick(0.5); // 3초: 처음 일격의 다음 시각
        assertThat(combatLines("a2")).contains(HIT_WOLF);
        assertThat(onlyMob(map.entranceId()).getQiBlood()).isEqualTo(33);
    }

    /** 끊긴 캐릭터의 교전 기록은 다음 공격 시각(3초)이 지난 틱에 지운다(L-D4) */
    @Test
    void idleOfflineCombatRecordRemoved() {
        GameMap map = prepareMap();
        Long hanbaek = enterAs("a", "한백", Realm.QI_1);
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "공격 청모랑");
        advance(1);
        gameConnector.disconnect("a");

        advanceAndTick(1.5); // 2.5초: 다음 공격 시각 전
        assertThat(playerCombatRepository.find(characterIdOf(hanbaek))).isPresent();

        advanceAndTick(0.5); // 3초
        assertThat(playerCombatRepository.find(characterIdOf(hanbaek))).isEmpty();
    }

    /** 다음 공격 시각 전의 틱은 기록을 남겨, 다시 접속해 곧바로 쳐도 간격이 줄지 않는다(D84) */
    @Test
    void offlineRecordKeptUntilAttackReady() {
        GameMap map = prepareMap();
        Long hanbaek = enterAs("a", "한백", Realm.QI_1);
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "공격 청모랑");
        advance(1);
        gameConnector.disconnect("a");
        advanceAndTick(1); // 2초
        assertThat(playerCombatRepository.find(characterIdOf(hanbaek))).isPresent();
        connect("a2", hanbaek);
        sender.clear();

        gameConnector.handle("a2", "공격 청모랑");

        assertThat(combatLines("a2")).containsExactly("청모랑과 싸움이 벌어진다.");
        assertThat(onlyMob(map.entranceId()).getQiBlood()).isEqualTo(41);

        advanceAndTick(1); // 3초: 처음 일격의 다음 시각
        assertThat(combatLines("a2")).contains(HIT_WOLF);
        assertThat(onlyMob(map.entranceId()).getQiBlood()).isEqualTo(33);
    }

    @Test
    void placementRemovedMidCombatEndsCombat() {
        GameMap map = prepareMap();
        Long hanbaek = enterAs("a", "한백", Realm.QI_1);
        PlacedMob placed = placeMob(map, wolf(), 1);
        gameConnector.handle("a", "공격 청모랑");

        mobPlacementEditor.delete(placed.placementId());
        tick();
        sender.clear();

        assertThat(playerCombatRepository.find(characterIdOf(hanbaek)).orElseThrow().isFighting()).isFalse();
        gameConnector.handle("a", "북");
        assertThat(sender.lastTo("a", RoomMessage.class).room().id()).isEqualTo(map.wellId());
    }

    /** 테스트 트랜잭션은 커밋되지 않아 삭제 반영(AFTER_COMMIT)이 오지 않으므로 다음 명령이 옮기기를 한다(4차 관례) */
    @Test
    void roomDeletedMidCombatRelocatesAndEnds() {
        GameMap map = prepareMap();
        Long hanbaek = enterAt("a", "한백", map.wellId());
        setRealm(hanbaek, Realm.QI_1);
        placeMob(map.wellId(), wolf(), 1);
        gameConnector.handle("a", "공격 청모랑");
        MobInstance mob = onlyMob(map.wellId());
        roomEditor.delete(map.wellId());
        sender.clear();

        gameConnector.handle("a", "보기");

        assertThat(sender.messagesTo("a").getFirst())
                .isEqualTo(SystemMessage.info("서 있던 곳이 사라져 시작 장소로 옮겨졌습니다."));
        PlayerCombat combat = playerCombatRepository.find(characterIdOf(hanbaek)).orElseThrow();
        assertThat(combat.isFighting()).isFalse();
        assertThat(combat.getRefillAt()).isNotNull();
        assertThat(mobInstanceRepository.findById(mob.getId())).isEmpty(); // 방 삭제와 함께 지움(다음 틱을 기다리지 않음)
    }

    // ---- 같은 틱의 행동 순서·취소·밀림

    @Test
    void deadMobActionCancelled() {
        GameMap map = prepareMap();
        enterAs("a", "한백", Realm.QI_13); // 위력 33.6 → 피해 29.6, 청모랑 49 는 두 번에
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "공격 청모랑");
        advance(2);
        tick(); // 몹 1.5초 행동 → 다음 4.5초
        sender.clear();

        advance(3);
        tick(); // 5초 틱: 한백 3초 일격이 먼저 → 몹 쓰러짐 → 4.5초 몹 행동 취소

        assertThat(combatLines("a")).containsExactly("청모랑에게 일격을 날린다. (피해 30)", "청모랑을 쓰러뜨렸습니다.");
    }

    @Test
    void defeatedPlayerActionCancelled() {
        GameMap map = prepareMap();
        Long hanbaek = enterAs("a", "한백", Realm.QI_1);
        placeMob(map, boss(), 1);
        characterVitalsEditor.setQiBlood(characterIdOf(hanbaek), 10, 60);
        gameConnector.handle("a", "공격 쌍두 흑망");
        sender.clear();

        advance(4);
        tick(); // 1.5초 몹 행동이 먼저 → 한백이 짐 → 3초 일격 취소

        assertThat(combatLines("a")).containsExactly(
                "쌍두 흑망이 두 머리 물기로 덮친다! (피해 16, 기혈 0/60)",
                "기혈이 다했습니다. 눈앞이 캄캄해진다…",
                "마을 어귀에서 깨어났습니다. (기혈 1/60)");
        assertThat(characterVitalsEditor.qiBloodLost(characterIdOf(hanbaek))).isEqualTo(59);
        MobInstance mob = onlyMob(map.entranceId());
        assertThat(mob.isFighting()).isFalse();
        assertThat(mob.getRefillAt()).isNotNull();
    }

    @Test
    void lateTickDoesNotBurst() {
        GameMap map = prepareMap();
        enterAs("a", "한백", Realm.QI_1);
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "공격 청모랑");
        sender.clear();

        advance(10);
        tick();

        assertThat(combatLines("a")).containsExactly(WOLF_HITS_FIRST, HIT_WOLF);
        MobInstance mob = onlyMob(map.entranceId());
        assertThat(mob.getNextActionAt()).isEqualTo(clock.instant().plusMillis(500));

        sender.clear();
        advance(0.5);
        tick();
        assertThat(combatLines("a")).containsExactly("청모랑이 물어뜯기로 덮친다! (피해 5, 기혈 50/60)");
    }

    // ---- 상태·피해 표시

    @Test
    void statusCombatLine() {
        GameMap map = prepareMap();
        enterAs("a", "한백", Realm.QI_1);
        placeMob(map, wolf(), 2);
        gameConnector.handle("a", "공격 청모랑");
        advanceAndTick(3); // 두 번 맞아 33/49 다침
        gameConnector.handle("a", "공격 2.청모랑");
        sender.clear();

        gameConnector.handle("a", "상태");

        assertThat(sender.lastTo("a", StatusMessage.class).text()).endsWith("\n[전투 중] 상대: 청모랑(다침), 청모랑 2");
    }

    @Test
    void statusWithoutCombatHasNoCombatLine() {
        GameMap map = prepareMap();
        enterAs("a", "한백", Realm.QI_1);
        placeMob(map, wolf(), 1);
        sender.clear();

        gameConnector.handle("a", "상태");

        assertThat(sender.lastTo("a", StatusMessage.class).text()).doesNotContain("[전투 중]");
    }

    /** 피해가 0 보다 크고 0.5 보다 작으면 {@code 1 미만}: 연기 2층 위력 13.8 − 육신 27 × 0.5 = 0.3 */
    @Test
    void damageDisplayLessThanOne() {
        GameMap map = prepareMap();
        enterAs("a", "한백", Realm.QI_2);
        MobTypeSaveInfo ghost = info("떠도는 음혼", MobKind.GHOST, Realm.FOUNDATION_PEAK, MobTier.NORMAL, "음기 뿜기",
                null, null);
        placeMob(map, ghost, 1);
        sender.clear();

        gameConnector.handle("a", "공격 떠도는 음혼");

        assertThat(combatLines("a")).containsExactly(
                "떠도는 음혼과 싸움이 벌어진다.", "떠도는 음혼에게 일격을 날린다. (피해 1 미만)");
        MobInstance mob = onlyMob(map.entranceId());
        assertThat(mob.getQiBlood()).isCloseTo(mob.getProfile().stats().qiBlood() - 0.3,
                offset(1e-9));
    }

    /** 범인은 영력 0 이라 위력 3: 육신 3 인 음혼에게 피해 1.5(표시 2), 위협·피해 기록은 층 전 3 */
    @Test
    void mortalPowerThree() {
        GameMap map = prepareMap();
        Long hanbaek = enterAs("a", "한백", Realm.MORTAL);
        placeMob(map, info("떠도는 음혼", MobKind.GHOST, Realm.QI_1, MobTier.NORMAL, "음기 뿜기", null, null), 1);
        sender.clear();

        gameConnector.handle("a", "공격 떠도는 음혼");

        assertThat(combatLines("a")).last().isEqualTo("떠도는 음혼에게 일격을 날린다. (피해 2)");
        MobInstance mob = onlyMob(map.entranceId());
        assertThat(mob.getQiBlood()).isEqualTo(40.5);
        assertThat(mob.engagementOf(characterIdOf(hanbaek)).orElseThrow().dealt()).isEqualTo(3);
    }

    // ---- 도우미

    private Long enterAs(String sessionId, String name, Realm realm) {
        Long accountId = enter(sessionId, name);
        setRealm(accountId, realm);
        return accountId;
    }

    private MobInstance onlyMob(Long roomId) {
        List<MobInstance> mobs = mobInstanceRepository.findAll().stream()
                .filter(mob -> mob.getRoomId().equals(roomId))
                .toList();
        assertThat(mobs).hasSize(1);
        return mobs.getFirst();
    }

    /** 그 연결이 받은 {@code system} {@code tone: "combat"} 줄 */
    private List<String> combatLines(String sessionId) {
        return sender.messagesTo(sessionId, SystemMessage.class).stream()
                .filter(message -> message.tone().equals("combat"))
                .map(SystemMessage::text)
                .toList();
    }
}
