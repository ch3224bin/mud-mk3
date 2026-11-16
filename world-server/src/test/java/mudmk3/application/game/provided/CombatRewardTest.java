package mudmk3.application.game.provided;

import jakarta.persistence.EntityManager;
import mudmk3.ApplicationServiceTest;
import mudmk3.MutableClock;
import mudmk3.ScriptedRandom;
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
import mudmk3.application.game.required.GameMessage.SystemMessage;
import mudmk3.application.game.required.MobInstanceRepository;
import mudmk3.application.inventory.provided.InventoryEditor;
import mudmk3.application.inventory.provided.InventoryFinder;
import mudmk3.application.item.provided.ItemTypeEditor;
import mudmk3.application.item.provided.ItemTypeFinder;
import mudmk3.application.item.provided.ItemTypeSaveRequest;
import mudmk3.application.mob.provided.MobPlacementEditor;
import mudmk3.application.mob.provided.MobTypeEditor;
import mudmk3.application.npc.provided.NpcEditor;
import mudmk3.application.room.provided.RoomEditor;
import mudmk3.application.world.provided.WorldSettingsEditor;
import mudmk3.application.zone.provided.ZoneEditor;
import mudmk3.domain.combat.Corpse;
import mudmk3.domain.combat.MobInstance;
import mudmk3.domain.item.ItemCategory;
import mudmk3.domain.mob.DropTier;
import mudmk3.domain.mob.MobFixedDrop;
import mudmk3.domain.mob.MobFixture;
import mudmk3.domain.mob.MobKind;
import mudmk3.domain.mob.MobTier;
import mudmk3.domain.mob.MobTierDrop;
import mudmk3.domain.mob.MobTypeSaveInfo;
import mudmk3.domain.shared.Realm;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static mudmk3.domain.mob.MobFixture.info;
import static mudmk3.domain.mob.MobFixture.snake;
import static mudmk3.domain.mob.MobFixture.withDrops;
import static mudmk3.domain.mob.MobFixture.wolf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/** 처치 보상(D87): 시체·{@code 수습}(60초 몫·부분·빈 시체·지워진 물건)·시체 사라짐(3분)·재등장 */
@ApplicationServiceTest
class CombatRewardTest extends BaseGameTest {

    private static final String KILL_WOLF = "공격 청모랑"; // 축기 초기 위력 64.2 → 피해 60, 청모랑 기혈 49 를 한 번에

    private final MobInstanceRepository mobInstanceRepository;
    private final CorpseRepository corpseRepository;
    private final ScriptedRandom random;

    CombatRewardTest(
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
            ScriptedRandom random
    ) {
        super(gameConnector, gameService, sender, accountLogin, characterCreator, characterFinder, characterMover,
                characterRepository, characterRealmEditor, zoneEditor, roomEditor, worldSettingsEditor, factionEditor, npcEditor,
                itemTypeEditor, itemTypeFinder, floorPlacementEditor, inventoryFinder, inventoryEditor, mobTypeEditor,
                mobPlacementEditor, clock, entityManager);
        this.mobInstanceRepository = mobInstanceRepository;
        this.corpseRepository = corpseRepository;
        this.random = random;
    }

    // ---- 시체

    @Test
    void corpseShownAfterKill() {
        GameMap map = prepareMap();
        enterAs("a", "한백", Realm.FOUNDATION_EARLY);
        enter("b", "한립");
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", KILL_WOLF);
        sender.clear();

        gameConnector.handle("a", "보기");
        gameConnector.handle("b", "보기");

        assertThat(sender.lastTo("a", RoomMessage.class).corpses())
                .containsExactly(new CorpseView("청모랑의 시체", true));
        assertThat(sender.lastTo("b", RoomMessage.class).corpses())
                .containsExactly(new CorpseView("청모랑의 시체", false));
        assertThat(sender.lastTo("a", RoomMessage.class).mobs()).isEmpty();
    }

    @Test
    void lookCorpseNotYet() {
        GameMap map = prepareMap();
        enterAs("a", "한백", Realm.FOUNDATION_EARLY);
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", KILL_WOLF);
        sender.clear();

        gameConnector.handle("a", "보기 청모랑의 시체");

        assertThat(sender.messagesTo("a")).containsExactly(SystemMessage.info("아직 자세히 볼 수 없습니다."));
    }

