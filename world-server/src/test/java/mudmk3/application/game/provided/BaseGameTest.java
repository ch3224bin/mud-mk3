package mudmk3.application.game.provided;

import jakarta.persistence.EntityManager;
import mudmk3.MutableClock;
import mudmk3.application.account.provided.AccountLogin;
import mudmk3.application.account.provided.AccountLoginRequest;
import mudmk3.application.character.provided.CharacterCreator;
import mudmk3.application.character.provided.CharacterFinder;
import mudmk3.application.character.provided.CharacterMover;
import mudmk3.application.faction.provided.FactionEditor;
import mudmk3.application.floor.provided.FloorPlacementEditor;
import mudmk3.application.floor.provided.FloorPlacementSaveRequest;
import mudmk3.application.game.RecordingGameMessageSender;
import mudmk3.application.inventory.provided.InventoryEditor;
import mudmk3.application.inventory.provided.InventoryFinder;
import mudmk3.application.item.provided.ItemTypeEditor;
import mudmk3.application.item.provided.ItemTypeFinder;
import mudmk3.application.item.provided.ItemTypeSaveRequest;
import mudmk3.application.npc.provided.NpcEditor;
import mudmk3.application.npc.provided.NpcSaveRequest;
import mudmk3.application.npc.provided.NpcTopicRequest;
import mudmk3.application.room.provided.ExitSaveRequest;
import mudmk3.application.room.provided.RoomEditor;
import mudmk3.application.world.provided.StartingItemRequest;
import mudmk3.application.world.provided.WorldSettingsEditor;
import mudmk3.application.zone.provided.ZoneEditor;
import mudmk3.application.zone.provided.ZoneSaveRequest;
import mudmk3.domain.faction.Faction;
import mudmk3.domain.faction.FactionKind;
import mudmk3.domain.item.BagGrade;
import mudmk3.domain.item.ItemCategory;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.item.SpiritStoneGrade;
import mudmk3.domain.npc.Npc;
import mudmk3.domain.room.Direction;
import mudmk3.domain.room.Room;
import mudmk3.domain.zone.Zone;
import org.junit.jupiter.api.AfterEach;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;

import static mudmk3.domain.faction.FactionFixture.createFactionSaveRequest;
import static mudmk3.domain.room.RoomFixture.createRoomSaveRequest;
import static mudmk3.domain.zone.ZoneFixture.createZoneSaveRequest;

/**
 * 게임 포트 테스트의 기반. 접속 상태(presence, 세션)는 메모리 싱글턴이라 테스트 롤백으로 지워지지 않으므로
 * 테스트가 연 세션을 끝날 때 모두 끊는다.
 */
abstract class BaseGameTest {

    protected final GameConnector gameConnector;
    protected final RecordingGameMessageSender sender;
    protected final AccountLogin accountLogin;
    protected final CharacterCreator characterCreator;
    protected final CharacterFinder characterFinder;
    protected final CharacterMover characterMover;
    protected final ZoneEditor zoneEditor;
    protected final RoomEditor roomEditor;
    protected final WorldSettingsEditor worldSettingsEditor;
    protected final FactionEditor factionEditor;
    protected final NpcEditor npcEditor;
    protected final ItemTypeEditor itemTypeEditor;
    protected final ItemTypeFinder itemTypeFinder;
    protected final FloorPlacementEditor floorPlacementEditor;
    protected final InventoryFinder inventoryFinder;
    protected final InventoryEditor inventoryEditor;
    protected final MutableClock clock;
    protected final EntityManager entityManager;

    private final List<String> openedSessions = new ArrayList<>();

