package mudmk3.application.game.provided;

import jakarta.persistence.EntityManager;
import mudmk3.ApplicationServiceTest;
import mudmk3.MutableClock;
import mudmk3.adapter.scheduling.GameTickScheduler;
import mudmk3.application.account.provided.AccountLogin;
import mudmk3.application.character.provided.CharacterCreator;
import mudmk3.application.character.provided.CharacterFinder;
import mudmk3.application.character.provided.CharacterMover;
import mudmk3.application.character.provided.CharacterRealmEditor;
import mudmk3.application.character.required.CharacterRepository;
import mudmk3.application.faction.provided.FactionEditor;
import mudmk3.application.floor.provided.FloorPlacementEditor;
import mudmk3.application.game.GameService;
import mudmk3.application.game.RecordingGameMessageSender;
import mudmk3.application.game.required.CorpseRepository;
import mudmk3.application.game.required.GameMessage.NoticeMessage;
import mudmk3.application.game.required.GameMessage.RoomMessage;
import mudmk3.application.game.required.GameMessage.RoomMessage.CorpseView;
import mudmk3.application.game.required.GameMessage.RoomMessage.FloorItemView;
import mudmk3.application.game.required.GameMessage.RoomMessage.MobView;
import mudmk3.application.game.required.GameMessage.RoomMessage.OccupantView;
import mudmk3.application.game.required.GameMessage.SystemMessage;
import mudmk3.application.game.required.MobInstanceRepository;
import mudmk3.application.game.required.PlayerCombatRepository;
import mudmk3.application.game.required.PresenceRepository;
import mudmk3.application.inventory.provided.InventoryEditor;
import mudmk3.application.inventory.provided.InventoryFinder;
import mudmk3.application.item.provided.ItemTypeEditor;
import mudmk3.application.item.provided.ItemTypeFinder;
import mudmk3.application.item.provided.ItemTypeSaveRequest;
import mudmk3.application.mob.provided.MobPlacementEditor;
import mudmk3.application.mob.provided.MobPlacementSaveRequest;
import mudmk3.application.mob.provided.MobTypeEditor;
import mudmk3.application.mob.provided.MobTypeView;
import mudmk3.application.npc.provided.NpcEditor;
import mudmk3.application.room.provided.RoomDeletedEvent;
import mudmk3.application.room.provided.RoomEditor;
import mudmk3.application.world.provided.WorldSettingsEditor;
import mudmk3.application.zone.provided.ZoneEditor;
import mudmk3.domain.combat.AttackNature;
import mudmk3.domain.combat.Corpse;
import mudmk3.domain.combat.MobInstance;
import mudmk3.domain.combat.PlayerCombat;
import mudmk3.domain.game.OnlinePlayer;
import mudmk3.domain.item.ItemCategory;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.room.Room;
import mudmk3.domain.shared.Realm;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static mudmk3.domain.mob.MobFixture.atRealm;
import static mudmk3.domain.mob.MobFixture.described;
import static mudmk3.domain.mob.MobFixture.request;
import static mudmk3.domain.mob.MobFixture.snake;
import static mudmk3.domain.mob.MobFixture.turtle;
import static mudmk3.domain.mob.MobFixture.wolf;
import static mudmk3.domain.room.RoomFixture.createRoomSaveRequest;
import static mudmk3.domain.zone.ZoneFixture.createZoneSaveRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 서버 틱의 배치 맞추기·몹 나타나기와 방 보기·{@code 보기 <몹>}(D85·D89) */
@ApplicationServiceTest
class CombatSpawnTest extends BaseGameTest {

    private static final NoticeMessage WOLF_APPEARS = new NoticeMessage("청모랑이 나타난다.", "combat", "청모랑", "respawn");

    private final MobInstanceRepository mobInstanceRepository;
    private final CorpseRepository corpseRepository;
    private final PlayerCombatRepository playerCombatRepository;
    private final ApplicationContext applicationContext;
    private final PresenceRepository presenceRepository;
    /** 방 삭제 반영({@link GameService#onRoomDeleted}). 테스트 트랜잭션은 커밋되지 않아 직접 넘긴다 */
    private final GameService gameService;

    CombatSpawnTest(
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
            CorpseRepository corpseRepository,
            PlayerCombatRepository playerCombatRepository,
            ApplicationContext applicationContext,
            PresenceRepository presenceRepository
    ) {
        super(gameConnector, gameService, sender, accountLogin, characterCreator, characterFinder, characterMover,
                characterRepository, characterRealmEditor, zoneEditor, roomEditor, worldSettingsEditor, factionEditor, npcEditor,
                itemTypeEditor, itemTypeFinder, floorPlacementEditor, inventoryFinder, inventoryEditor, mobTypeEditor,
                mobPlacementEditor, clock, entityManager);
        this.mobInstanceRepository = mobInstanceRepository;
        this.corpseRepository = corpseRepository;
        this.playerCombatRepository = playerCombatRepository;
        this.applicationContext = applicationContext;
        this.presenceRepository = presenceRepository;
        this.gameService = gameService;
    }

