package mudmk3.application.character.provided;

import jakarta.persistence.EntityManager;
import mudmk3.ApplicationServiceTest;
import mudmk3.application.account.provided.AccountFinder;
import mudmk3.application.account.provided.AccountLogin;
import mudmk3.application.account.provided.AccountLoginRequest;
import mudmk3.application.inventory.provided.InventoryFinder;
import mudmk3.application.item.provided.ItemTypeEditor;
import mudmk3.application.room.provided.RoomEditor;
import mudmk3.application.world.provided.StartingItemRequest;
import mudmk3.application.world.provided.WorldSettingsEditor;
import mudmk3.application.zone.provided.ZoneEditor;
import mudmk3.domain.character.CharacterName;
import mudmk3.domain.character.PlayerCharacter;
import mudmk3.domain.inventory.Inventory;
import mudmk3.domain.item.ItemCategory;
import mudmk3.domain.room.Room;
import mudmk3.domain.zone.Zone;
import org.junit.jupiter.api.Test;

import java.util.List;

import static mudmk3.domain.character.CharacterFixture.spiritRoot;
import static mudmk3.domain.item.ItemTypeFixture.createItemTypeSaveRequest;
import static mudmk3.domain.room.RoomFixture.createRoomSaveRequest;
import static mudmk3.domain.zone.ZoneFixture.createZoneSaveRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ApplicationServiceTest
record CharacterRemoverTest(
        CharacterRemover characterRemover,
        CharacterCreator characterCreator,
        CharacterFinder characterFinder,
        AccountLogin accountLogin,
        AccountFinder accountFinder,
        InventoryFinder inventoryFinder,
        ItemTypeEditor itemTypeEditor,
        ZoneEditor zoneEditor,
        RoomEditor roomEditor,
        WorldSettingsEditor worldSettingsEditor,
        EntityManager entityManager
) {

    @Test
    void delete() {
        prepareWorld();
        Long accountId = accountLogin.login(new AccountLoginRequest("google-hanbaek", "hanbaek@example.com")).accountId();
        PlayerCharacter character = characterCreator.create(accountId, "한백", spiritRoot());
        Long characterId = character.getId();
        flushAndClear();
        assertThat(inventoryFinder.find(characterId).getEntries()).hasSize(1);

        DeletedCharacter deleted = characterRemover.delete(characterId);
        flushAndClear();

        assertThat(deleted).isEqualTo(new DeletedCharacter(characterId, accountId, "한백"));
        assertThat(characterFinder.findByAccountId(accountId)).isEmpty();
        // 소지품이 없으면 저장하지 않은 빈 소지품이다
        Inventory inventory = inventoryFinder.find(characterId);
        assertThat(inventory.getId()).isNull();
        assertThat(inventory.getEntries()).isEmpty();
        assertThat(accountFinder.find(accountId).getId()).isEqualTo(accountId);

        PlayerCharacter recreated = characterCreator.create(accountId, "한백", spiritRoot());
        flushAndClear();
        assertThat(characterFinder.find(recreated.getId()).getName()).isEqualTo(new CharacterName("한백"));
    }

    @Test
    void deleteFail() {
        assertThatThrownBy(() -> characterRemover.delete(9L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("캐릭터를 찾을 수 없습니다. id: 9");
    }

    /** 시작 방과 시작 소지품(회춘단 3) */
    private void prepareWorld() {
        Zone zone = zoneEditor.create(createZoneSaveRequest("청석숲"));
        Room room = roomEditor.create(createRoomSaveRequest(zone.getId(), "청석숲 오솔길"));
        worldSettingsEditor.designateStartRoom(room.getId());
        Long healPill = itemTypeEditor.create(createItemTypeSaveRequest("회춘단(연기 초)", ItemCategory.PILL, 3)).getId();
        worldSettingsEditor.replaceStartingItems(List.of(new StartingItemRequest(healPill, 3)));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
