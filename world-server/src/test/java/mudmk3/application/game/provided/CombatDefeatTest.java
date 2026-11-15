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
import mudmk3.application.game.required.GameMessage;
import mudmk3.application.game.required.GameMessage.NoticeMessage;
import mudmk3.application.game.required.GameMessage.RoomMessage;
import mudmk3.application.game.required.GameMessage.StatusMessage;
import mudmk3.application.game.required.GameMessage.StatusMessage.GaugeView;
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
import mudmk3.domain.shared.Realm;
import org.junit.jupiter.api.Test;

import java.util.List;

import static mudmk3.domain.mob.MobFixture.boss;
import static mudmk3.domain.mob.MobFixture.wolf;
import static org.assertj.core.api.Assertions.assertThat;

/** 질 때(D88·D96: 시작 방·기혈 1)와 교전 뒤 기혈 다시 차기(10초, 접속 때) — 쌍두 흑망 vs 연기 1층(기혈 60) */
@ApplicationServiceTest
class CombatDefeatTest extends BaseGameTest {

    private static final String BOSS_FINISHES = "쌍두 흑망이 두 머리 물기로 덮친다! (피해 16, 기혈 0/60)";
    private static final String DEFEATED = "기혈이 다했습니다. 눈앞이 캄캄해진다…";
    private static final String WOKE = "마을 어귀에서 깨어났습니다. (기혈 1/60)";
    private static final String REFILLED = "숨을 고르자 기운이 돌아온다. (기혈 60/60)";

    private final MobInstanceRepository mobInstanceRepository;
    private final PlayerCombatRepository playerCombatRepository;
    private final CharacterVitalsEditor characterVitalsEditor;

    CombatDefeatTest(
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

    // ---- 질 때

    @Test
    void defeatMovesToStartWithOne() {
        GameMap map = prepareMap();
        Long hanbaek = enterAtAs("a", "한백", map.wellId());
        enterAt("b", "한립", map.wellId());
        enter("c", "여소");
        placeMob(map.wellId(), boss(), 1);
        characterVitalsEditor.setQiBlood(characterIdOf(hanbaek), 10, 60);
        gameConnector.handle("a", "공격 쌍두 흑망");
        sender.clear();

        advanceAndTick(1.5);

        List<GameMessage> toA = sender.messagesTo("a");
        assertThat(toA).hasSize(4);
        assertThat(toA.subList(0, 3)).containsExactly(
                SystemMessage.combat(BOSS_FINISHES), SystemMessage.combat(DEFEATED), SystemMessage.combat(WOKE));
        assertThat(((RoomMessage) toA.get(3)).room().id()).isEqualTo(map.entranceId());
        assertThat(sender.messagesTo("b")).containsExactly(NoticeMessage.combat("한백이 쓰러진다.", "한백", "defeated"));
        assertThat(sender.messagesTo("c")).containsExactly(NoticeMessage.movement("한백이 나타난다.", "한백", "appear"));
        assertThat(roomIdOf(hanbaek)).isEqualTo(map.entranceId());

        gameConnector.handle("a", "상태");
        assertThat(sender.lastTo("a", StatusMessage.class).stats().qiBlood()).isEqualTo(new GaugeView(1, 60));
        gameConnector.handle("c", "보기");
        assertThat(sender.lastTo("c", RoomMessage.class).occupants()).isNotEmpty();
    }

    @Test
    void defeatInStartRoomStays() {
        GameMap map = prepareMap();
        Long hanbaek = enterAs("a", "한백");
        enter("c", "여소");
        placeMob(map, boss(), 1);
        characterVitalsEditor.setQiBlood(characterIdOf(hanbaek), 10, 60);
        gameConnector.handle("a", "공격 쌍두 흑망");
        sender.clear();

        advanceAndTick(1.5);

        List<GameMessage> toA = sender.messagesTo("a");
        assertThat(toA).hasSize(4);
        assertThat(toA.subList(0, 3)).containsExactly(
                SystemMessage.combat(BOSS_FINISHES), SystemMessage.combat(DEFEATED), SystemMessage.combat(WOKE));
        assertThat(((RoomMessage) toA.get(3)).room().id()).isEqualTo(map.entranceId());
        assertThat(sender.messagesTo("c")).containsExactly(NoticeMessage.combat("한백이 쓰러진다.", "한백", "defeated"));
        assertThat(roomIdOf(hanbaek)).isEqualTo(map.entranceId());
    }

    /**
     * 몹 둘과 싸우다 지면 둘 다 상대가 없어지고 10초 뒤 몹도 플레이어도 다 찬다. 먼저 교전한 청모랑(한 번 맞음)이 1.5초에 먼저
     * 물고(10 → 5), 이어 흑망이 쓰러뜨린다
     */
    @Test
    void defeatEndsAllEngagements() {
        GameMap map = prepareMap();
        Long hanbaek = enterAtAs("a", "한백", map.wellId());
        placeMob(map.wellId(), boss(), 1);
        placeMob(map.wellId(), wolf(), 1);
        characterVitalsEditor.setQiBlood(characterIdOf(hanbaek), 10, 60);
        gameConnector.handle("a", "공격 청모랑"); // 일격 8 → 41
        gameConnector.handle("a", "공격 쌍두 흑망"); // 공격 간격이 안 차 교전만
        sender.clear();

        advanceAndTick(1.5);

        assertThat(combatLines("a")).containsExactly(
                "청모랑이 물어뜯기로 덮친다! (피해 5, 기혈 5/60)", BOSS_FINISHES, DEFEATED, WOKE);
        List<MobInstance> mobs = mobInstanceRepository.findAllInRoom(map.wellId());
        assertThat(mobs).hasSize(2).allSatisfy(mob -> {
            assertThat(mob.isFighting()).isFalse();
            assertThat(mob.opponentIds()).isEmpty();
        });
        MobInstance wolf = mobs.stream().filter(mob -> mob.getProfile().name().equals("청모랑")).findFirst()
                .orElseThrow();
        assertThat(wolf.getQiBlood()).isEqualTo(41);
        assertThat(playerCombatRepository.find(characterIdOf(hanbaek)).orElseThrow().isFighting()).isFalse();
        sender.clear();

        advanceAndTick(9.5);
        assertThat(sender.messagesTo("a")).isEmpty();
        assertThat(wolf.getQiBlood()).isEqualTo(41);

        advanceAndTick(0.5);
        assertThat(sender.messagesTo("a")).containsExactly(SystemMessage.info(REFILLED));
        assertThat(wolf.getQiBlood()).isEqualTo(49);
        assertThat(characterVitalsEditor.qiBloodLost(characterIdOf(hanbaek))).isZero();
    }

    // ---- 교전 뒤 기혈

    /** 연기 1층이 청모랑을 18초에 쓰러뜨림(그동안 물려 깎임) → 9.5초 뒤 아님, 10초 뒤 다 참 */
    @Test
    void refillTenSecondsAfterCombat() {
        GameMap map = prepareMap();
        Long hanbaek = enterAs("a", "한백");
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "공격 청모랑");
        advanceAndTick(18);
        assertThat(combatLines("a")).last().isEqualTo("청모랑을 쓰러뜨렸습니다.");
        assertThat(characterVitalsEditor.qiBloodLost(characterIdOf(hanbaek))).isPositive();
        sender.clear();

        advanceAndTick(9.5);
        assertThat(sender.messagesTo("a")).isEmpty();

        advanceAndTick(0.5);
        assertThat(sender.messagesTo("a")).containsExactly(SystemMessage.info(REFILLED));
        assertThat(characterVitalsEditor.qiBloodLost(characterIdOf(hanbaek))).isZero();
    }