    @Test
    void firstTickSpawnsAll() {
        GameMap map = prepareMap();
        MobTypeView wolf = mobTypeEditor.create(request(wolf()));
        mobPlacementEditor.create(map.entranceId(), new MobPlacementSaveRequest(wolf.id(), 2));
        enter("s1", "한백");
        assertThat(look("s1").mobs()).isEmpty();

        tick();

        assertThat(look("s1").mobs()).containsExactly(new MobView("청모랑", "1급", null, 2, false));
        assertThat(mobInstanceRepository.findAllInRoom(map.entranceId()))
                .allSatisfy(mob -> assertThat(mob.getQiBlood()).isEqualTo(mob.getProfile().stats().qiBlood()))
                .extracting(MobInstance::getSlot)
                .containsExactly(0, 1);
    }

    @Test
    void spawnNotifiesRoom() {
        GameMap map = prepareMap();
        enter("s1", "한백");
        enterAt("s2", "한립", map.wellId());
        MobTypeView wolf = mobTypeEditor.create(request(wolf()));
        mobPlacementEditor.create(map.entranceId(), new MobPlacementSaveRequest(wolf.id(), 1));
        sender.clear();

        tick();

        assertThat(sender.messagesTo("s1")).containsExactly(WOLF_APPEARS);
        assertThat(sender.messagesTo("s2")).isEmpty();

        sender.clear();
        tick();
        assertThat(sender.messagesTo("s1")).isEmpty();
    }

    // ---- 조용한 방 상태(부채 정리 2차 #13)

    @Test
    void respawnSendsQuietRoom() {
        GameMap map = prepareMap();
        Long hanbaek = enter("s1", "한백");
        setRealm(hanbaek, Realm.FOUNDATION_EARLY);
        placeMob(map, wolf(), 1);
        gameConnector.handle("s1", "공격 청모랑"); // 축기 초기 일격 60 > 청모랑 기혈 49
        advanceAndTick(89.5);
        sender.clear();

        advanceAndTick(0.5);

        assertThat(sender.allMessagesTo("s1")).hasSize(2);
        assertThat(sender.allMessagesTo("s1").getFirst()).isEqualTo(WOLF_APPEARS);
        assertThat(sender.quietRoomsTo("s1")).singleElement()
                .satisfies(room -> assertThat(room.mobs()).extracting(MobView::name).containsExactly("청모랑"));
    }

    /** 관리 툴에서 배치를 지워 몹이 빠져도 그 방 사람에게 조용한 방 상태(알림 줄은 없음) */
    @Test
    void placementDeletedSendsQuietRoom() {
        GameMap map = prepareMap();
        PlacedMob placed = placeMob(map, wolf(), 2);
        enter("s1", "한백");
        enterAt("s2", "한립", map.wellId());
        sender.clear();

        mobPlacementEditor.delete(placed.placementId());
        tick();

        assertThat(sender.messagesTo("s1")).isEmpty();
        assertThat(sender.quietRoomsTo("s1")).singleElement()
                .satisfies(room -> assertThat(room.mobs()).isEmpty());
        assertThat(sender.allMessagesTo("s2")).isEmpty();
    }

    /**
     * 실패한 처리가 남긴 방 표시는 다음 처리로 새지 않는다(F-9). 몹이 나타나 방이 표시된 뒤 같은 틱의 법력 회복이 DB에 없는
     * 캐릭터(가짜 접속)를 읽다 실패한다 → 가짜 접속을 지우고 아무 일 없는 틱 → 조용한 방 상태도 알림도 없다
     */
    @Test
    void failedProcessingMarkDoesNotLeak() {
        GameMap map = prepareMap();
        enter("s1", "한백");
        OnlinePlayer ghost = OnlinePlayer.enter(999_999L, 999_999L, "유령", map.wellId(), "ghost");
        presenceRepository.save(ghost);
        tick(); // 유령의 법력 회복 시각을 처음 잡는다(0초라 깎인 법력을 읽지 않음)
        MobTypeView wolf = mobTypeEditor.create(request(wolf()));
        mobPlacementEditor.create(map.entranceId(), new MobPlacementSaveRequest(wolf.id(), 1));
        advance(1);
        sender.clear();

        assertThatThrownBy(this::tick).isInstanceOf(IllegalArgumentException.class);
        assertThat(mobInstanceRepository.findAllInRoom(map.entranceId())).hasSize(1);
        presenceRepository.delete(ghost.getCharacterId());

        advance(1);
        tick();

        assertThat(sender.allMessagesTo("s1")).isEmpty();
    }

