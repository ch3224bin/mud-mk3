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
import mudmk3.application.game.required.AbilityTimersRepository;
import mudmk3.application.game.required.GameMessage.NoticeMessage;
import mudmk3.application.game.required.GameMessage.RoomMessage;
import mudmk3.application.game.required.GameMessage.RoomMessage.CorpseView;
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
import mudmk3.domain.combat.MobInstance;
import mudmk3.domain.combat.PlayerCombat;
import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Realm;
import mudmk3.domain.technique.AbilityKind;
import mudmk3.domain.technique.AbilityTarget;
import mudmk3.domain.technique.CostStat;
import mudmk3.domain.technique.TechniqueSaveInfo;
import mudmk3.domain.technique.UsableWhen;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static mudmk3.application.technique.provided.TechniqueRequestFixture.requestOf;
import static mudmk3.domain.mob.MobFixture.rabbit;
import static mudmk3.domain.mob.MobFixture.wolf;
import static mudmk3.domain.technique.TechniqueFixture.ability;
import static mudmk3.domain.technique.TechniqueFixture.changchunInfo;
import static mudmk3.domain.technique.TechniqueFixture.changchunStages;
import static mudmk3.domain.technique.TechniqueFixture.changchunWith;
import static mudmk3.domain.technique.TechniqueFixture.fireBall;
import static mudmk3.domain.technique.TechniqueFixture.heavenEye;
import static mudmk3.domain.technique.TechniqueFixture.ironBodyInfo;
import static mudmk3.domain.technique.TechniqueFixture.stagePowers;
import static mudmk3.domain.technique.TechniqueFixture.windRiding;
import static mudmk3.domain.technique.TechniqueFixture.withCost;
import static mudmk3.domain.technique.TechniqueFixture.withStagePowers;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 공격 신통 쓰기(command.md {@code ### 신통 쓰기}·{@code ### 신통 교전 메시지}). 기준(D117): 한백(목·화 50, 연기 1층 — 영력 5,
 * 법력 25, 근력 3)이 장춘공을 익히고, 마을 어귀의 청모랑(기혈 49, 육신 8)에게 쓴다. 화탄술 위력 (2 + 5 × 1.2) × 1.5 = 12 → 피해 8
 */
@ApplicationServiceTest
class AbilityUseTest extends BaseGameTest {

    private static final String ENGAGED = "청모랑과 싸움이 벌어진다.";
    private static final String FIRE_BALL_HIT = "손끝에서 붉은 불덩이가 피어올라 청모랑을 덮친다! (피해 8, 법력 20/25)";
    private static final String UNKNOWN = "무슨 말인지 모르겠습니다. ('도움말' 로 명령을 볼 수 있습니다)";

    private final TechniqueEditor techniqueEditor;
    private final PlayerCombatRepository playerCombatRepository;
    private final MobInstanceRepository mobInstanceRepository;
    private final AbilityTimersRepository abilityTimersRepository;
    private final ScriptedRandom random;
    /** 신통을 쓰는 한백의 계정 */
    private @Nullable Long casterAccountId;

    AbilityUseTest(
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
            ScriptedRandom random
    ) {
        super(gameConnector, gameService, sender, accountLogin, characterCreator, characterFinder, characterMover,
                characterRepository, characterRealmEditor, zoneEditor, roomEditor, worldSettingsEditor, factionEditor, npcEditor,
                itemTypeEditor, itemTypeFinder, floorPlacementEditor, inventoryFinder, inventoryEditor, mobTypeEditor,
                mobPlacementEditor, clock, entityManager);
        this.techniqueEditor = techniqueEditor;
        this.playerCombatRepository = playerCombatRepository;
        this.mobInstanceRepository = mobInstanceRepository;
        this.abilityTimersRepository = abilityTimersRepository;
        this.random = random;
    }

    // ---- 성공

    @Test
    void fireballStartsCombatAndHits() {
        GameMap map = prepareMap();
        enter("b", "한립");
        hanbaek("a");
        placeMob(map, wolf(), 1);
        sender.clear();
        random.enqueueInts(0);

        gameConnector.handle("a", "화탄술 청모랑");

        assertThat(sender.messagesTo("a")).containsExactly(
                SystemMessage.combat(ENGAGED), SystemMessage.combat(FIRE_BALL_HIT));
        assertThat(sender.messagesTo("b")).containsExactly(
                NoticeMessage.combat("한백이 청모랑과 싸우기 시작한다.", "한백", "engage"));
        assertThat(mobInstanceRepository.findAll().getFirst().getQiBlood()).isEqualTo(41);
    }

    @Test
    void effectLinePickedByRandom() {
        GameMap map = prepareMap();
        hanbaek("a");
        placeMob(map, wolf(), 1);
        random.enqueueInts(1);

        gameConnector.handle("a", "화탄술 청모랑");

        assertThat(sender.lastTo("a", SystemMessage.class)).isEqualTo(
                SystemMessage.combat("한백의 손바닥에서 화탄이 튀어 나가 청모랑에게 터진다! (피해 8, 법력 20/25)"));
    }

    @Test
    void singleEffectLineUsesNoRandom() {
        GameMap map = prepareMap();
        Long han = hanbaek("a");
        learn(han, ironBodyInfo());
        placeMob(map, wolf(), 1);
        int before = random.intCalls();

        gameConnector.handle("a", "철권 청모랑");

        assertThat(random.intCalls()).isEqualTo(before);
    }

    /** 철권 위력 5 + 3 × 2.2 = 11.6 → 피해 7.6 → 8 */
    @Test
    void noManaCostShowsDamageOnly() {
        GameMap map = prepareMap();
        Long han = hanbaek("a");
        learn(han, ironBodyInfo());
        placeMob(map, wolf(), 1);

        gameConnector.handle("a", "철권 청모랑");

        assertThat(sender.lastTo("a", SystemMessage.class))
                .isEqualTo(SystemMessage.combat("단단히 쥔 주먹이 청모랑의 몸에 꽂힌다! (피해 8)"));
        assertThat(manaLost(han)).isZero();
    }

    @Test
    void targetOmittedInCombat() {
        GameMap map = prepareMap();
        hanbaek("a");
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "공격 청모랑");
        sender.clear();

        gameConnector.handle("a", "화탄술");

        assertThat(sender.messagesTo("a")).containsExactly(SystemMessage.combat(FIRE_BALL_HIT));
        assertThat(mobInstanceRepository.findAll().getFirst().getQiBlood()).isEqualTo(49 - 8 - 8);
    }

    @Test
    void targetByOrdinal() {
        GameMap map = prepareMap();
        hanbaek("a");
        placeMob(map, wolf(), 2);

        gameConnector.handle("a", "화탄술 2.청모랑");

        List<MobInstance> mobs = mobInstanceRepository.findAll();
        assertThat(mobs.get(0).getQiBlood()).isEqualTo(49);
        assertThat(mobs.get(1).getQiBlood()).isEqualTo(41);
    }

    @Test
    void castSwitchesTarget() {
        GameMap map = prepareMap();
        Long han = hanbaek("a");
        placeMob(map, wolf(), 1);
        placeMob(map, rabbit(), 1);
        gameConnector.handle("a", "공격 청모랑");
        gameConnector.handle("a", "공격 들토끼");
        long wolfId = mobNamed("청모랑").getId();

        gameConnector.handle("a", "화탄술 청모랑");

        assertThat(playerCombatRepository.find(characterIdOf(han)).flatMap(PlayerCombat::currentTarget))
                .contains(wolfId);
        sender.clear();
        advanceAndTick(3);
        List<String> lines = textsTo("a");
        assertThat(lines).contains("청모랑에게 일격을 날린다. (피해 8)");
        assertThat(lines).noneMatch(line -> line.startsWith("들토끼에게 일격"));
    }

    @Test
    void autoAttackStartsThreeSecondsAfterAbility() {
        GameMap map = prepareMap();
        hanbaek("a");
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "화탄술 청모랑");
        sender.clear();

        advanceAndTick(2.5);
        assertThat(textsTo("a")).noneMatch(line -> line.contains("일격"));

        advanceAndTick(0.5);
        assertThat(textsTo("a")).contains("청모랑에게 일격을 날린다. (피해 8)");
    }

    @Test
    void castInCombatKeepsAutoAttackTime() {
        GameMap map = prepareMap();
        Long han = hanbaek("a");
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "공격 청모랑");
        Instant nextAttackAt = playerCombatRepository.find(characterIdOf(han)).orElseThrow().getNextAttackAt();
        advance(1);

        gameConnector.handle("a", "화탄술");

        assertThat(playerCombatRepository.find(characterIdOf(han)).orElseThrow().getNextAttackAt())
                .isEqualTo(nextAttackAt);
    }

    /** 축기 초기 화탄술 위력 (2 + 34 × 1.2) × 1.5 = 64.2 → 피해 60.2 → 한 번에 쓰러짐 */
    @Test
    void abilityKillLeavesCorpse() {
        GameMap map = prepareMap();
        Long han = hanbaek("a");
        setRealm(han, Realm.FOUNDATION_EARLY);
        placeMob(map, wolf(), 1);
        sender.clear();

        gameConnector.handle("a", "화탄술 청모랑");

        assertThat(sender.messagesTo("a")).containsExactly(
                SystemMessage.combat(ENGAGED),
                SystemMessage.combat("손끝에서 붉은 불덩이가 피어올라 청모랑을 덮친다! (피해 60, 법력 165/170)"),
                SystemMessage.combat("청모랑을 쓰러뜨렸습니다."));
        gameConnector.handle("a", "보기");
        assertThat(sender.lastTo("a", RoomMessage.class).corpses())
                .containsExactly(new CorpseView("청모랑의 시체", true, null));
    }

    /** 위협·피해 기록에 신통 위력이 더해진다: 한립 공격 12, 한백 화탄술 12 × 2 → 처치자는 한백 */
    @Test
    void abilityDamageCountsForKiller() {
        GameMap map = prepareMap();
        Long lip = enter("b", "한립");
        setRealm(lip, Realm.QI_1);
        Long han = hanbaek("a");
        placeMob(map, wolf(), 1);
        gameConnector.handle("b", "공격 청모랑");

        gameConnector.handle("a", "화탄술 청모랑");
        advance(3);
        gameConnector.handle("a", "화탄술 청모랑");

        MobInstance mob = mobNamed("청모랑");
        assertThat(mob.engagementOf(characterIdOf(han)).orElseThrow().dealt()).isEqualTo(24);
        assertThat(mob.killer()).contains(characterIdOf(han));
    }

    @Test
    void spiritOutputAtTwentyPercentIsFull() {
        GameMap map = prepareMap();
        Long han = hanbaek("a");
        placeMob(map, wolf(), 1);
        setManaLost(han, 20);

        gameConnector.handle("a", "화탄술 청모랑");

        assertThat(sender.lastTo("a", SystemMessage.class)).isEqualTo(
                SystemMessage.combat("손끝에서 붉은 불덩이가 피어올라 청모랑을 덮친다! (피해 8, 법력 0/25)"));
    }

    /** 소모 1 신통, 남은 법력 4/25(16%) → 발휘 0.9 → 위력 (2 + 5 × 0.9 × 1.2) × 1.5 = 11.1 → 피해 7.1 */
    @Test
    void spiritOutputBelowTwentyPercent() {
        GameMap map = prepareMap();
        Long han = hanbaekWith(changchunWith(List.of(withCost(fireBall(), CostStat.MANA, "1", false))));
        placeMob(map, wolf(), 1);
        setManaLost(han, 21);

        gameConnector.handle("a", "화탄술 청모랑");

        assertThat(sender.lastTo("a", SystemMessage.class)).isEqualTo(
                SystemMessage.combat("손끝에서 붉은 불덩이가 피어올라 청모랑을 덮친다! (피해 7, 법력 3/25)"));
    }

    @Test
    void exactManaIsEnough() {
        GameMap map = prepareMap();
        Long han = hanbaek("a");
        placeMob(map, wolf(), 1);
        setManaLost(han, 20);

        gameConnector.handle("a", "화탄술 청모랑");

        assertThat(sender.lastTo("a", SystemMessage.class).tone()).isEqualTo("combat");
        assertThat(manaLost(han)).isEqualTo(25);
    }

    /** L6-2: 범인(근력 1) 철권 5 + 1 × 2.2 = 7.2 → 피해 3.2 */
    @Test
    void mortalCanUseBodyRefiningAbility() {
        GameMap map = prepareMap();
        Long han = enterMortal("a");
        learn(han, ironBodyInfo());
        placeMob(map, wolf(), 1);

        gameConnector.handle("a", "철권 청모랑");

        assertThat(sender.lastTo("a", SystemMessage.class))
                .isEqualTo(SystemMessage.combat("단단히 쥔 주먹이 청모랑의 몸에 꽂힌다! (피해 3)"));
    }

    /** 관리로 1성 기본 위력 10 → 위력 (10 + 6) × 1.5 = 24 → 피해 20 */
    @Test
    void editedDefinitionUsedNextCast() {
        GameMap map = prepareMap();
        Long han = enterQi1("a");
        Long techniqueId = learn(han, changchunInfo());
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "화탄술 청모랑");
        advance(3);

        techniqueEditor.update(techniqueId, requestOf(changchunWith(List.of(
                withStagePowers(fireBall(), stagePowers(1, 9, "10", "0.2", "1.2")), heavenEye(), windRiding()))));
        gameConnector.handle("a", "화탄술 청모랑");

        assertThat(sender.lastTo("a", SystemMessage.class)).isEqualTo(
                SystemMessage.combat("손끝에서 붉은 불덩이가 피어올라 청모랑을 덮친다! (피해 20, 법력 15/25)"));
    }

    // ---- 이름

    @Test
    void abilityNameIgnoresCase() {
        GameMap map = prepareMap();
        hanbaekWith(changchunWith(List.of(ability("Bolt", 1, AbilityKind.ATTACK, List.of(Element.FIRE),
                AbilityTarget.SINGLE, CostStat.MANA, "5", "0", "3", UsableWhen.COMBAT_ONLY))));
        placeMob(map, wolf(), 1);

        gameConnector.handle("a", "bolt 청모랑");

        assertThat(sender.lastTo("a", SystemMessage.class))
                .isEqualTo(SystemMessage.combat("Bolt 기운이 퍼진다. (피해 8, 법력 20/25)"));
    }

    @Test
    void abbreviationIsUnknown() {
        GameMap map = prepareMap();
        hanbaek("a");
        placeMob(map, wolf(), 1);

        assertRejected("a", "화탄 청모랑", UNKNOWN);
    }

    @Test
    void unknownWordIsUnknown() {
        GameMap map = prepareMap();
        hanbaek("a");
        placeMob(map, wolf(), 1);

        assertRejected("a", "뇌격 청모랑", UNKNOWN);
    }

    @Test
    void unlearnedStageRejected() {
        GameMap map = prepareMap();
        hanbaek("a");
        placeMob(map, wolf(), 1);

        assertRejected("a", "어풍결", "어풍결은 아직 깨치지 못한 신통입니다.");
    }

    @Test
    void notLearnedTechniqueRejected() {
        GameMap map = prepareMap();
        hanbaek("a");
        techniqueEditor.create(requestOf(ironBodyInfo()));
        placeMob(map, wolf(), 1);

        assertRejected("a", "철권 청모랑", "철권은 아직 깨치지 못한 신통입니다.");
    }

    /** 먼저 익힌 청운공(1성 기본 위력 10 → 피해 20)의 화탄술이 나중에 익힌 장춘공 것보다 먼저다 */
    @Test
    void firstLearnedWins() {
        GameMap map = prepareMap();
        Long han = enterQi1("a");
        learn(han, new TechniqueSaveInfo("청운공", "", List.of(Element.WOOD), Realm.MORTAL, changchunStages(),
                List.of(withStagePowers(fireBall(), stagePowers(1, 9, "10", "0.2", "1.2")))));
        learn(han, changchunInfo());
        placeMob(map, wolf(), 1);

        gameConnector.handle("a", "화탄술 청모랑");

        assertThat(sender.lastTo("a", SystemMessage.class).text()).endsWith("(피해 20, 법력 20/25)");
    }

    @Test
    void commandWinsOverAbility() {
        GameMap map = prepareMap();
        hanbaek("a");
        placeMob(map, wolf(), 1);
        sender.clear();

        gameConnector.handle("a", "공격 청모랑");

        assertThat(sender.messagesTo("a")).containsExactly(
                SystemMessage.combat(ENGAGED), SystemMessage.combat("청모랑에게 일격을 날린다. (피해 8)"));
    }

    // ---- 거절

    @Test
    void unusableFormRejected() {
        GameMap map = prepareMap();
        hanbaekWith(changchunWith(List.of(
                ability("천안술", 1, AbilityKind.DETECTION, List.of(Element.WOOD), AbilityTarget.SELF,
                        CostStat.MANA, "3", "0", "10", UsableWhen.ANYTIME),
                withCost(ability("연화술", 1, AbilityKind.ATTACK, List.of(Element.FIRE), AbilityTarget.SINGLE,
                        CostStat.MANA, "5", "0", "3", UsableWhen.COMBAT_ONLY), CostStat.MANA, "5", true))));
        placeMob(map, wolf(), 1);

        assertRejected("a", "천안술 청모랑", "천안술은 아직 쓸 수 없는 신통입니다.");
        assertRejected("a", "연화술 청모랑", "연화술은 아직 쓸 수 없는 신통입니다.");
    }

    @Test
    void targetMissingOutOfCombat() {
        GameMap map = prepareMap();
        hanbaek("a");
        placeMob(map, wolf(), 1);

        assertRejected("a", "화탄술", "누구에게 쓰시겠습니까? (예: 화탄술 늑대)");
        assertUsableRightAway("a");
    }

    @Test
    void targetNotHere() {
        GameMap map = prepareMap();
        hanbaek("a");
        placeMob(map, wolf(), 1);

        assertRejected("a", "화탄술 곰", "이곳에 '곰'은 없습니다.");
        assertUsableRightAway("a");
    }

    @Test
    void targetNpc() {
        GameMap map = prepareMap();
        prepareNpcs(map);
        hanbaek("a");

        assertRejected("a", "화탄술 왕씨", "왕씨는 공격할 수 있는 상대가 아닙니다.");
    }

    @Test
    void targetPlayer() {
        prepareMap();
        enter("b", "한립");
        hanbaek("a");

        assertRejected("a", "화탄술 한립", "다른 사람과는 아직 싸울 수 없습니다.");
    }

    @Test
    void noSpiritRejected() {
        GameMap map = prepareMap();
        Long han = enterMortal("a");
        learn(han, changchunInfo());
        placeMob(map, wolf(), 1);

        assertRejected("a", "화탄술 청모랑", "영력이 없어 술법을 펼칠 수 없습니다.");
    }

    @Test
    void globalCooldownRejected() {
        GameMap map = prepareMap();
        hanbaek("a");
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "화탄술 청모랑");
        advance(1.2);

        gameConnector.handle("a", "화탄술 청모랑");

        assertThat(sender.lastTo("a", SystemMessage.class))
                .isEqualTo(SystemMessage.error("아직 기운을 고르는 중입니다. (0.3초)"));
    }

    @Test
    void cooldownRejected() {
        GameMap map = prepareMap();
        Long han = hanbaek("a");
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "화탄술 청모랑");
        advance(1.6);

        gameConnector.handle("a", "화탄술 청모랑");
        assertThat(sender.lastTo("a", SystemMessage.class))
                .isEqualTo(SystemMessage.error("화탄술은 아직 쓸 수 없습니다. (1.4초)"));

        advance(0.05);
        gameConnector.handle("a", "화탄술 청모랑");
        assertThat(sender.lastTo("a", SystemMessage.class))
                .isEqualTo(SystemMessage.error("화탄술은 아직 쓸 수 없습니다. (1.4초)"));
        assertThat(manaLost(han)).isEqualTo(5);
    }

    @Test
    void cooldownOverAfterThreeSeconds() {
        GameMap map = prepareMap();
        Long han = hanbaek("a");
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "화탄술 청모랑");
        advance(3);

        gameConnector.handle("a", "화탄술 청모랑");

        assertThat(sender.lastTo("a", SystemMessage.class)).isEqualTo(SystemMessage.combat(
                "손끝에서 붉은 불덩이가 피어올라 청모랑을 덮친다! (피해 8, 법력 15/25)"));
        assertThat(manaLost(han)).isEqualTo(10);
    }

    /** 끊긴 캐릭터의 대기시간 기록은 공통·재사용 대기가 모두 끝난 틱에 지운다(L-D4) */
    @Test
    void expiredOfflineTimersRemoved() {
        GameMap map = prepareMap();
        Long han = hanbaek("a");
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "화탄술 청모랑");
        gameConnector.disconnect("a");

        advanceAndTick(2.5); // 공통 대기는 끝났고 재사용 대기(3초)는 남음
        assertThat(abilityTimersRepository.find(characterIdOf(han))).isPresent();

        advanceAndTick(0.5);
        assertThat(abilityTimersRepository.find(characterIdOf(han))).isEmpty();
    }

    /** 대기시간이 남은 동안의 틱은 기록을 남겨, 다시 접속해도 대기시간이 그대로다(룰북) */
    @Test
    void offlineCooldownKeptUntilExpired() {
        GameMap map = prepareMap();
        Long han = hanbaek("a");
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "화탄술 청모랑");
        gameConnector.disconnect("a");
        advanceAndTick(1.6);
        connect("a2", han);

        gameConnector.handle("a2", "화탄술 청모랑");

        assertThat(sender.lastTo("a2", SystemMessage.class))
                .isEqualTo(SystemMessage.error("화탄술은 아직 쓸 수 없습니다. (1.4초)"));
    }

    @Test
    void insufficientManaRejected() {
        GameMap map = prepareMap();
        Long han = hanbaek("a");
        placeMob(map, wolf(), 1);
        setManaLost(han, 22);

        assertRejected("a", "화탄술 청모랑", "법력이 모자랍니다. (필요 5, 남은 것 3)");
        assertThat(manaLost(han)).isEqualTo(22);
    }

    /** L6-3·F5: 필요는 소모량 그대로, 남은 것은 버린 정수 */
    @Test
    void insufficientManaShowsDecimalCost() {
        GameMap map = prepareMap();
        Long han = hanbaekWith(changchunWith(List.of(withCost(fireBall(), CostStat.MANA, "2.5", false))));
        placeMob(map, wolf(), 1);
        setManaLost(han, 22.6);

        assertRejected("a", "화탄술 청모랑", "법력이 모자랍니다. (필요 2.5, 남은 것 2)");
    }

    @Test
    void manaBelowOneShowsOne() {
        GameMap map = prepareMap();
        Long han = hanbaek("a");
        placeMob(map, wolf(), 1);
        setManaLost(han, 24.5);

        assertRejected("a", "화탄술 청모랑", "법력이 모자랍니다. (필요 5, 남은 것 1)");
    }

    /** 영력 0 캐릭터가 교전 밖에서 대상 없이: 검사 3(대상)이 4(영력)보다 먼저 */
    @Test
    void checkOrder() {
        prepareMap();
        Long han = enterMortal("a");
        learn(han, changchunInfo());

        assertRejected("a", "화탄술", "누구에게 쓰시겠습니까? (예: 화탄술 늑대)");
    }

    @Test
    void cooldownSurvivesTechniqueSave() {
        GameMap map = prepareMap();
        Long han = enterQi1("a");
        Long techniqueId = learn(han, changchunInfo());
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "화탄술 청모랑");

        techniqueEditor.update(techniqueId, requestOf(changchunInfo()));
        advance(1.6);
        gameConnector.handle("a", "화탄술 청모랑");

        assertThat(sender.lastTo("a", SystemMessage.class))
                .isEqualTo(SystemMessage.error("화탄술은 아직 쓸 수 없습니다. (1.4초)"));
    }

    @Test
    void castAllowedInCombatButMoveBlocked() {
        GameMap map = prepareMap();
        hanbaek("a");
        placeMob(map, wolf(), 1);
        gameConnector.handle("a", "공격 청모랑");

        gameConnector.handle("a", "화탄술");
        assertThat(sender.lastTo("a", SystemMessage.class)).isEqualTo(SystemMessage.combat(FIRE_BALL_HIT));

        gameConnector.handle("a", "북");
        assertThat(sender.lastTo("a", SystemMessage.class))
                .isEqualTo(SystemMessage.error("싸우는 중에는 움직일 수 없습니다."));
    }

    // ---- 도우미

    /** 연기 1층 한백이 장춘공을 익힌다 */
    private Long hanbaek(String sessionId) {
        return hanbaekWith(sessionId, changchunInfo());
    }

    private Long hanbaekWith(TechniqueSaveInfo technique) {
        return hanbaekWith("a", technique);
    }

    private Long hanbaekWith(String sessionId, TechniqueSaveInfo technique) {
        Long accountId = enterQi1(sessionId);
        learn(accountId, technique);
        return accountId;
    }

    private Long enterQi1(String sessionId) {
        Long accountId = enterMortal(sessionId);
        setRealm(accountId, Realm.QI_1);
        return accountId;
    }

    /** 범인 한백(영력 0, 근력 1) */
    private Long enterMortal(String sessionId) {
        casterAccountId = enter(sessionId, "한백");
        return casterAccountId;
    }

    /** 거절은 {@code error} 한 줄이고 소모·대기시간이 없다 */
    private void assertRejected(String sessionId, String command, String text) {
        Long characterId = characterIdOf(requireNonNull(casterAccountId));
        double manaLostBefore = characterRepository.readManaLost(characterId).orElseThrow();
        sender.clear();

        gameConnector.handle(sessionId, command);

        assertThat(sender.messagesTo(sessionId)).containsExactly(SystemMessage.error(text));
        assertThat(characterRepository.readManaLost(characterId).orElseThrow()).isEqualTo(manaLostBefore);
        assertThat(abilityTimersRepository.find(characterId)).isEmpty();
    }

    /** 거절 뒤 바로 다시 써도 공통 대기 거절이 나오지 않는다 */
    private void assertUsableRightAway(String sessionId) {
        gameConnector.handle(sessionId, "화탄술 청모랑");
        assertThat(sender.lastTo(sessionId, SystemMessage.class).tone()).isEqualTo("combat");
    }

    private MobInstance mobNamed(String name) {
        return mobInstanceRepository.findAll().stream()
                .filter(mob -> mob.getProfile().name().equals(name))
                .findFirst().orElseThrow();
    }
}
