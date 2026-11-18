package mudmk3.application.character.provided;

import jakarta.persistence.EntityManager;
import mudmk3.ApplicationServiceTest;
import mudmk3.application.account.provided.AccountLogin;
import mudmk3.application.account.provided.AccountLoginRequest;
import mudmk3.application.character.CharacterModifyService;
import mudmk3.application.character.required.CharacterRepository;
import mudmk3.application.inventory.provided.InventoryFinder;
import mudmk3.application.item.ItemTypeModifyService;
import mudmk3.application.item.provided.ItemTypeEditor;
import mudmk3.application.item.provided.ItemTypeFinder;
import mudmk3.application.npc.provided.NpcEditor;
import mudmk3.application.room.provided.RoomEditor;
import mudmk3.application.world.provided.StartingItemRequest;
import mudmk3.application.world.provided.WorldSettingsEditor;
import mudmk3.application.zone.provided.ZoneEditor;
import mudmk3.domain.character.CharacterAlreadyExistsException;
import mudmk3.domain.character.CharacterName;
import mudmk3.domain.character.DuplicateCharacterNameException;
import mudmk3.domain.character.PlayerCharacter;
import mudmk3.domain.character.SpiritRoot;
import mudmk3.domain.inventory.Inventory;
import mudmk3.domain.inventory.InventoryEntry;
import mudmk3.domain.item.BagGrade;
import mudmk3.domain.item.ItemCategory;
import mudmk3.domain.item.SpiritStoneGrade;
import mudmk3.domain.room.Room;
import mudmk3.domain.shared.Realm;
import mudmk3.domain.zone.Zone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static mudmk3.domain.character.CharacterFixture.createCharacter;
import static mudmk3.domain.character.CharacterFixture.spiritRoot;
import static mudmk3.domain.item.ItemTypeFixture.createItemTypeSaveRequest;
import static mudmk3.domain.npc.NpcFixture.createNpcSaveRequest;
import static mudmk3.domain.room.RoomFixture.createRoomSaveRequest;
import static mudmk3.domain.zone.ZoneFixture.createZoneSaveRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@ApplicationServiceTest
@ExtendWith(OutputCaptureExtension.class)
record CharacterCreatorTest(
        CharacterCreator characterCreator,
        CharacterFinder characterFinder,
        CharacterModifyService characterModifyService,
        CharacterRepository characterRepository,
        AccountLogin accountLogin,
        ZoneEditor zoneEditor,
        RoomEditor roomEditor,
        WorldSettingsEditor worldSettingsEditor,
        NpcEditor npcEditor,
        InventoryFinder inventoryFinder,
        ItemTypeEditor itemTypeEditor,
        ItemTypeFinder itemTypeFinder,
        EntityManager entityManager
) {

    @Test
    void create() {
        Long startRoomId = prepareStartRoom();
        Long accountId = prepareAccount("hanbaek@example.com");

        PlayerCharacter character = characterCreator.create(accountId, "한백", spiritRoot());
        flushAndClear();

        PlayerCharacter found = characterFinder.findByAccountId(accountId).orElseThrow();
        assertThat(found.getId()).isEqualTo(character.getId());
        assertThat(found.getName()).isEqualTo(new CharacterName("한백"));
        assertThat(found.getRoomId()).isEqualTo(startRoomId);
        assertThat(characterFinder.isNameTaken("한백")).isTrue();
        assertThat(characterFinder.isNameTaken("한립")).isFalse();
        assertThat(characterFinder.isNameTaken("한 백")).isFalse();
    }

    @Test
    void createWithSpiritRoot() {
        prepareStartRoom();
        Long accountId = prepareAccount("hanbaek@example.com");

        PlayerCharacter character = characterCreator.create(accountId, "한백", spiritRoot());
        flushAndClear();

        PlayerCharacter found = characterFinder.find(character.getId());
        assertThat(found.getRealm()).isEqualTo(Realm.MORTAL);
        assertThat(found.findSpiritRoot()).map(SpiritRoot::name).contains("목·화 진영근");
    }

    @Test
    void createWithSpiritRootChecksInSameOrder() {
        prepareStartRoom();
        Long accountId = prepareAccount("hanbaek@example.com");
        characterCreator.create(accountId, "한백", spiritRoot());

        assertThatThrownBy(() -> characterCreator.create(accountId, "한 백", spiritRoot()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이름은 띄어쓰기 없이 한글 2~6자로 정하십시오.");
        assertThatThrownBy(() -> characterCreator.create(accountId, "한백", spiritRoot()))
                .isInstanceOf(CharacterAlreadyExistsException.class);
        assertThatThrownBy(() -> characterCreator.create(prepareAccount("hanli@example.com"), "한백", spiritRoot()))
                .isInstanceOf(DuplicateCharacterNameException.class);
    }

    @Test
    void chooseSpiritRootForLegacy() {
        Long startRoomId = prepareStartRoom();
        // 4차 전에 만든 캐릭터: 영근이 없다
        PlayerCharacter character = characterRepository.save(
                createCharacter(prepareAccount("hanbaek@example.com"), "한백", startRoomId));
        flushAndClear();
        assertThat(characterFinder.find(character.getId()).findSpiritRoot()).isEmpty();

        characterCreator.chooseSpiritRoot(character.getId(), SpiritRoot.parse("금").orElseThrow());
        flushAndClear();

        assertThat(characterFinder.find(character.getId()).findSpiritRoot())
                .map(SpiritRoot::summary).contains("금 100");
    }

    @Test
    void chooseSpiritRootTwiceFail() {
        prepareStartRoom();
        PlayerCharacter character = characterCreator.create(prepareAccount("hanbaek@example.com"), "한백", spiritRoot());
        SpiritRoot other = SpiritRoot.parse("금").orElseThrow();

        assertThatThrownBy(() -> characterCreator.chooseSpiritRoot(character.getId(), other))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("영근은 이미 정했습니다");
    }

    @Test
    void findUnknownFail() {
        assertThatThrownBy(() -> characterFinder.find(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("캐릭터를 찾을 수 없습니다. id: -1");
        assertThatThrownBy(() -> characterCreator.chooseSpiritRoot(-1L, spiritRoot()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("캐릭터를 찾을 수 없습니다. id: -1");
    }

    @Test
    void fillMissingRealmsOnStartupListener() throws NoSuchMethodException {
        prepareStartRoom();
        PlayerCharacter character = characterCreator.create(prepareAccount("hanbaek@example.com"), "한백", spiritRoot());
        entityManager.flush();
        entityManager.createNativeQuery("update player_character set realm = null where id = ?")
                .setParameter(1, character.getId())
                .executeUpdate();
        entityManager.clear();

        characterModifyService.fillMissingRealms();
        entityManager.clear();

        assertThat(characterFinder.find(character.getId()).getRealm()).isEqualTo(Realm.QI_1);
        Method fill = CharacterModifyService.class.getMethod("fillMissingRealms");
        assertThat(fill.getAnnotation(EventListener.class)).isNotNull();
        assertThat(fill.getAnnotation(Order.class).value()).isZero();
        Method seed = ItemTypeModifyService.class.getMethod("seedSpiritStones");
        assertThat(seed.getAnnotation(EventListener.class)).isNotNull();
        assertThat(seed.getAnnotation(Order.class).value()).isEqualTo(10);
    }

    @Test
    void findByAccountIdWithoutCharacter() {
        Long accountId = prepareAccount("hanbaek@example.com");

        assertThat(characterFinder.findByAccountId(accountId)).isEmpty();
    }

    @Test
    void duplicateNameFail() {
        prepareStartRoom();
        characterCreator.create(prepareAccount("hanbaek@example.com"), "한백", spiritRoot());
        Long otherAccountId = prepareAccount("hanli@example.com");

        assertThatThrownBy(() -> characterCreator.create(otherAccountId, "한백", spiritRoot()))
                .isInstanceOf(DuplicateCharacterNameException.class)
                .hasMessage("이미 있는 이름입니다. 다른 이름을 정하십시오.");
    }

    @Test
    void createCharacterWithNpcNameFail() {
        prepareStartRoom();
        npcEditor.create(createNpcSaveRequest("왕씨", null));
        Long accountId = prepareAccount("hanbaek@example.com");

        assertThat(characterFinder.isNameTaken("왕씨")).isTrue();
        assertThatThrownBy(() -> characterCreator.create(accountId, "왕씨", spiritRoot()))
                .isInstanceOf(DuplicateCharacterNameException.class)
                .hasMessage("이미 있는 이름입니다. 다른 이름을 정하십시오.");
        assertThat(characterFinder.findByAccountId(accountId)).isEmpty();
    }

    @Test
    void secondCharacterForAccountFail() {
        prepareStartRoom();
        Long accountId = prepareAccount("hanbaek@example.com");
        characterCreator.create(accountId, "한백", spiritRoot());

        assertThatThrownBy(() -> characterCreator.create(accountId, "한립", spiritRoot()))
                .isInstanceOf(CharacterAlreadyExistsException.class)
                .hasMessage("이미 캐릭터가 있습니다");
        // 계정 검사가 이름 중복 검사보다 먼저다
        assertThatThrownBy(() -> characterCreator.create(accountId, "한백", spiritRoot()))
                .isInstanceOf(CharacterAlreadyExistsException.class);
    }

    @Test
    void invalidNameFail() {
        prepareStartRoom();
        Long accountId = prepareAccount("hanbaek@example.com");

        assertThatThrownBy(() -> characterCreator.create(accountId, "한 백", spiritRoot()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이름은 띄어쓰기 없이 한글 2~6자로 정하십시오.");
        assertThat(characterFinder.findByAccountId(accountId)).isEmpty();
    }

    @Test
    void noStartRoomFail() {
        Long accountId = prepareAccount("hanbaek@example.com");

        assertThatThrownBy(() -> characterCreator.create(accountId, "한백", spiritRoot()))
                .isInstanceOf(WorldNotReadyException.class)
                .hasMessage("아직 세계가 열리지 않았습니다. 관리자가 시작 방을 정해야 합니다.");
        assertThat(characterFinder.findByAccountId(accountId)).isEmpty();
    }

    @Test
    void unknownAccountFail() {
        prepareStartRoom();

        assertThatThrownBy(() -> characterCreator.create(-1L, "한백", spiritRoot()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("계정을 찾을 수 없습니다. id: -1");
    }

    @Test
    void createGivesStartingItems() {
        prepareStartRoom();
        Long bag = itemTypeEditor.create(createItemTypeSaveRequest("하급 저물대", ItemCategory.BAG, 10, BagGrade.LOW)).getId();
        Long lowStone = itemTypeFinder.spiritStone(SpiritStoneGrade.LOW).getId();
        Long healPill = itemTypeEditor.create(createItemTypeSaveRequest("회춘단(연기 초)", ItemCategory.PILL, 3)).getId();
        Long qiPill = itemTypeEditor.create(createItemTypeSaveRequest("회기단(연기 초)", ItemCategory.PILL, 3)).getId();
        worldSettingsEditor.replaceStartingItems(List.of(new StartingItemRequest(bag, 1), new StartingItemRequest(lowStone, 30),
                new StartingItemRequest(healPill, 3), new StartingItemRequest(qiPill, 3)));

        PlayerCharacter character = characterCreator.create(prepareAccount("hanbaek@example.com"), "한백", spiritRoot());
        flushAndClear();

        Inventory inventory = inventoryFinder.find(character.getId());
        assertThat(inventory.getId()).isNotNull();
        assertThat(inventory.usedSlots()).isEqualTo(3);
        assertThat(inventory.capacity(false)).isEqualTo(22);
        assertThat(inventory.spiritStones()).isEqualTo(Map.of(SpiritStoneGrade.LOW, 30L));
        assertThat(inventory.getEntries())
                .extracting(entry -> entry.getItemType().getName(), InventoryEntry::getCount)
                .containsExactly(
                        tuple("하품 영석", 30),
                        tuple("하급 저물대", 1),
                        tuple("회춘단(연기 초)", 3),
                        tuple("회기단(연기 초)", 3));
    }

    @Test
    void createGivesStartingItemsWithBagLast() {
        prepareStartRoom();
        Long sword = itemTypeEditor.create(createItemTypeSaveRequest("비검", ItemCategory.ARTIFACT, 50)).getId();
        Long healPill = itemTypeEditor.create(createItemTypeSaveRequest("회춘단(연기 초)", ItemCategory.PILL, 3)).getId();
        Long bag = itemTypeEditor.create(createItemTypeSaveRequest("하급 저물대", ItemCategory.BAG, 10, BagGrade.LOW)).getId();
        // 저물대 없이는 칸(2)을 넘지만 다 넣은 뒤로는 들어간다
        worldSettingsEditor.replaceStartingItems(List.of(new StartingItemRequest(sword, 2),
                new StartingItemRequest(healPill, 3), new StartingItemRequest(bag, 1)));

        PlayerCharacter character = characterCreator.create(prepareAccount("hanbaek@example.com"), "한백", spiritRoot());
        flushAndClear();

        Inventory inventory = inventoryFinder.find(character.getId());
        assertThat(inventory.usedSlots()).isEqualTo(3);
        assertThat(inventory.getEntries()).extracting(entry -> entry.getItemType().getName())
                .containsExactly("비검", "회춘단(연기 초)", "하급 저물대");
    }

    @Test
    void createWithoutStartingItems() {
        prepareStartRoom();

        PlayerCharacter character = characterCreator.create(prepareAccount("hanbaek@example.com"), "한백", spiritRoot());
        flushAndClear();

        Inventory inventory = inventoryFinder.find(character.getId());
        assertThat(inventory.getId()).isNotNull();
        assertThat(inventory.usedSlots()).isZero();
        assertThat(inventory.capacity(false)).isEqualTo(2);
        assertThat(inventory.getEntries()).isEmpty();
    }

    @Test
    void changedStartingItemsGoToLaterCharacters() {
        prepareStartRoom();
        Long healPill = itemTypeEditor.create(createItemTypeSaveRequest("회춘단(연기 초)", ItemCategory.PILL, 3)).getId();
        PlayerCharacter first = characterCreator.create(prepareAccount("hanbaek@example.com"), "한백", spiritRoot());

        worldSettingsEditor.replaceStartingItems(List.of(new StartingItemRequest(healPill, 5)));
        PlayerCharacter second = characterCreator.create(prepareAccount("hanli@example.com"), "한립", spiritRoot());
        flushAndClear();

        assertThat(inventoryFinder.find(first.getId()).getEntries()).isEmpty();
        assertThat(inventoryFinder.find(second.getId()).getEntries())
                .extracting(InventoryEntry::getCount).containsExactly(5);
    }

    @Test
    void createSkipsDeletedStartingItem(CapturedOutput output) {
        prepareStartRoom();
        Long healPill = itemTypeEditor.create(createItemTypeSaveRequest("회춘단(연기 초)", ItemCategory.PILL, 3)).getId();
        Long deleted = itemTypeEditor.create(createItemTypeSaveRequest("사라진 단약", ItemCategory.PILL, 3)).getId();
        Long bag = itemTypeEditor.create(createItemTypeSaveRequest("하급 저물대", ItemCategory.BAG, 10, BagGrade.LOW)).getId();
        Long qiPill = itemTypeEditor.create(createItemTypeSaveRequest("회기단(연기 초)", ItemCategory.PILL, 3)).getId();
        worldSettingsEditor.replaceStartingItems(List.of(new StartingItemRequest(healPill, 3),
                new StartingItemRequest(deleted, 1), new StartingItemRequest(bag, 1), new StartingItemRequest(qiPill, 2)));
        // 시작 소지품에 남은 채로 물건 종류만 지워진 상태(서비스는 이렇게 지우지 못하게 막는다)
        flushAndClear();
        entityManager.createNativeQuery("delete from item_type where id = ?").setParameter(1, deleted).executeUpdate();

        PlayerCharacter character = characterCreator.create(prepareAccount("hanbaek@example.com"), "한백", spiritRoot());
        flushAndClear();

        assertThat(characterFinder.find(character.getId()).getName()).isEqualTo(new CharacterName("한백"));
        assertThat(inventoryFinder.find(character.getId()).getEntries())
                .extracting(entry -> entry.getItemType().getName(), InventoryEntry::getCount)
                .containsExactly(
                        tuple("회춘단(연기 초)", 3),
                        tuple("하급 저물대", 1),
                        tuple("회기단(연기 초)", 2));
        assertThat(output).contains("시작 소지품의 물건 종류가 없어 건너뜁니다. id: " + deleted);
    }

    @Test
    void createGivesStartingItemsThatFitWhenDeletedBagLeavesNoRoom(CapturedOutput output) {
        prepareStartRoom();
        Long bag = itemTypeEditor.create(createItemTypeSaveRequest("하급 저물대", ItemCategory.BAG, 10, BagGrade.LOW)).getId();
        Long healPill = itemTypeEditor.create(createItemTypeSaveRequest("회춘단(연기 초)", ItemCategory.PILL, 3)).getId();
        Long sword = itemTypeEditor.create(createItemTypeSaveRequest("비검", ItemCategory.ARTIFACT, 50)).getId();
        Long qiPill = itemTypeEditor.create(createItemTypeSaveRequest("회기단(연기 초)", ItemCategory.PILL, 3)).getId();
        worldSettingsEditor.replaceStartingItems(List.of(new StartingItemRequest(bag, 1), new StartingItemRequest(healPill, 3),
                new StartingItemRequest(sword, 2), new StartingItemRequest(qiPill, 3)));
        // 저물대가 지워져 기본 2칸에 회춘단(1칸)·비검(2칸)·회기단(1칸)이 다 들어가지 않는다
        flushAndClear();
        entityManager.createNativeQuery("delete from item_type where id = ?").setParameter(1, bag).executeUpdate();

        PlayerCharacter character = characterCreator.create(prepareAccount("hanbaek@example.com"), "한백", spiritRoot());
        flushAndClear();

        assertThat(characterFinder.find(character.getId()).getName()).isEqualTo(new CharacterName("한백"));
        assertThat(inventoryFinder.find(character.getId()).getEntries())
                .extracting(entry -> entry.getItemType().getName(), InventoryEntry::getCount)
                .containsExactly(
                        tuple("회춘단(연기 초)", 3),
                        tuple("회기단(연기 초)", 3));
        assertThat(output).contains("시작 소지품의 물건 종류가 없어 건너뜁니다. id: " + bag)
                .contains("시작 소지품이 칸에 들어가지 않아 건너뜁니다. id: " + sword + ", 수: 2");
    }

    private Long prepareStartRoom() {
        Zone zone = zoneEditor.create(createZoneSaveRequest("청석숲"));
        Room room = roomEditor.create(createRoomSaveRequest(zone.getId(), "청석숲 오솔길"));
        worldSettingsEditor.designateStartRoom(room.getId());
        return room.getId();
    }

    private Long prepareAccount(String email) {
        return accountLogin.login(new AccountLoginRequest("google-" + email, email)).accountId();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