    @Test
    void placementChangeRespawnsFresh() {
        GameMap map = prepareMap();
        PlacedMob placed = placeMob(map, wolf(), 2);
        List<Long> before = mobIdsIn(map.entranceId());
        enter("s1", "한백");
        sender.clear();

        mobPlacementEditor.update(placed.placementId(), new MobPlacementSaveRequest(placed.mobTypeId(), 1));
        tick();

        assertThat(sender.messagesTo("s1")).containsExactly(WOLF_APPEARS);
        assertThat(look("s1").mobs()).containsExactly(new MobView("청모랑", "1급", null, 1, false));
        assertThat(mobIdsIn(map.entranceId())).hasSize(1).doesNotContainAnyElementsOf(before);
    }

    @Test
    void placementMobChangeRespawns() {
        GameMap map = prepareMap();
        PlacedMob placed = placeMob(map, wolf(), 1);
        MobTypeView snake = mobTypeEditor.create(request(snake()));

        mobPlacementEditor.update(placed.placementId(), new MobPlacementSaveRequest(snake.id(), 1));
        tick();

        assertThat(mobInstanceRepository.findAllInRoom(map.entranceId()))
                .extracting(mob -> mob.getProfile().name())
                .containsExactly("적안 독사");
    }

    @Test
    void placementDeletedRemovesMobs() {
        GameMap map = prepareMap();
        PlacedMob placed = placeMob(map, wolf(), 2);
        enter("s1", "한백");
        sender.clear();

        mobPlacementEditor.delete(placed.placementId());
        tick();

        assertThat(sender.messagesTo("s1")).isEmpty();
        assertThat(look("s1").mobs()).isEmpty();
        assertThat(mobInstanceRepository.findAll()).isEmpty();
    }

    @Test
    void placementDeletedEndsCombat() {
        GameMap map = prepareMap();
        PlacedMob placed = placeMob(map, wolf(), 1);
        Long characterId = characterIdOf(enter("s1", "한백"));
        MobInstance mob = mobInstanceRepository.findAllInRoom(map.entranceId()).getFirst();
        mob.engage(characterId, clock.instant());
        PlayerCombat combat = new PlayerCombat(characterId);
        combat.engage(mob.getId());
        playerCombatRepository.save(combat);
        sender.clear();

        mobPlacementEditor.delete(placed.placementId());
        tick();

        assertThat(sender.messagesTo("s1")).isEmpty();
        assertThat(playerCombatRepository.find(characterId).orElseThrow().isFighting()).isFalse();
    }

    /**
     * 관리 툴에서 몹 유형을 지우면 배치가 함께 지워져 다음 틱에 교전 중이던 몹도 사라진다. 플레이어는 따로 알림 없이 교전에서
     * 빠지고 그 방 사람에게 조용한 방 상태만 간다
     */
    @Test
    void mobTypeDeletedRemovesLivingMob() {
        GameMap map = prepareMap();
        PlacedMob placed = placeMob(map, wolf(), 1);
        Long hanbaek = enter("s1", "한백");
        gameConnector.handle("s1", "공격 청모랑");
        assertThat(playerCombatRepository.find(characterIdOf(hanbaek)).orElseThrow().isFighting()).isTrue();
        sender.clear();

        mobTypeEditor.delete(placed.mobTypeId());
        advanceAndTick(1);

        assertThat(sender.quietRoomsTo("s1")).singleElement()
                .satisfies(room -> assertThat(room.mobs()).extracting(MobView::name).doesNotContain("청모랑"));
        assertThat(sender.messagesTo("s1")).isEmpty();
        assertThat(mobInstanceRepository.findAll()).isEmpty();

        gameConnector.handle("s1", "북");
        assertThat(roomIdOf(hanbaek)).isEqualTo(map.wellId());
        advanceAndTick(1);
    }

