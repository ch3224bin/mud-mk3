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
import mudmk3.application.cultivation.provided.CultivationFinder;
import mudmk3.application.faction.provided.FactionEditor;
import mudmk3.application.floor.provided.FloorPlacementEditor;
import mudmk3.application.game.GameService;
import mudmk3.application.game.RecordingGameMessageSender;
import mudmk3.application.game.RecordingGameMessageSender.Sent;
import mudmk3.application.game.required.AbilityTimersRepository;
import mudmk3.application.game.required.GameMessage.StatusMessage.GaugeView;
import mudmk3.application.game.required.GameMessage.SayMessage;
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
import mudmk3.application.technique.provided.TechniqueEditor;
import mudmk3.application.world.provided.WorldSettingsEditor;
import mudmk3.application.zone.provided.ZoneEditor;
import mudmk3.domain.combat.AbilityTimers;
import mudmk3.domain.combat.MobInstance;
import mudmk3.domain.combat.PlayerCombat;
import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Realm;
import mudmk3.domain.technique.TechniqueSaveInfo;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static mudmk3.application.technique.provided.TechniqueRequestFixture.requestOf;
import static mudmk3.domain.mob.MobFixture.boss;
import static mudmk3.domain.mob.MobFixture.wolf;
import static mudmk3.domain.technique.TechniqueFixture.changchunStages;
import static mudmk3.domain.technique.TechniqueFixture.changchunWith;
import static mudmk3.domain.technique.TechniqueFixture.fireBall;
import static mudmk3.domain.technique.TechniqueFixture.heavenEye;
import static mudmk3.domain.technique.TechniqueFixture.lightning;
import static mudmk3.domain.technique.TechniqueFixture.windRiding;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 시전과 {@code 중지}(command.md {@code ### 시전과 `중지`}, D113). 기준: 연기 1층 한백(영력 5, 법력 25)이 화탄술·낙뢰술(시전 2초,
 * 대기 3초, 단계별 값은 화탄술과 같음 — F14)이 든 장춘공을 익히고 마을 어귀의 청모랑(기혈 49, 육신 8)에게 쓴다.
 * 낙뢰술 위력 (2 + 5 × 1.2) × 1.5 = 12 → 피해 8
 */
@ApplicationServiceTest
class AbilityCastingTest extends BaseGameTest {

    private static final String ENGAGED = "청모랑과 싸움이 벌어진다.";
    private static final String CAST_START = "낙뢰술을 펼치기 시작한다… (2초)";
    private static final String LIGHTNING_HIT = "하늘에서 내리친 뇌기가 청모랑을 꿰뚫는다! (피해 8, 법력 20/25)";
    private static final String CASTING = "술법을 펼치는 중입니다. (끊으려면 '중지')";
    private static final String SCATTERED = "펼치던 술법이 갈 곳을 잃고 흩어진다.";
    private static final String NOT_CASTING = "펼치는 술법이 없습니다.";
    private static final String AUTO_HIT = "청모랑에게 일격을 날린다. (피해 8)";

    private final TechniqueEditor techniqueEditor;
    private final PlayerCombatRepository playerCombatRepository;
    private final MobInstanceRepository mobInstanceRepository;
    private final AbilityTimersRepository abilityTimersRepository;
    private final CharacterVitalsEditor characterVitalsEditor;
    private final CultivationFinder cultivationFinder;
    /** 한백이 익힌 장춘공 id */
    private @Nullable Long techniqueId;

    AbilityCastingTest(
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
            TechniqueEditor techniqueEditor,
            PlayerCombatRepository playerCombatRepository,
            MobInstanceRepository mobInstanceRepository,
            AbilityTimersRepository abilityTimersRepository,
            CharacterVitalsEditor characterVitalsEditor,
            CultivationFinder cultivationFinder
    ) {
        super(gameConnector, gameService, sender, accountLogin, characterCreator, characterFinder, characterMover,
                characterRepository, characterRealmEditor, zoneEditor, roomEditor, worldSettingsEditor, factionEditor, npcEditor,
                itemTypeEditor, itemTypeFinder, floorPlacementEditor, inventoryFinder, inventoryEditor, mobTypeEditor,
                mobPlacementEditor, clock, entityManager);
        this.techniqueEditor = techniqueEditor;
        this.playerCombatRepository = playerCombatRepository;
        this.mobInstanceRepository = mobInstanceRepository;
        this.abilityTimersRepository = abilityTimersRepository;
        this.characterVitalsEditor = characterVitalsEditor;
        this.cultivationFinder = cultivationFinder;
    }

    // ---- 시작과 발동

    @Test
    void castStartLine() {
        GameMap map = prepareMap();
        enter("b", "한립");
        Long han = hanbaek("a");
        placeMob(map, wolf(), 1);
        sender.clear();

        gameConnector.handle("a", "낙뢰술 청모랑");

        assertThat(sender.messagesTo("a")).containsExactly(SystemMessage.combat(CAST_START));
        assertThat(sender.messagesTo("b")).isEmpty();
        assertThat(playerCombatRepository.find(characterIdOf(han)).map(PlayerCombat::isFighting)).contains(false);
        assertThat(mobNamed("청모랑").isFighting()).isFalse();
        gameConnector.handle("a", "상태");
        assertThat(sender.lastTo("a", StatusMessage.class).stats().mana()).isEqualTo(new GaugeView(25, 25));
    }

    /** 소모·재사용 대기·교전 시작은 발동할 때다 */
    @Test
    void castFiresAfterTwoSeconds() {
        GameMap map = prepareMap();
        enter("b", "한립");
        Long han = hanbaek("a");
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "낙뢰술 청모랑");
        sender.clear();

        advanceAndTick(2);

        assertThat(sender.messagesTo("a")).containsExactly(
                SystemMessage.combat(ENGAGED), SystemMessage.combat(LIGHTNING_HIT));
        assertThat(sender.messagesTo("b")).hasSize(1);
        assertThat(mobNamed("청모랑").getQiBlood()).isEqualTo(41);
        assertThat(manaLost(han)).isEqualTo(5);
        gameConnector.handle("a", "낙뢰술");
        assertThat(sender.lastTo("a", SystemMessage.class))
                .isEqualTo(SystemMessage.error("낙뢰술은 아직 쓸 수 없습니다. (3초)"));
    }

    @Test
    void globalCooldownStartsAtCastStart() {
        GameMap map = prepareMap();
        hanbaek("a");
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "낙뢰술 청모랑");
        advance(1);
        gameConnector.handle("a", "중지");

        gameConnector.handle("a", "화탄술 청모랑");

        assertThat(sender.lastTo("a", SystemMessage.class))
                .isEqualTo(SystemMessage.error("아직 기운을 고르는 중입니다. (0.5초)"));
    }

    // ---- 시전 중 막힘

    /** 검사 2(시전 중)는 검사 1 다음, 대상(3)보다 먼저 */
    @Test
    void otherAbilityBlockedWhileCasting() {
        GameMap map = prepareMap();
        hanbaek("a");
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "낙뢰술 청모랑");
        advance(1.6);

        assertRejected("화탄술 청모랑", CASTING);
        assertRejected("화탄술", CASTING);
        assertRejected("낙뢰술 청모랑", CASTING);
    }

    @Test
    void moveBlockedWhileCasting() {
        GameMap map = prepareMap();
        Long han = hanbaek("a");
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "낙뢰술 청모랑");

        assertRejected("북", CASTING);
        assertThat(roomIdOf(han)).isEqualTo(map.entranceId());

        gameConnector.handle("a", "중지");
        gameConnector.handle("a", "공격 청모랑");
        advance(1.6);
        gameConnector.handle("a", "낙뢰술");
        assertRejected("북", "싸우는 중에는 움직일 수 없습니다.");
    }

    @Test
    void otherCommandsAllowedWhileCasting() {
        GameMap map = prepareMap();
        hanbaek("a");
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "낙뢰술 청모랑");
        sender.clear();

        gameConnector.handle("a", "상태");
        gameConnector.handle("a", "말하기 안녕");
        gameConnector.handle("a", "공격 청모랑");

        assertThat(sender.messagesTo("a")).hasSize(4);
        assertThat(sender.messagesTo("a").get(0)).isInstanceOf(StatusMessage.class);
        assertThat(sender.messagesTo("a").get(1)).isInstanceOf(SayMessage.class);
        assertThat(sender.messagesTo("a").subList(2, 4)).containsExactly(
                SystemMessage.combat(ENGAGED), SystemMessage.combat(AUTO_HIT));
        advanceAndTick(2);
        assertThat(textsTo("a")).contains(LIGHTNING_HIT);
    }

    // ---- 중지

    @Test
    void stopCasting() {
        GameMap map = prepareMap();
        Long han = hanbaek("a");
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "낙뢰술 청모랑");
        sender.clear();

        gameConnector.handle("a", "중지");

        assertThat(sender.messagesTo("a")).containsExactly(SystemMessage.combat("펼치던 술법을 거두었습니다."));
        advanceAndTick(2);
        assertThat(sender.messagesTo("a")).hasSize(1);
        assertThat(manaLost(han)).isZero();
        assertThat(mobNamed("청모랑").getQiBlood()).isEqualTo(49);
        assertNoCooldown(han);
    }

    @Test
    void stopWithoutCasting() {
        prepareMap();
        hanbaek("a");
        sender.clear();

        gameConnector.handle("a", "중지");

        assertThat(sender.messagesTo("a")).containsExactly(SystemMessage.error(NOT_CASTING));
    }

    // ---- 발동 때 다시 확인

    /** 축기 초기 한립의 일격 64.2 → 피해 60 → 청모랑이 쓰러진다 */
    @Test
    void castingFizzlesWhenTargetDies() {
        GameMap map = prepareMap();
        Long han = hanbaek("a");
        Long lip = enter("b", "한립");
        setRealm(lip, Realm.FOUNDATION_EARLY);
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "낙뢰술 청모랑");
        gameConnector.handle("b", "공격 청모랑");
        assertThat(mobNamed("청모랑").isAlive()).isFalse();
        sender.clear();

        advanceAndTick(2);

        assertThat(textsTo("a")).containsExactly(SCATTERED);
        assertThat(sender.lastTo("a", SystemMessage.class).tone()).isEqualTo("combat");
        assertThat(manaLost(han)).isZero();
        assertNoCooldown(han);
        assertNotCasting(han);
    }

    @Test
    void castingFizzlesWhenAbilityRemoved() {
        GameMap map = prepareMap();
        Long han = hanbaek("a");
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "낙뢰술 청모랑");
        techniqueEditor.update(requireNonNull(techniqueId),
                requestOf(changchunWith(List.of(fireBall(), heavenEye(), windRiding()))));
        sender.clear();

        advanceAndTick(2);

        assertThat(sender.messagesTo("a")).containsExactly(SystemMessage.combat(SCATTERED));
        assertThat(manaLost(han)).isZero();
        assertThat(mobNamed("청모랑").isFighting()).isFalse();
    }

    @Test
    void castingFizzlesWhenTargetLeaves() {
        GameMap map = prepareMap();
        Long han = hanbaek("a");
        PlacedMob placed = placeMob(map, wolf(), 1);
        gameConnector.handle("a", "낙뢰술 청모랑");
        mobPlacementEditor.delete(placed.placementId());
        sender.clear();

        advanceAndTick(2);

        assertThat(sender.messagesTo("a")).containsExactly(SystemMessage.combat(SCATTERED));
        assertThat(manaLost(han)).isZero();
        assertNoCooldown(han);
    }

    /** 시작 뒤 깎인 법력 22 → 남은 3(2초 회복 0.25를 더해도 버린 정수 3) */
    @Test
    void castingManaShortAtFire() {
        GameMap map = prepareMap();
        Long han = hanbaek("a");
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "낙뢰술 청모랑");
        setManaLost(han, 22);
        sender.clear();

        advanceAndTick(2);

        assertThat(sender.messagesTo("a")).containsExactly(SystemMessage.error("법력이 모자랍니다. (필요 5, 남은 것 3)"));
        assertThat(manaLost(han)).isLessThan(22);
        assertThat(mobNamed("청모랑").isFighting()).isFalse();
        assertNoCooldown(han);
        assertNotCasting(han);
    }

    // ---- 교전과 함께

    /** 청모랑의 첫 공격(1.5초)을 받아도 시전은 끊기지 않는다 */
    @Test
    void castingNotInterruptedByHit() {
        GameMap map = prepareMap();
        hanbaek("a");
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "공격 청모랑");
        gameConnector.handle("a", "낙뢰술");
        sender.clear();

        advanceAndTick(2);

        List<String> lines = textsTo("a");
        assertThat(lines).hasSize(2);
        assertThat(lines.getFirst()).startsWith("청모랑이 물어뜯기로 덮친다!");
        assertThat(lines.get(1)).isEqualTo(LIGHTNING_HIT);
    }

    @Test
    void autoAttackContinuesWhileCasting() {
        GameMap map = prepareMap();
        hanbaek("a");
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "공격 청모랑");
        advanceAndTick(2);
        gameConnector.handle("a", "낙뢰술");
        sender.clear();

        advanceAndTick(1);
        assertThat(textsTo("a")).containsExactly(AUTO_HIT);

        advanceAndTick(1);
        assertThat(textsTo("a")).containsExactly(AUTO_HIT, LIGHTNING_HIT);
    }

    /** 쌍두 흑망의 첫 공격(1.5초)에 지면 시전이 지워진다 */
    @Test
    void defeatClearsCasting() {
        GameMap map = prepareMap();
        Long han = hanbaek("a");
        placeMob(map, boss(), 1);
        Long characterId = characterIdOf(han);
        characterVitalsEditor.setQiBlood(characterId, 10, cultivationFinder.stats(characterId).qiBlood());
        gameConnector.handle("a", "공격 쌍두 흑망");
        gameConnector.handle("a", "낙뢰술");
        advanceAndTick(1.5);
        assertThat(textsTo("a")).contains("기혈이 다했습니다. 눈앞이 캄캄해진다…");
        assertNotCasting(han);
        sender.clear();

        advanceAndTick(1);
        gameConnector.handle("a", "중지");

        assertThat(sender.messagesTo("a")).containsExactly(SystemMessage.error(NOT_CASTING));
    }

    @Test
    void disconnectClearsCasting() {
        GameMap map = prepareMap();
        Long han = hanbaek("a");
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "낙뢰술 청모랑");

        gameConnector.disconnect("a");
        connect("a2", han);
        advanceAndTick(2);
        sender.clear();
        gameConnector.handle("a2", "중지");

        assertThat(sender.messagesTo("a2")).containsExactly(SystemMessage.error(NOT_CASTING));
        assertThat(manaLost(han)).isZero();
        assertThat(mobNamed("청모랑").isFighting()).isFalse();
    }

    /**
     * 같은 시각: 교전 중인 한백의 자동 공격(무리 0)이 아직 교전하지 않던 한립의 시전 발동(무리 1)보다 먼저다.
     * 한백은 0초에 치고(다음 3초), 한립은 1초에 시전을 시작한다(발동 3초)
     */
    @Test
    void castingOrderAfterEngagedActions() {
        GameMap map = prepareMap();
        hanbaek("a");
        Long lip = enterQi1("b", "한립");
        learn(lip, new TechniqueSaveInfo("청뢰공", "", List.of(Element.WOOD), Realm.MORTAL, changchunStages(),
                List.of(lightning())));
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "공격 청모랑");
        advanceAndTick(1);
        gameConnector.handle("b", "낙뢰술 청모랑");
        sender.clear();

        advanceAndTick(2);

        List<Sent> sent = sender.sent();
        int autoHit = sent.indexOf(new Sent("a", SystemMessage.combat(AUTO_HIT)));
        int lightningHit = sent.indexOf(new Sent("b", SystemMessage.combat(LIGHTNING_HIT)));
        assertThat(autoHit).isNotNegative();
        assertThat(lightningHit).isGreaterThan(autoHit);
    }

    /** 같은 캐릭터의 같은 시각 자동 공격과 교전 중 시전 발동은 자동 공격이 먼저다(F20 넷째 키) */
    @Test
    void ownAutoAttackBeforeOwnCasting() {
        GameMap map = prepareMap();
        hanbaek("a");
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "공격 청모랑");
        advanceAndTick(1);
        gameConnector.handle("a", "낙뢰술");
        sender.clear();

        advanceAndTick(2);

        assertThat(textsTo("a")).containsSubsequence(AUTO_HIT, LIGHTNING_HIT);
    }

    // ---- 도우미

    /** 연기 1층 한백이 화탄술·낙뢰술이 든 장춘공을 익힌다 */
    private Long hanbaek(String sessionId) {
        Long accountId = enterQi1(sessionId, "한백");
        techniqueId = learn(accountId, changchunWith(List.of(fireBall(), lightning(), heavenEye(), windRiding())));
        return accountId;
    }

    private Long enterQi1(String sessionId, String name) {
        Long accountId = enter(sessionId, name);
        setRealm(accountId, Realm.QI_1);
        return accountId;
    }

    /** 한백(세션 a)의 거절은 {@code error} 한 줄이다 */
    private void assertRejected(String command, String text) {
        sender.clear();
        gameConnector.handle("a", command);
        assertThat(sender.messagesTo("a")).containsExactly(SystemMessage.error(text));
    }

    private void assertNoCooldown(Long accountId) {
        assertThat(abilityTimersRepository.find(characterIdOf(accountId)).map(AbilityTimers::getCooldowns).orElseThrow())
                .isEmpty();
    }

    private void assertNotCasting(Long accountId) {
        assertThat(abilityTimersRepository.find(characterIdOf(accountId)).flatMap(AbilityTimers::casting)).isEmpty();
    }

    private MobInstance mobNamed(String name) {
        return mobInstanceRepository.findAll().stream()
                .filter(mob -> mob.getProfile().name().equals(name))
                .findFirst().orElseThrow();
    }
}
