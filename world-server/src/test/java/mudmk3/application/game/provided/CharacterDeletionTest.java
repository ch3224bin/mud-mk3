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
import mudmk3.application.character.provided.DeletedCharacter;
import mudmk3.application.character.required.CharacterRepository;
import mudmk3.application.cultivation.provided.InsightEditor;
import mudmk3.application.faction.provided.FactionEditor;
import mudmk3.application.floor.provided.FloorPlacementEditor;
import mudmk3.application.game.GameService;
import mudmk3.application.game.RecordingGameMessageSender;
import mudmk3.application.game.required.AbilityTimersRepository;
import mudmk3.application.game.required.GameMessage;
import mudmk3.application.game.required.GameMessage.PromptMessage;
import mudmk3.application.game.required.GameMessage.RoomMessage;
import mudmk3.application.game.required.GameMessage.SessionMessage;
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
import mudmk3.domain.cultivation.InsightKind;
import mudmk3.domain.shared.Realm;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static mudmk3.domain.mob.MobFixture.snake;
import static mudmk3.domain.mob.MobFixture.wolf;
import static mudmk3.domain.technique.TechniqueFixture.changchunInfo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 관리 툴의 캐릭터 삭제(admin-tool.md {@code ### 캐릭터 삭제}, platform.md {@code ### 관리 툴 삭제 월드 상태}):
 * 접속 중이면 거절하고, 아니면 게임 락 안에서 DB 행과 메모리의 교전·대기·몹 기록을 함께 지운다
 */
@ApplicationServiceTest
class CharacterDeletionTest extends BaseGameTest {

    private static final String ONLINE = "접속 중인 캐릭터는 지울 수 없습니다. 접속을 끊은 뒤 다시 시도하십시오: 한백";

    private final OfflineCharacterRemover offlineCharacterRemover;
    private final PlayerCombatRepository playerCombatRepository;
    private final AbilityTimersRepository abilityTimersRepository;
    private final MobInstanceRepository mobInstanceRepository;
    private final InsightEditor insightEditor;
    private final ScriptedRandom random;

    CharacterDeletionTest(
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
            OfflineCharacterRemover offlineCharacterRemover,
            PlayerCombatRepository playerCombatRepository,
            AbilityTimersRepository abilityTimersRepository,
            MobInstanceRepository mobInstanceRepository,
            InsightEditor insightEditor,
            ScriptedRandom random
    ) {
        super(gameConnector, gameService, sender, accountLogin, characterCreator, characterFinder, characterMover,
                characterRepository, characterRealmEditor, zoneEditor, roomEditor, worldSettingsEditor, factionEditor, npcEditor,
                itemTypeEditor, itemTypeFinder, floorPlacementEditor, inventoryFinder, inventoryEditor, mobTypeEditor,
                mobPlacementEditor, clock, entityManager);
        this.offlineCharacterRemover = offlineCharacterRemover;
        this.playerCombatRepository = playerCombatRepository;
        this.abilityTimersRepository = abilityTimersRepository;
        this.mobInstanceRepository = mobInstanceRepository;
        this.insightEditor = insightEditor;
        this.random = random;
    }

    /** 캐릭터를 가리키는 표가 먼저 지워지고(FK) 계정은 남는다 */
    @Test
    void deleteOfflineCharacter() {
        prepareMap();
        Items items = prepareItems();
        Long accountId = enter("a", "한백");
        Long characterId = characterIdOf(accountId);
        give(accountId, items.pill(), 2);
        learn(accountId, changchunInfo());
        insightEditor.add(characterId, Map.of(InsightKind.FIRE, 10.0));
        gameConnector.disconnect("a");
        entityManager.flush();
        Long inventoryId = id(inventoryFinder.find(characterId).getId());
        assertThat(count("select count(*) from character_technique where character_id = ?", characterId)).isPositive();
        assertThat(count("select count(*) from character_insight where character_id = ?", characterId)).isPositive();
        assertThat(count("select count(*) from inventory where character_id = ?", characterId)).isPositive();
        assertThat(count("select count(*) from inventory_entry where inventory_id = ?", inventoryId)).isPositive();
        assertThat(count("select count(*) from character_spirit_root where character_id = ?", characterId)).isPositive();
        assertThat(count("select count(*) from player_character where id = ?", characterId)).isPositive();

        DeletedCharacter deleted = offlineCharacterRemover.delete(characterId);
        entityManager.flush();
        entityManager.clear();

        assertThat(deleted).isEqualTo(new DeletedCharacter(characterId, accountId, "한백"));
        assertThat(count("select count(*) from character_technique where character_id = ?", characterId)).isZero();
        assertThat(count("select count(*) from character_insight where character_id = ?", characterId)).isZero();
        assertThat(count("select count(*) from inventory where character_id = ?", characterId)).isZero();
        assertThat(count("select count(*) from inventory_entry where inventory_id = ?", inventoryId)).isZero();
        assertThat(count("select count(*) from character_spirit_root where character_id = ?", characterId)).isZero();
        assertThat(count("select count(*) from player_character where id = ?", characterId)).isZero();
        assertThat(count("select count(*) from account where id = ?", accountId)).isEqualTo(1);
    }

    @Test
    void deleteOnlineFail() {
        prepareMap();
        Long accountId = enter("a", "한백");
        Long characterId = characterIdOf(accountId);
        sender.clear();

        assertThatThrownBy(() -> offlineCharacterRemover.delete(characterId))
                .isInstanceOf(CharacterOnlineException.class)
                .hasMessage(ONLINE);

        assertThat(characterFinder.findByAccountId(accountId)).isPresent();
        gameConnector.handle("a", "보기");
        assertThat(sender.messagesTo("a")).singleElement().isInstanceOf(RoomMessage.class);
    }

    /** 4차 전 캐릭터가 영근을 고르는 중이어도(세계 밖) 게임 연결이 있으면 접속 중이다 */
    @Test
    void deleteWhileChoosingRootFail() {
        GameMap map = prepareMap();
        Long accountId = prepareLegacyCharacter("한백", map.entranceId());
        connect("s1", accountId);
        Long characterId = characterIdOf(accountId);

        assertThatThrownBy(() -> offlineCharacterRemover.delete(characterId))
                .isInstanceOf(CharacterOnlineException.class)
                .hasMessage(ONLINE);
        assertThat(characterFinder.findByAccountId(accountId)).isPresent();
    }

    /** 계정은 남아 다음 접속 때 이름 prompt 부터 새로 만들고, 지운 이름을 곧바로 다시 쓸 수 있다 */
    @Test
    void reconnectAfterDeleteCreatesNew() {
        prepareMap();
        Long accountId = enter("a", "한백");
        Long characterId = characterIdOf(accountId);
        gameConnector.disconnect("a");
        offlineCharacterRemover.delete(characterId);
        entityManager.flush();
        entityManager.clear();
        sender.clear();

        connect("b", accountId);
        assertThat(sender.messagesTo("b")).containsExactly(
                new SessionMessage("creating", null), new PromptMessage("이름을 정하십시오. (한글 2~6자)"));

        gameConnector.handle("b", "한백");
        gameConnector.handle("b", "목 화");
        sender.clear();
        gameConnector.handle("b", "확인");

        List<GameMessage> messages = sender.messagesTo("b");
        assertThat(messages.get(0)).isEqualTo(new SystemMessage("캐릭터를 만들었습니다.", "info"));
        assertThat(messages.get(1)).isEqualTo(new SessionMessage("playing", "한백"));
        assertThat(characterIdOf(accountId)).isNotEqualTo(characterId);
    }

    /**
     * 끊기 전 피해 1위가 지워지면 그 기록도 몹에서 지워져, 남은 한립이 쓰러뜨릴 때 처치자가 된다(지우지 않으면 없는 캐릭터를
     * 처치자로 찾다가 틱이 실패). 수치는 {@code CombatTest.killerKeptWhenTopDealerLeft}
     */
    @Test
    void deleteClearsMobRecords() {
        GameMap map = prepareMap();
        Long hanbaek = enterAs("a", "한백", Realm.QI_13);
        enterAs("b", "한립", Realm.QI_1);
        placeMob(map, snake(), 1); // 기혈 80, 육신 10
        gameConnector.handle("a", "공격 적안 독사"); // 28.6
        gameConnector.handle("b", "공격 적안 독사"); // 7 → 44.4
        advanceAndTick(3); // 한백 28.6, 한립 7 → 8.8 (위력 합 한백 67.2)
        gameConnector.disconnect("a");
        Long hanbaekId = characterIdOf(hanbaek);
        assertThat(onlyMob().killer()).contains(hanbaekId);

        offlineCharacterRemover.delete(hanbaekId);
        sender.clear();
        advanceAndTick(6); // 한립 6초 7 → 1.8, 9초 7 → 쓰러짐

        assertThat(onlyMob().isAlive()).isFalse();
        assertThat(combatLines("b")).last().isEqualTo("적안 독사를 쓰러뜨렸습니다.");
    }

    /** 끊긴 뒤 남은 교전 기록·신통 대기시간도 지운다 */
    @Test
    void deleteClearsCombatAndTimers() {
        GameMap map = prepareMap();
        Long accountId = enterAs("a", "한백", Realm.QI_1);
        learn(accountId, changchunInfo());
        placeMob(map, wolf(), 1);
        random.enqueueInts(0);
        gameConnector.handle("a", "화탄술 청모랑");
        gameConnector.disconnect("a");
        Long characterId = characterIdOf(accountId);
        // 끊긴 뒤 틱을 돌리지 않아 틱 끝 정리가 아직 지우지 않았다
        assertThat(playerCombatRepository.find(characterId)).isPresent();
        assertThat(abilityTimersRepository.find(characterId)).isPresent();

        offlineCharacterRemover.delete(characterId);

        assertThat(playerCombatRepository.find(characterId)).isEmpty();
        assertThat(abilityTimersRepository.find(characterId)).isEmpty();
    }

    // ---- 도우미

    private Long enterAs(String sessionId, String name, Realm realm) {
        Long accountId = enter(sessionId, name);
        setRealm(accountId, realm);
        return accountId;
    }

    private MobInstance onlyMob() {
        List<MobInstance> mobs = mobInstanceRepository.findAll();
        assertThat(mobs).hasSize(1);
        return mobs.getFirst();
    }

    private long count(String sql, Long id) {
        return ((Number) entityManager.createNativeQuery(sql).setParameter(1, id).getSingleResult()).longValue();
    }

    /** 그 연결이 받은 {@code system} {@code tone: "combat"} 줄 */
    private List<String> combatLines(String sessionId) {
        return sender.messagesTo(sessionId, SystemMessage.class).stream()
                .filter(message -> message.tone().equals("combat"))
                .map(SystemMessage::text)
                .toList();
    }
}