    /** 처치자는 피해(층 전 위력의 합)가 가장 많은 사람: 마지막 일격을 한 사람이 아니어도 시체는 처치자 몫 */
    @Test
    void killerIsTopDamageDealer() {
        GameMap map = prepareMap();
        Long hanbaek = enterAs("a", "한백", Realm.QI_1);
        enterAs("b", "한립", Realm.FOUNDATION_EARLY);
        placeMob(map, snake(), 1); // 기혈 80, 육신 10
        gameConnector.handle("a", "공격 적안 독사"); // 3초마다 위력 12 → 피해 7
        advanceAndTick(15); // 0·3·6·9·12·15초 여섯 번: 피해 42, 위력 합 72 → 38 남음
        gameConnector.handle("b", "공격 적안 독사"); // 위력 64.2 → 피해 59.2 로 쓰러짐(위력 합 64.2 < 72)

        assertThat(combatLines("b")).endsWith("적안 독사에게 일격을 날린다. (피해 59)", "적안 독사가 쓰러졌다.");
        assertThat(combatLines("a")).last().isEqualTo("적안 독사를 쓰러뜨렸습니다.");
        Corpse corpse = onlyCorpse(map.entranceId());
        assertThat(corpse.getKillerCharacterId()).isEqualTo(characterIdOf(hanbaek));
        sender.clear();

        gameConnector.handle("b", "수습 적안 독사");
        gameConnector.handle("a", "수습 적안 독사");

        assertThat(sender.messagesTo("b")).containsExactly(
                SystemMessage.error("그 시체는 아직 다른 사람의 몫입니다. (1분 뒤 수습할 수 있습니다)"),
                NoticeMessage.item("한백이 적안 독사의 시체를 수습한다.", "한백", "collect"));
        assertThat(sender.messagesTo("a")).containsExactly(
                SystemMessage.item("적안 독사의 시체에는 거둘 것이 없습니다."));
    }

    // ---- 수습

    @Test
    void collectFixedDrops() {
        GameMap map = prepareMap();
        Items items = prepareItems();
        Long wolfHide = wolfHide();
        Long hanbaek = enterAs("a", "한백", Realm.FOUNDATION_EARLY);
        enter("b", "한립");
        placeMob(map, wolfWithDrops(wolfHide, items.fang()), 1);
        gameConnector.handle("a", KILL_WOLF);
        sender.clear();

        gameConnector.handle("a", "수습 청모랑");

        assertThat(sender.messagesTo("a")).containsExactly(
                SystemMessage.item("청모랑의 시체를 수습했습니다: 청모랑 가죽 1, 늑대 이빨 1"));
        assertThat(sender.messagesTo("b")).containsExactly(
                NoticeMessage.item("한백이 청모랑의 시체를 수습한다.", "한백", "collect"));
        assertThat(corpseRepository.findAllInRoom(map.entranceId())).isEmpty();
        assertThat(inventoryFinder.find(characterIdOf(hanbaek)).getEntries())
                .extracting(entry -> entry.getItemType().getName(), entry -> entry.getCount())
                .containsExactly(tuple("청모랑 가죽", 1),
                        tuple("늑대 이빨", 1));
    }

    /** 고정 재료 매입가 합 0.4 + 0.4 = 0.8, 0.1 < 0.3 이라 영석, u = 0.5 → floor(0.8 × 1.5) = 1. 영석도 칸을 써 저물대를 든다 */
    @Test
    void collectWithStones() {
        GameMap map = prepareMap();
        Items items = prepareItems();
        Long wolfHide = wolfHide();
        Long hanbaek = enterAs("a", "한백", Realm.FOUNDATION_EARLY);
        give(hanbaek, items.bag(), 1);
        placeMob(map, wolfWithDrops(wolfHide, items.fang()), 1);
        random.enqueueDoubles(0.1, 0.5);
        gameConnector.handle("a", KILL_WOLF);
        sender.clear();

        gameConnector.handle("a", "수습 청모랑");

        assertThat(sender.messagesTo("a")).containsExactly(
                SystemMessage.item("청모랑의 시체를 수습했습니다: 청모랑 가죽 1, 늑대 이빨 1, 하품 영석 1"));
    }

