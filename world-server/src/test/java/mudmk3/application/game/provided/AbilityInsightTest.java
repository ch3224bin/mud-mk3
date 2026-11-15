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
import mudmk3.application.game.required.GameMessage.StatusMessage;
import mudmk3.application.game.required.GameMessage.SystemMessage;
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
import mudmk3.domain.cultivation.Insight;
import mudmk3.domain.cultivation.InsightKind;
import mudmk3.domain.shared.Realm;
import mudmk3.domain.technique.TechniqueSaveInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static mudmk3.domain.mob.MobFixture.wolf;
import static mudmk3.domain.technique.TechniqueFixture.changchunInfo;
import static mudmk3.domain.technique.TechniqueFixture.changchunWith;
import static mudmk3.domain.technique.TechniqueFixture.fireBall;
import static mudmk3.domain.technique.TechniqueFixture.ironBodyInfo;
import static mudmk3.domain.technique.TechniqueFixture.stagePowers;
import static mudmk3.domain.technique.TechniqueFixture.withStagePowers;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
 * 깨달음 쌓기(command.md {@code ### 깨달음 쌓기}): 신통이 발동할 때 기본 양 × 상황 계수를 갈래로 나눠 한 상대 상한까지 쌓는다.
 * 기준(D117): 연기 1층 한백(목·화 50)이 장춘공을 익히고 청모랑(연기 2층, 기혈 49)에게 쓴다
 */
@ApplicationServiceTest
class AbilityInsightTest extends BaseGameTest {

    private static final String CAPPED = "같은 상대에게서는 더 깨닫는 것이 없습니다.";

    private final CharacterVitalsEditor characterVitalsEditor;

    AbilityInsightTest(
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
            CharacterVitalsEditor characterVitalsEditor
    ) {
        super(gameConnector, gameService, sender, accountLogin, characterCreator, characterFinder, characterMover,
                characterRepository, characterRealmEditor, zoneEditor, roomEditor, worldSettingsEditor, factionEditor, npcEditor,
                itemTypeEditor, itemTypeFinder, floorPlacementEditor, inventoryFinder, inventoryEditor, mobTypeEditor,
                mobPlacementEditor, clock, entityManager);
        this.characterVitalsEditor = characterVitalsEditor;
    }

    /** 장춘공(목)의 화탄술(화, 법력 5) → 화 5, 목 2.5(룰북) */
    @Test
    void fireballInsight() {
        GameMap map = prepareMap();
        Long han = hanbaek(changchunInfo());
        placeMob(map, wolf(), 1);

        gameConnector.handle("a", "화탄술 청모랑");

        Insight insight = insightOf(han);
        assertThat(insight.amount(InsightKind.FIRE)).isEqualTo(5);
        assertThat(insight.amount(InsightKind.WOOD)).isEqualTo(2.5);
        gameConnector.handle("a", "상태");
        assertThat(sender.lastTo("a", StatusMessage.class).text().lines().toList())
                .contains("깨달음  금 0 · 목 2 · 수 0 · 화 5 · 토 0 · 연체 0");
    }

    /** 철권(소모 없음, 대기 4초): 기준 소모 25 ÷ 5 × 4/3 ≈ 6.67 → 연체 */
    @Test
    void ironFistInsight() {
        GameMap map = prepareMap();
        Long han = hanbaek(ironBodyInfo());
        placeMob(map, wolf(), 1);

        gameConnector.handle("a", "철권 청모랑");

        assertThat(insightOf(han).amount(InsightKind.BODY)).isCloseTo(20.0 / 3, offset(1e-9));
        gameConnector.handle("a", "상태");
        assertThat(sender.lastTo("a", StatusMessage.class).text()).contains("연체 6");
    }

    /** 연기 7층이 청모랑(연기 2층)에게: 소경지 5 낮음 → ×0.5 */
    @Test
    void lowerMinorRealmHalves() {
        GameMap map = prepareMap();
        Long han = hanbaek(changchunInfo());
        setRealm(han, Realm.QI_7);
        placeMob(map, wolf(), 1);

        gameConnector.handle("a", "화탄술 청모랑");

        assertThat(insightOf(han).amount(InsightKind.FIRE)).isEqualTo(2.5);
        assertThat(insightOf(han).amount(InsightKind.WOOD)).isEqualTo(1.25);
    }

    /** 축기 초기가 청모랑에게: 대경지 낮음 → ×0.1 */
    @Test
    void lowerMajorRealmTenth() {
        GameMap map = prepareMap();
        Long han = hanbaek(changchunInfo());
        setRealm(han, Realm.FOUNDATION_EARLY);
        placeMob(map, wolf(), 1);

        gameConnector.handle("a", "화탄술 청모랑");

        assertThat(insightOf(han).amount(InsightKind.FIRE)).isCloseTo(0.5, offset(1e-9));
        assertThat(insightOf(han).amount(InsightKind.WOOD)).isCloseTo(0.25, offset(1e-9));
    }

    /**
     * 한 상대 상한 = 청모랑 기혈 최대치 49: 7.5 씩 6번 45, 7번째는 남은 4 만큼만(같은 비율) — 그 일격(피해 8 × 7 = 56)에 쓰러진다
     */
    @Test
    void insightCapPerOpponent() {
        GameMap map = prepareMap();
        Long han = hanbaek(changchunInfo());
        placeMob(map, wolf(), 1);
        castTimes(han, 6);
        assertThat(insightOf(han).amount(InsightKind.FIRE)).isEqualTo(30);
        assertThat(insightOf(han).amount(InsightKind.WOOD)).isEqualTo(15);
        sender.clear();

        castTimes(han, 1);

        assertThat(sender.messagesTo("a")).containsExactly(
                SystemMessage.combat("손끝에서 붉은 불덩이가 피어올라 청모랑을 덮친다! (피해 8, 법력 20/25)"),
                SystemMessage.info(CAPPED),
                SystemMessage.combat("청모랑을 쓰러뜨렸습니다."));
        Insight insight = insightOf(han);
        assertThat(insight.amount(InsightKind.FIRE)).isCloseTo(32.667, offset(0.001));
        assertThat(insight.amount(InsightKind.WOOD)).isCloseTo(16.333, offset(0.001));
        assertThat(insight.amount(InsightKind.FIRE) + insight.amount(InsightKind.WOOD)).isCloseTo(49, offset(1e-9));
    }

    @Test
    void capNotRepeated() {
        GameMap map = prepareMap();
        Long han = hanbaek(harmlessFireBall());
        placeMob(map, wolf(), 1);
        castTimes(han, 7);
        assertThat(textsTo("a")).containsOnlyOnce(CAPPED);
        sender.clear();

        castTimes(han, 1);

        assertThat(sender.messagesTo("a")).containsExactly(
                SystemMessage.combat("손끝에서 붉은 불덩이가 피어올라 청모랑을 덮친다! (피해 0, 법력 20/25)"));
        assertThat(totalInsight(han)).isCloseTo(49, offset(1e-9));
    }

    /** 상한 뒤 교전이 끝나고(끊김 → 몹 다시 참) 다시 싸우면 새로 센다 */
    @Test
    void insightCapResetsAfterReengage() {
        GameMap map = prepareMap();
        Long han = hanbaek(harmlessFireBall());
        placeMob(map, wolf(), 1);
        castTimes(han, 7);
        assertThat(totalInsight(han)).isCloseTo(49, offset(1e-9));
        gameConnector.disconnect("a");
        advanceAndTick(10);
        connect("a", han);
        sender.clear();

        castTimes(han, 1);

        assertThat(textsTo("a")).doesNotContain(CAPPED);
        assertThat(totalInsight(han)).isCloseTo(49 + 7.5, offset(1e-9));
    }

    @Test
    void rejectedCastAddsNoInsight() {
        GameMap map = prepareMap();
        Long han = hanbaek(changchunInfo());
        placeMob(map, wolf(), 1);
        setManaLost(han, 22);

        gameConnector.handle("a", "화탄술 청모랑");

        assertThat(sender.lastTo("a", SystemMessage.class).tone()).isEqualTo("error");
        assertThat(totalInsight(han)).isZero();
    }

    @Test
    void insightSurvivesDefeat() {
        GameMap map = prepareMap();
        Long han = hanbaek(changchunInfo());
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "화탄술 청모랑");
        characterVitalsEditor.setQiBlood(characterIdOf(han), 1, 60);

        advanceAndTick(2);

        assertThat(textsTo("a")).contains("기혈이 다했습니다. 눈앞이 캄캄해진다…");
        assertThat(insightOf(han).amount(InsightKind.FIRE)).isEqualTo(5);
        assertThat(insightOf(han).amount(InsightKind.WOOD)).isEqualTo(2.5);
    }

    // ---- 도우미

    /** 연기 1층 한백(목·화 50)이 그 공법을 익힌다 */
    private Long hanbaek(TechniqueSaveInfo technique) {
        Long accountId = enter("a", "한백");
        setRealm(accountId, Realm.QI_1);
        learn(accountId, technique);
        return accountId;
    }

    /** 피해 0 화탄술(단계별 기본 위력 0·계수 0, F6) — 몹이 쓰러지지 않아 상한까지 쓸 수 있다 */
    private static TechniqueSaveInfo harmlessFireBall() {
        return changchunWith(List.of(withStagePowers(fireBall(), stagePowers(1, 9, "0", "0", "0"))));
    }

    /** 시계만 움직이며(틱 없음 → 자동 공격·몹 공격 없음) 법력을 채워 가며 화탄술을 n 번 쓴다 */
    private void castTimes(Long accountId, int times) {
        for (int i = 0; i < times; i++) {
            setManaLost(accountId, 0);
            gameConnector.handle("a", "화탄술 청모랑");
            advance(3);
        }
    }

    private double totalInsight(Long accountId) {
        return insightOf(accountId).amounts().values().stream().mapToDouble(Double::doubleValue).sum();
    }

    private List<String> textsTo(String sessionId) {
        return sender.messagesTo(sessionId).stream()
                .filter(SystemMessage.class::isInstance)
                .map(message -> ((SystemMessage) message).text())
                .toList();
    }
}