    /** 처치한 몹(쓰러진 채 재등장을 기다림)·시체·살아 있는 몹이 모두 다음 틱을 기다리지 않고 메모리에서 사라진다 */
    @Test
    void roomDeletionRemovesCorpsesAndMobs() {
        GameMap map = prepareMap();
        Long hanbaek = enterAt("s1", "한백", map.wellId());
        setRealm(hanbaek, Realm.FOUNDATION_EARLY);
        placeMob(map.wellId(), wolf(), 2);
        placeMob(map, snake(), 1);
        gameConnector.handle("s1", "공격 청모랑"); // 한 번에 쓰러뜨림
        assertThat(corpseRepository.findAllInRoom(map.wellId())).hasSize(1);
        assertThat(mobsIn(map.wellId())).hasSize(2);
        roomEditor.delete(map.wellId());

        gameConnector.handle("s1", "보기"); // 테스트 트랜잭션은 커밋되지 않아 다음 명령이 옮기기를 한다(4차 관례)

        assertThat(sender.lastTo("s1", RoomMessage.class).room().id()).isEqualTo(map.entranceId());
        assertThat(corpseRepository.findAllInRoom(map.wellId())).isEmpty();
        assertThat(mobsIn(map.wellId())).isEmpty();
        assertThat(mobsIn(map.entranceId())).hasSize(1);
    }

    /** 시작 방이 없어 옮길 사람이 없어도(P11) 그 방의 몹은 지운다 */
    @Test
    void roomDeletionWithoutStartRoomRemovesMobs() {
        Long zoneId = requireNonNull(zoneEditor.create(createZoneSaveRequest("청석숲")).getId());
        Room room = roomEditor.create(createRoomSaveRequest(zoneId, "숲 입구"));
        Long roomId = requireNonNull(room.getId());
        placeMob(roomId, wolf(), 1);
        roomEditor.delete(roomId);

        gameService.onRoomDeleted(new RoomDeletedEvent(roomId));

        assertThat(mobsIn(roomId)).isEmpty();
    }

    @Test
    void mobTypeEditKeepsLiveMob() {
        GameMap map = prepareMap();
        PlacedMob placed = placeMob(map, wolf(), 1);
        enter("s1", "한백");

        mobTypeEditor.update(placed.mobTypeId(), request(atRealm(wolf(), Realm.QI_8)));
        tick();
        assertThat(look("s1").mobs()).containsExactly(new MobView("청모랑", "1급", null, 1, false));

        // 새로 나타나는 몹은 지금 정의로 나타난다(재등장은 처치가 생기는 Task 7 에서 본다)
        mobPlacementEditor.update(placed.placementId(), new MobPlacementSaveRequest(placed.mobTypeId(), 2));
        tick();
        assertThat(look("s1").mobs()).containsExactly(new MobView("청모랑", "2급", null, 2, false));
    }

    @Test
    void roomViewOrder() {
        GameMap map = prepareMap();
        prepareNpcs(map);
        Items items = prepareItems();
        place(map.entranceId(), items.hide(), 2, null);
        placeMob(map, wolf(), 1);
        enter("s1", "한백");
        Long hanliAccount = enter("s2", "한립");
        corpseRepository.save(new Corpse(1, map.entranceId(), "들토끼", characterIdOf(hanliAccount), clock.instant(), List.of()));

        RoomMessage room = look("s1");

        assertThat(room.occupants()).containsExactly(OccupantView.player("한립"), OccupantView.npc("왕씨"));
        assertThat(room.mobs()).containsExactly(new MobView("청모랑", "1급", null, 1, false));
        assertThat(room.corpses()).containsExactly(new CorpseView("들토끼의 시체", false));
        assertThat(room.items()).containsExactly(new FloorItemView("늑대 가죽", 2));
        assertThat(Arrays.stream(RoomMessage.class.getRecordComponents()).map(RecordComponent::getName))
                .containsSubsequence("occupants", "mobs", "corpses", "items");
        assertThat(look("s2").corpses()).containsExactly(new CorpseView("들토끼의 시체", true));
    }

    @Test
    void strongBadgeAndGrouping() {
        GameMap map = prepareMap();
        placeMob(map, wolf(), 2);
        placeMob(map, snake(), 1);
        enter("s1", "한백");

        assertThat(look("s1").mobs()).containsExactly(
                new MobView("청모랑", "1급", null, 2, false),
                new MobView("적안 독사", "1급", "강적", 1, false));
    }

    @Test
    void fightingMobsGroupedApart() {
        GameMap map = prepareMap();
        placeMob(map, wolf(), 2);
        Long accountId = enter("s1", "한백");
        MobInstance second = mobInstanceRepository.findAllInRoom(map.entranceId()).get(1);
        second.engage(characterIdOf(accountId), clock.instant());

        assertThat(look("s1").mobs()).containsExactly(
                new MobView("청모랑", "1급", null, 1, false),
                new MobView("청모랑", "1급", null, 1, true));
    }