    /** 고정 재료가 있어 영석 굴림이 먼저(0.9 — 안 됨), 고급 0.01 < 0.08 → 고급 목록에서 nextInt 1 = 둘째 물건 */
    @Test
    void collectTierDrop() {
        GameMap map = prepareMap();
        Long wolfHide = wolfHide();
        Long fang = material("청모랑 송곳니", 3);
        Long core = material("푸른 요단", 5);
        enterAs("a", "한백", Realm.FOUNDATION_EARLY);
        placeMob(map, withDrops(wolf(), List.of(new MobFixedDrop(wolfHide, 1)),
                List.of(new MobTierDrop(DropTier.ADVANCED, fang), new MobTierDrop(DropTier.ADVANCED, core))), 1);
        random.enqueueDoubles(0.9, 0.01);
        random.enqueueInts(1);
        gameConnector.handle("a", KILL_WOLF);
        sender.clear();

        gameConnector.handle("a", "수습 청모랑");

        assertThat(sender.messagesTo("a")).containsExactly(
                SystemMessage.item("청모랑의 시체를 수습했습니다: 청모랑 가죽 1, 푸른 요단 1"));
    }

    @Test
    void otherPlayerWaitsForCorpse() {
        GameMap map = prepareMap();
        Items items = prepareItems();
        Long wolfHide = wolfHide();
        enterAs("a", "한백", Realm.FOUNDATION_EARLY);
        enter("b", "한립");
        placeMob(map, wolfWithDrops(wolfHide, items.fang()), 1);
        gameConnector.handle("a", KILL_WOLF);
        advance(26);
        sender.clear();

        gameConnector.handle("b", "수습 청모랑");
        advance(33.5);
        gameConnector.handle("b", "수습 청모랑"); // 59.5초: 남은 0.5초는 올려 1초

        assertThat(sender.messagesTo("b")).containsExactly(
                SystemMessage.error("그 시체는 아직 다른 사람의 몫입니다. (34초 뒤 수습할 수 있습니다)"),
                SystemMessage.error("그 시체는 아직 다른 사람의 몫입니다. (1초 뒤 수습할 수 있습니다)"));
        sender.clear();

        advance(0.5);
        gameConnector.handle("b", "보기");
        gameConnector.handle("b", "수습 청모랑");

        assertThat(sender.lastTo("b", RoomMessage.class).corpses())
                .containsExactly(new CorpseView("청모랑의 시체", true));
        assertThat(sender.lastTo("b", SystemMessage.class))
                .isEqualTo(SystemMessage.item("청모랑의 시체를 수습했습니다: 청모랑 가죽 1, 늑대 이빨 1"));
        assertThat(sender.messagesTo("a")).containsExactly(
                NoticeMessage.item("한립이 청모랑의 시체를 수습한다.", "한립", "collect"));
    }

    /** 기본 2칸 중 1칸을 비검이 차지: 가죽은 들어가고 이빨은 남는다 → 비검을 버리고 다시 수습하면 나머지를 거둔다 */
    @Test
    void collectPartialWhenBagFull() {
        GameMap map = prepareMap();
        Items items = prepareItems();
        Long wolfHide = wolfHide();
        Long hanbaek = enterAs("a", "한백", Realm.FOUNDATION_EARLY);
        give(hanbaek, items.sword(), 1);
        placeMob(map, wolfWithDrops(wolfHide, items.fang()), 1);
        gameConnector.handle("a", KILL_WOLF);
        sender.clear();

        gameConnector.handle("a", "수습 청모랑");

        assertThat(sender.messagesTo("a")).containsExactly(
                SystemMessage.item("청모랑의 시체를 수습했습니다: 청모랑 가죽 1"),
                SystemMessage.error("저물대에 자리가 없습니다. 남은 것: 늑대 이빨 1"));
        assertThat(onlyCorpse(map.entranceId()).getRemaining()).extracting(line -> line.name()).containsExactly("늑대 이빨");

        gameConnector.handle("a", "버리기 비검");
        sender.clear();
        gameConnector.handle("a", "수습 청모랑");

        assertThat(sender.messagesTo("a")).containsExactly(
                SystemMessage.item("청모랑의 시체를 수습했습니다: 늑대 이빨 1"));
        assertThat(corpseRepository.findAllInRoom(map.entranceId())).isEmpty();
    }

    @Test
    void collectNothingFits() {
        GameMap map = prepareMap();
        Items items = prepareItems();
        Long wolfHide = wolfHide();
        Long hanbaek = enterAs("a", "한백", Realm.FOUNDATION_EARLY);
        enter("b", "한립");
        give(hanbaek, items.sword(), 2);
        placeMob(map, wolfWithDrops(wolfHide, items.fang()), 1);
        gameConnector.handle("a", KILL_WOLF);
        sender.clear();

        gameConnector.handle("a", "수습 청모랑");

        assertThat(sender.messagesTo("a")).containsExactly(
                SystemMessage.error("저물대에 자리가 없습니다. 남은 것: 청모랑 가죽 1, 늑대 이빨 1"));
        assertThat(sender.messagesTo("b")).isEmpty();
        assertThat(onlyCorpse(map.entranceId()).getRemaining()).hasSize(2);
    }