    protected BaseGameTest(
            GameConnector gameConnector,
            RecordingGameMessageSender sender,
            AccountLogin accountLogin,
            CharacterCreator characterCreator,
            CharacterFinder characterFinder,
            CharacterMover characterMover,
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
            MutableClock clock,
            EntityManager entityManager
    ) {
        this.gameConnector = gameConnector;
        this.sender = sender;
        this.accountLogin = accountLogin;
        this.characterCreator = characterCreator;
        this.characterFinder = characterFinder;
        this.characterMover = characterMover;
        this.zoneEditor = zoneEditor;
        this.roomEditor = roomEditor;
        this.worldSettingsEditor = worldSettingsEditor;
        this.factionEditor = factionEditor;
        this.npcEditor = npcEditor;
        this.itemTypeEditor = itemTypeEditor;
        this.itemTypeFinder = itemTypeFinder;
        this.floorPlacementEditor = floorPlacementEditor;
        this.inventoryFinder = inventoryFinder;
        this.inventoryEditor = inventoryEditor;
        this.clock = clock;
        this.entityManager = entityManager;
    }

    @BeforeEach
    void clearSender() {
        sender.clear();
        clock.reset();
    }

    @AfterEach
    void disconnectAll() {
        openedSessions.forEach(gameConnector::disconnect);
    }

    /** 지역 청우촌(마을 어귀(시작) 북↔남 우물가), 지역 청석숲(숲 입구), 우물가 동↔서 숲 입구 */
    protected GameMap prepareMap() {
        Zone village = zoneEditor.create(createZoneSaveRequest("청우촌"));
        Zone forest = zoneEditor.create(createZoneSaveRequest("청석숲"));
        Room entrance = roomEditor.create(createRoomSaveRequest(village.getId(), "마을 어귀"));
        Room well = roomEditor.create(createRoomSaveRequest(village.getId(), "우물가"));
        Room forestEntrance = roomEditor.create(createRoomSaveRequest(forest.getId(), "숲 입구"));
        roomEditor.putExit(entrance.getId(), Direction.NORTH, new ExitSaveRequest(well.getId(), true));
        roomEditor.putExit(well.getId(), Direction.EAST, new ExitSaveRequest(forestEntrance.getId(), true));
        worldSettingsEditor.designateStartRoom(entrance.getId());
        return new GameMap(id(village.getId()), id(forest.getId()),
                id(entrance.getId()), id(well.getId()), id(forestEntrance.getId()));
    }

    /**
     * {@link #prepareMap()} 에 더한다: 성 청석성과 그 소유 지역 청석성의 방 남문(우물가 북↔남),
     * 마을 어귀에 배치한 NPC 왕씨(낱말 단약·일거리)
     */
    protected NpcMap prepareNpcs(GameMap map) {
        Faction castle = factionEditor.create(createFactionSaveRequest(FactionKind.CASTLE, "청석성"));
        Zone castleZone = zoneEditor.create(new ZoneSaveRequest("청석성", "돌로 쌓은 성이다.", castle.getId()));
        Room southGate = roomEditor.create(createRoomSaveRequest(castleZone.getId(), "남문"));
        roomEditor.putExit(map.wellId(), Direction.NORTH, new ExitSaveRequest(southGate.getId(), true));
        Npc wang = npcEditor.create(new NpcSaveRequest("왕씨", "어서 오시오.", "글쎄, 잘 모르겠구려.", List.of(
                new NpcTopicRequest("단약", "회춘단이 좋소."),
                new NpcTopicRequest("일거리", "늑대가 말썽이오.")), false, map.entranceId()));
        return new NpcMap(id(castle.getId()), id(castleZone.getId()), id(southGate.getId()), id(wang.getId()));
    }

    /**
     * 아이템 종류: 회춘단(연기 초) 단약 3, 회기단(연기 초) 단약 4, 늑대 가죽 재료 2(묘사 {@code 거칠고 질긴 늑대의 가죽이다.}),
     * 늑대 이빨 재료 1(묘사 없음), 비검 법기 40, 하급 저물대 저물대 하급 30, 왕씨의 약초 꾸러미 퀘스트 0. 영석은 서버가 심은 것
     */
    protected Items prepareItems() {
        return new Items(
                item("회춘단(연기 초)", "", ItemCategory.PILL, 3, null),
                item("회기단(연기 초)", "", ItemCategory.PILL, 4, null),
                item("늑대 가죽", "거칠고 질긴 늑대의 가죽이다.", ItemCategory.MATERIAL, 2, null),
                item("늑대 이빨", "", ItemCategory.MATERIAL, 1, null),
                item("비검", "푸른 빛이 도는 작은 비검이다.", ItemCategory.ARTIFACT, 40, null),
                item("하급 저물대", "", ItemCategory.BAG, 30, BagGrade.LOW),
                item("왕씨의 약초 꾸러미", "왕씨가 맡긴 약초 꾸러미다.", ItemCategory.QUEST, 0, null),
                id(itemTypeFinder.spiritStone(SpiritStoneGrade.LOW).getId()));
    }