    @Test
    void unknowableRank() {
        GameMap map = prepareMap();
        placeMob(map, turtle(), 1);
        enter("s1", "한백");
        Long hanliAccount = enter("s2", "한립");
        setRealm(hanliAccount, Realm.QI_1);

        assertThat(look("s1").mobs()).containsExactly(new MobView("철갑 거북", "헤아릴 수 없음", null, 1, false));
        assertThat(look("s2").mobs()).containsExactly(new MobView("철갑 거북", "3급", null, 1, false));
    }

    @Test
    void lookMob() {
        GameMap map = prepareMap();
        placeMob(map, described(wolf(), "푸른 털의 늑대."), 2);
        placeMob(map, described(snake(), "붉은 눈이 번뜩인다."), 1);
        Long accountId = enter("s1", "한백");

        assertThat(lookAt("s1", "청모랑")).isEqualTo(SystemMessage.info("청모랑 (1급) — 푸른 털의 늑대. 멀쩡하다."));
        assertThat(lookAt("s1", "적안")).isEqualTo(SystemMessage.info("적안 독사 (1급) [강적] — 붉은 눈이 번뜩인다. 멀쩡하다."));

        // 둘째 청모랑만 다쳐 교전 중: 방 보기 순서(교전 안 함 → 교전 중)로 센다
        Long characterId = characterIdOf(accountId);
        MobInstance second = mobInstanceRepository.findAllInRoom(map.entranceId()).get(1);
        second.engage(characterId, clock.instant());
        second.receive(characterId, 20, AttackNature.NONE, clock.instant());
        assertThat(lookAt("s1", "2.청모랑")).isEqualTo(SystemMessage.info("청모랑 (1급) — 푸른 털의 늑대. 다쳤다."));
        assertThat(lookAt("s1", "1.청모랑")).isEqualTo(SystemMessage.info("청모랑 (1급) — 푸른 털의 늑대. 멀쩡하다."));
    }

    @Test
    void lookMobWithoutDescription() {
        GameMap map = prepareMap();
        placeMob(map, described(wolf(), ""), 1);
        enter("s1", "한백");

        assertThat(lookAt("s1", "청모랑")).isEqualTo(SystemMessage.info("청모랑 (1급) — 멀쩡하다."));
    }

    @Test
    void lookUnknowableMob() {
        GameMap map = prepareMap();
        placeMob(map, described(turtle(), ""), 1);
        enter("s1", "한백");

        assertThat(lookAt("s1", "거북")).isEqualTo(SystemMessage.info("철갑 거북 (헤아릴 수 없음) — 멀쩡하다."));
    }

    @Test
    void lookOrderPrefersMob() {
        GameMap map = prepareMap();
        ItemType sameName = itemTypeEditor.create(
                new ItemTypeSaveRequest("청모랑", "늑대 모양 장난감이다.", ItemCategory.MATERIAL, 1, null, null));
        place(map.entranceId(), sameName.getId(), 1, null);
        placeMob(map, described(wolf(), "푸른 털의 늑대."), 1);
        Long accountId = enter("s1", "한백");
        corpseRepository.save(new Corpse(1, map.entranceId(), "청모랑", characterIdOf(accountId), clock.instant(), List.of()));

        assertThat(lookAt("s1", "청모랑")).isEqualTo(SystemMessage.info("청모랑 (1급) — 푸른 털의 늑대. 멀쩡하다."));
        assertThat(lookAt("s1", "청모랑의")).isEqualTo(SystemMessage.item("청모랑의 시체 — 거둘 것이 없다."));
        assertThat(lookAt("s1", "3.청모랑")).isEqualTo(SystemMessage.item("청모랑 — 늑대 모양 장난감이다."));
    }

    @Test
    void schedulerDisabledInTests() {
        assertThat(applicationContext.getBeansOfType(GameTickScheduler.class)).isEmpty();
    }

    private RoomMessage look(String sessionId) {
        gameConnector.handle(sessionId, "보기");
        return sender.lastTo(sessionId, RoomMessage.class);
    }

    private SystemMessage lookAt(String sessionId, String target) {
        gameConnector.handle(sessionId, "보기 " + target);
        return sender.lastTo(sessionId, SystemMessage.class);
    }

    /** 그 방의 몹(쓰러진 것 포함) */
    private List<MobInstance> mobsIn(Long roomId) {
        return mobInstanceRepository.findAll().stream().filter(mob -> mob.getRoomId().equals(roomId)).toList();
    }

    private List<Long> mobIdsIn(Long roomId) {
        return mobInstanceRepository.findAllInRoom(roomId).stream().map(MobInstance::getId).toList();
    }
}