    @Test
    void emptyCorpse() {
        GameMap map = prepareMap();
        enterAs("a", "한백", Realm.FOUNDATION_EARLY);
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", KILL_WOLF);
        sender.clear();

        gameConnector.handle("a", "수습 청모랑");

        assertThat(sender.messagesTo("a")).containsExactly(SystemMessage.item("청모랑의 시체에는 거둘 것이 없습니다."));
        assertThat(corpseRepository.findAllInRoom(map.entranceId())).isEmpty();
    }

    @Test
    void collectArgumentMissing() {
        prepareMap();
        enterAs("a", "한백", Realm.QI_1);
        sender.clear();

        gameConnector.handle("a", "수습");

        assertThat(sender.messagesTo("a"))
                .containsExactly(SystemMessage.error("무엇을 수습하시겠습니까? (예: 수습 늑대)"));
    }

    @Test
    void collectNotHere() {
        GameMap map = prepareMap();
        enterAs("a", "한백", Realm.FOUNDATION_EARLY);
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", KILL_WOLF);
        sender.clear();

        gameConnector.handle("a", "수습 곰");
        gameConnector.handle("a", "수습 2.청모랑");

        assertThat(sender.messagesTo("a")).containsExactly(
                SystemMessage.error("이곳에 '곰'은 없습니다."),
                SystemMessage.error("이곳에 '2.청모랑'은 없습니다."));
    }

    @Test
    void collectInCombatFail() {
        GameMap map = prepareMap();
        enterAs("a", "한백", Realm.FOUNDATION_EARLY);
        placeMob(map, wolf(), 2);
        gameConnector.handle("a", KILL_WOLF);
        gameConnector.handle("a", KILL_WOLF); // 남은 청모랑과 교전(공격 간격이 안 차 아직 치지 않음)
        sender.clear();

        gameConnector.handle("a", "수습 청모랑");

        assertThat(sender.messagesTo("a")).containsExactly(SystemMessage.error("싸우는 중에는 할 수 없습니다."));
        assertThat(corpseRepository.findAllInRoom(map.entranceId())).hasSize(1);
    }

    @Test
    void collectSkipsDeletedItemType() {
        GameMap map = prepareMap();
        Items items = prepareItems();
        Long wolfHide = wolfHide();
        enterAs("a", "한백", Realm.FOUNDATION_EARLY);
        PlacedMob placed = placeMob(map, wolfWithDrops(wolfHide, items.fang()), 1);
        gameConnector.handle("a", KILL_WOLF);
        mobTypeEditor.update(placed.mobTypeId(),
                MobFixture.request(withDrops(wolf(), List.of(new MobFixedDrop(wolfHide, 1)), List.of())));
        itemTypeEditor.delete(items.fang());
        entityManager.flush();
        sender.clear();

        gameConnector.handle("a", "수습 청모랑");

        assertThat(sender.messagesTo("a")).containsExactly(
                SystemMessage.item("청모랑의 시체를 수습했습니다: 청모랑 가죽 1"));
        assertThat(corpseRepository.findAllInRoom(map.entranceId())).isEmpty();
    }

    /** 배치·몹 유형을 지운 뒤 다음 틱 전에 처치: 쓰러지는 순간의 드롭 표가 없어 드롭 없는 시체(L-D3) */
    @Test
    void killAfterMobTypeDeletedLeavesEmptyCorpse() {
        GameMap map = prepareMap();
        Items items = prepareItems();
        Long wolfHide = wolfHide();
        enterAs("a", "한백", Realm.FOUNDATION_EARLY);
        PlacedMob placed = placeMob(map, wolfWithDrops(wolfHide, items.fang()), 1);
        mobPlacementEditor.delete(placed.placementId());
        mobTypeEditor.delete(placed.mobTypeId());
        entityManager.flush();
        sender.clear();

        gameConnector.handle("a", KILL_WOLF);

        assertThat(combatLines("a")).containsExactly(
                "청모랑과 싸움이 벌어진다.", "청모랑에게 일격을 날린다. (피해 60)", "청모랑을 쓰러뜨렸습니다.");
        assertThat(onlyCorpse(map.entranceId()).name()).isEqualTo("청모랑의 시체");
        sender.clear();

        gameConnector.handle("a", "수습 청모랑");

        assertThat(sender.messagesTo("a")).containsExactly(SystemMessage.item("청모랑의 시체에는 거둘 것이 없습니다."));
    }

