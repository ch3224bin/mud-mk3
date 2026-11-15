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
import mudmk3.application.room.provided.RoomEditor;
import mudmk3.application.world.provided.WorldSettingsEditor;
import mudmk3.application.zone.provided.ZoneEditor;
import mudmk3.domain.combat.Corpse;
import mudmk3.domain.combat.MobInstance;
import mudmk3.domain.combat.PlayerCombat;
import mudmk3.domain.item.ItemCategory;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.shared.Realm;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import static mudmk3.domain.mob.MobFixture.atRealm;
import static mudmk3.domain.mob.MobFixture.described;
import static mudmk3.domain.mob.MobFixture.request;
import static mudmk3.domain.mob.MobFixture.snake;
import static mudmk3.domain.mob.MobFixture.turtle;
import static mudmk3.domain.mob.MobFixture.wolf;
import static org.assertj.core.api.Assertions.assertThat;

/** 서버 틱의 배치 맞추기·몹 나타나기와 방 보기·{@code 보기 <몹>}(D85·D89) */
@ApplicationServiceTest
class CombatSpawnTest extends BaseGameTest {

    private static final NoticeMessage WOLF_APPEARS = new NoticeMessage("청모랑이 나타난다.", "combat", "청모랑", "respawn");

    private final MobInstanceRepository mobInstanceRepository;
    private final CorpseRepository corpseRepository;
    private final PlayerCombatRepository playerCombatRepository;
    private final ApplicationContext applicationContext;

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
            ApplicationContext applicationContext
    ) {
        super(gameConnector, gameService, sender, accountLogin, characterCreator, characterFinder, characterMover,
                characterRepository, characterRealmEditor, zoneEditor, roomEditor, worldSettingsEditor, factionEditor, npcEditor,
                itemTypeEditor, itemTypeFinder, floorPlacementEditor, inventoryFinder, inventoryEditor, mobTypeEditor,
                mobPlacementEditor, clock, entityManager);
        this.mobInstanceRepository = mobInstanceRepository;
        this.corpseRepository = corpseRepository;
        this.playerCombatRepository = playerCombatRepository;
        this.applicationContext = applicationContext;
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
        second.receive(characterId, 20, clock.instant());
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
        assertThat(lookAt("s1", "청모랑의")).isEqualTo(SystemMessage.info("아직 자세히 볼 수 없습니다."));
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

    private List<Long> mobIdsIn(Long roomId) {
        return mobInstanceRepository.findAllInRoom(roomId).stream().map(MobInstance::getId).toList();
    }
}