    @Test
    void noRefillMessageWhenFull() {
        GameMap map = prepareMap();
        Long hanbaek = enterAs("a", "한백");
        setRealm(hanbaek, Realm.FOUNDATION_EARLY);
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "공격 청모랑"); // 한 번에 쓰러뜨려 깎이지 않음
        sender.clear();

        advanceAndTick(10);

        assertThat(sender.messagesTo("a")).isEmpty();
    }

    /** 첫 청모랑을 18초에 쓰러뜨리고 23초에 둘째와 교전 → 28초에 다 차지 않는다 */
    @Test
    void reengageCancelsPlayerRefill() {
        GameMap map = prepareMap();
        Long hanbaek = enterAs("a", "한백");
        placeMob(map, wolf(), 2);
        gameConnector.handle("a", "공격 청모랑");
        advanceAndTick(18);
        assertThat(combatLines("a")).last().isEqualTo("청모랑을 쓰러뜨렸습니다.");
        advanceAndTick(5);
        gameConnector.handle("a", "공격 청모랑");
        sender.clear();

        advanceAndTick(5.5);

        assertThat(sender.messagesTo("a", SystemMessage.class)).noneMatch(message -> message.text().equals(REFILLED));
        assertThat(playerCombatRepository.find(characterIdOf(hanbaek)).orElseThrow().getRefillAt()).isNull();
        assertThat(characterVitalsEditor.qiBloodLost(characterIdOf(hanbaek))).isPositive();
    }

    /** 깎인 채 끊고 다시 접속하면 들어온 뒤 10초에 찬다 */
    @Test
    void hurtPlayerRefillsAfterEnter() {
        prepareMap();
        Long hanbaek = enterAs("a", "한백");
        characterVitalsEditor.setQiBlood(characterIdOf(hanbaek), 30, 60);
        gameConnector.disconnect("a");
        advance(30);
        connect("a2", hanbaek);
        sender.clear();

        advanceAndTick(9.5);
        assertThat(sender.messagesTo("a2")).isEmpty();

        advanceAndTick(0.5);
        assertThat(sender.messagesTo("a2")).containsExactly(SystemMessage.info(REFILLED));
        assertThat(characterVitalsEditor.qiBloodLost(characterIdOf(hanbaek))).isZero();
    }

    // ---- 도우미

    /** 연기 1층으로 시작 방에 들어온다 */
    private Long enterAs(String sessionId, String name) {
        Long accountId = enter(sessionId, name);
        setRealm(accountId, Realm.QI_1);
        return accountId;
    }

    /** 연기 1층으로 그 방에 들어온다 */
    private Long enterAtAs(String sessionId, String name, Long roomId) {
        Long accountId = enterAt(sessionId, name, roomId);
        setRealm(accountId, Realm.QI_1);
        return accountId;
    }

    private List<String> combatLines(String sessionId) {
        return sender.messagesTo(sessionId, SystemMessage.class).stream()
                .filter(message -> message.tone().equals("combat"))
                .map(SystemMessage::text)
                .toList();
    }
}
