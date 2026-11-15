package mudmk3.application.game.provided;

import jakarta.persistence.EntityManager;
import mudmk3.ApplicationServiceTest;
import mudmk3.MutableClock;
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
import mudmk3.application.game.required.GameMessage.StatusMessage;
import mudmk3.application.game.required.GameMessage.StatusMessage.GaugeView;
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
import mudmk3.domain.combat.PlayerCombat;
import mudmk3.domain.shared.Realm;
import org.junit.jupiter.api.Test;

import static mudmk3.domain.mob.MobFixture.wolf;
import static org.assertj.core.api.Assertions.assertThat;

/** 법력 자연 회복(D104) — 연기 1층(법력 25): 초마다 0.125, 0~1초 비례 */
@ApplicationServiceTest
class ManaRecoveryTest extends BaseGameTest {

    private final PlayerCombatRepository playerCombatRepository;

    ManaRecoveryTest(
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
            PlayerCombatRepository playerCombatRepository
    ) {
        super(gameConnector, gameService, sender, accountLogin, characterCreator, characterFinder, characterMover,
                characterRepository, characterRealmEditor, zoneEditor, roomEditor, worldSettingsEditor, factionEditor, npcEditor,
                itemTypeEditor, itemTypeFinder, floorPlacementEditor, inventoryFinder, inventoryEditor, mobTypeEditor,
                mobPlacementEditor, clock, entityManager);
        this.playerCombatRepository = playerCombatRepository;
    }

    @Test
    void statusShowsCurrentMana() {
        prepareMap();
        Long han = enterAs("a", "한백", Realm.QI_1);
        setManaLost(han, 5.5);
        sender.clear();

        gameConnector.handle("a", "상태");

        StatusMessage status = sender.lastTo("a", StatusMessage.class);
        assertThat(status.text().lines().toList()).contains("기혈  60 / 60     법력  19 / 25");
        assertThat(status.stats().mana()).isEqualTo(new GaugeView(19, 25));
    }

    @Test
    void manaRegenPerSecond() {
        prepareMap();
        Long han = enterAs("a", "한백", Realm.QI_1);
        setManaLost(han, 10);

        advanceAndTick(8);

        assertThat(manaLost(han)).isEqualTo(9.0);
        gameConnector.handle("a", "상태");
        assertThat(sender.lastTo("a", StatusMessage.class).text().lines().toList())
                .contains("기혈  60 / 60     법력  16 / 25");
    }

    @Test
    void manaRegenCapsAtMax() {
        prepareMap();
        Long han = enterAs("a", "한백", Realm.QI_1);
        setManaLost(han, 0.1);

        advanceAndTick(1);

        assertThat(manaLost(han)).isZero();
    }

    @Test
    void manaRegenDuringCombat() {
        GameMap map = prepareMap();
        Long han = enterAs("a", "한백", Realm.QI_1);
        placeMob(map, wolf(), 1);
        setManaLost(han, 10);
        gameConnector.handle("a", "공격 청모랑");

        advanceAndTick(2);

        assertThat(playerCombatRepository.find(characterIdOf(han)).map(PlayerCombat::isFighting)).contains(true);
        assertThat(manaLost(han)).isEqualTo(9.75);
    }

    @Test
    void noRegenWhileOffline() {
        prepareMap();
        Long han = enterAs("a", "한백", Realm.QI_1);
        setManaLost(han, 10);
        gameConnector.disconnect("a");

        advanceAndTick(10);
        connect("a", han);

        assertThat(manaLost(han)).isEqualTo(10);
    }

    @Test
    void mortalNoRegen() {
        prepareMap();
        Long han = enter("a", "한백");
        setManaLost(han, 3);

        advanceAndTick(2);

        assertThat(manaLost(han)).isEqualTo(3);
    }

    @Test
    void noRegenWithoutLoss() {
        prepareMap();
        Long han = enterAs("a", "한백", Realm.QI_1);

        advanceAndTick(3);

        Object stored = entityManager.createNativeQuery("select mana_lost from player_character where id = :id")
                .setParameter("id", characterIdOf(han))
                .getSingleResult();
        assertThat(stored).isNull();
    }

    /** 끊긴 동안 흐른 시간은 셈하지 않는다: 다시 들어온 직후 0.5초 틱 하나는 0.5초 몫(0.0625)만 채운다 */
    @Test
    void enterStartsRegenClock() {
        prepareMap();
        Long han = enterAs("a", "한백", Realm.QI_1);
        setManaLost(han, 5.5);
        tick();
        gameConnector.disconnect("a");
        advance(5);
        connect("a", han);

        advanceAndTick(0.5);

        assertThat(manaLost(han)).isEqualTo(5.5 - 0.0625);
    }

    private Long enterAs(String sessionId, String name, Realm realm) {
        Long accountId = enter(sessionId, name);
        setRealm(accountId, realm);
        return accountId;
    }
}