    private Long item(String name, String description, ItemCategory category, long baseValue, @Nullable BagGrade bagGrade) {
        ItemType itemType = itemTypeEditor.create(
                new ItemTypeSaveRequest(name, description, category, baseValue, true, bagGrade));
        return id(itemType.getId());
    }

    /** 시작 소지품을 정한다(뒤에 만드는 캐릭터부터 받는다) */
    protected void prepareStartingItems(StartingItemRequest... items) {
        worldSettingsEditor.replaceStartingItems(List.of(items));
    }

    /** 방 바닥에 물건을 둔다. 재등장 분이 null이면 한 번뿐 */
    protected Long place(Long roomId, Long itemTypeId, int count, @Nullable Integer respawnMinutes) {
        return id(floorPlacementEditor.create(roomId, new FloorPlacementSaveRequest(itemTypeId, count, respawnMinutes)).getId());
    }

    /** 그 계정 캐릭터의 소지품에 귀속 안 된 물건을 넣는다(다 들어가야 한다) */
    protected void give(Long accountId, Long itemTypeId, int count) {
        int added = inventoryEditor.addUpTo(characterIdOf(accountId), itemTypeId, count);
        if (added != count) throw new IllegalStateException("소지품에 다 들어가지 않았다: " + added + "/" + count);
    }

    protected Long characterIdOf(Long accountId) {
        return id(characterFinder.findByAccountId(accountId).orElseThrow().getId());
    }

    protected Long prepareAccount(String email) {
        return accountLogin.login(new AccountLoginRequest("google-" + email, email)).accountId();
    }

    /** 캐릭터를 만든 계정으로 접속한다(시작 방에서 시작) */
    protected Long enter(String sessionId, String name) {
        Long accountId = prepareAccount(emailOf(name));
        characterCreator.create(accountId, name);
        connect(sessionId, accountId);
        return accountId;
    }

    /** 캐릭터를 그 방에 옮겨 둔 뒤 접속한다 */
    protected Long enterAt(String sessionId, String name, Long roomId) {
        Long accountId = prepareAccount(emailOf(name));
        Long characterId = id(characterCreator.create(accountId, name).getId());
        characterMover.moveTo(characterId, roomId);
        connect(sessionId, accountId);
        return accountId;
    }

    protected void connect(String sessionId, Long accountId) {
        openedSessions.add(sessionId);
        gameConnector.connect(sessionId, accountId);
    }

    protected Long roomIdOf(Long accountId) {
        entityManager.flush();
        entityManager.clear();
        return characterFinder.findByAccountId(accountId).orElseThrow().getRoomId();
    }

    /** 한글 이름은 이메일 로컬 파트로 쓸 수 없어 코드 포인트로 바꾼다 */
    private static String emailOf(String name) {
        return "p" + name.codePoints().mapToObj(Integer::toHexString).reduce("", String::concat) + "@example.com";
    }

    private static Long id(@Nullable Long id) {
        if (id == null) throw new IllegalStateException("저장된 엔티티에 id가 없습니다");
        return id;
    }

    protected record GameMap(Long villageZoneId, Long forestZoneId, Long entranceId, Long wellId, Long forestEntranceId) {
    }

    protected record Items(Long pill, Long energyPill, Long hide, Long fang, Long sword, Long bag, Long quest,
                           Long lowStone) {
    }

    protected record NpcMap(Long castleId, Long castleZoneId, Long southGateId, Long wangId) {
    }
}