    // ---- 시체 사라짐·재등장

    @Test
    void corpseExpiresWithLeftovers() {
        GameMap map = prepareMap();
        Items items = prepareItems();
        Long wolfHide = wolfHide();
        enterAs("a", "한백", Realm.FOUNDATION_EARLY);
        enter("b", "한립");
        MobTypeSaveInfo slowWolf = info("청모랑", MobKind.BEAST, Realm.QI_2, MobTier.NORMAL, "물어뜯기", null, 600);
        placeMob(map, withDrops(slowWolf, List.of(new MobFixedDrop(wolfHide, 1), new MobFixedDrop(items.fang(), 1)),
                List.of()), 1);
        gameConnector.handle("a", KILL_WOLF);

        advanceAndTick(179.5);
        assertThat(corpseRepository.findAllInRoom(map.entranceId())).hasSize(1);
        sender.clear();

        advanceAndTick(0.5);
        gameConnector.handle("a", "보기");

        assertThat(sender.lastTo("a", RoomMessage.class).corpses()).isEmpty();
        assertThat(sender.messagesTo("a")).hasSize(1);
        assertThat(sender.messagesTo("b")).isEmpty();
    }

    @Test
    void respawnAfterTierTime() {
        GameMap map = prepareMap();
        enterAs("a", "한백", Realm.FOUNDATION_EARLY);
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", KILL_WOLF);
        sender.clear();

        advanceAndTick(89.5);
        assertThat(sender.messagesTo("a")).isEmpty();
        assertThat(mobInstanceRepository.findAllInRoom(map.entranceId())).isEmpty();

        advanceAndTick(0.5);
        assertThat(sender.messagesTo("a"))
                .containsExactly(NoticeMessage.combat("청모랑이 나타난다.", "청모랑", "respawn"));
        MobInstance mob = mobInstanceRepository.findAllInRoom(map.entranceId()).getFirst();
        assertThat(mob.getQiBlood()).isEqualTo(mob.getProfile().stats().qiBlood());
        assertThat(mob.isFighting()).isFalse();
    }

    @Test
    void respawnOverrideSeconds() {
        GameMap map = prepareMap();
        enterAs("a", "한백", Realm.FOUNDATION_EARLY);
        placeMob(map, info("청모랑", MobKind.BEAST, Realm.QI_2, MobTier.NORMAL, "물어뜯기", null, 10), 1);
        gameConnector.handle("a", KILL_WOLF);
        sender.clear();

        advanceAndTick(9.5);
        assertThat(sender.messagesTo("a")).isEmpty();

        advanceAndTick(0.5);
        assertThat(sender.messagesTo("a"))
                .containsExactly(NoticeMessage.combat("청모랑이 나타난다.", "청모랑", "respawn"));
    }

    // ---- 도우미

    private Long enterAs(String sessionId, String name, Realm realm) {
        Long accountId = enter(sessionId, name);
        setRealm(accountId, realm);
        return accountId;
    }

    /** 청모랑 가죽: 재료, 기준가 1(매입가 0.4) */
    private Long wolfHide() {
        return material("청모랑 가죽", 1);
    }

    private Long material(String name, long baseValue) {
        return requireNonNull(itemTypeEditor.create(
                new ItemTypeSaveRequest(name, "", ItemCategory.MATERIAL, baseValue, null, null)).getId());
    }

    /** 고정 재료 가죽 1·이빨 1 인 청모랑 */
    private static MobTypeSaveInfo wolfWithDrops(Long hide, Long fang) {
        return withDrops(wolf(), List.of(new MobFixedDrop(hide, 1), new MobFixedDrop(fang, 1)), List.of());
    }

    private Corpse onlyCorpse(Long roomId) {
        List<Corpse> corpses = corpseRepository.findAllInRoom(roomId);
        assertThat(corpses).hasSize(1);
        return corpses.getFirst();
    }

    private List<String> combatLines(String sessionId) {
        return sender.messagesTo(sessionId, SystemMessage.class).stream()
                .filter(message -> message.tone().equals("combat"))
                .map(SystemMessage::text)
                .toList();
    }
}
