package mudmk3.application.item.provided;

import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import mudmk3.ApplicationServiceTest;
import mudmk3.MutableClock;
import mudmk3.application.floor.provided.FloorPlacementEditor;
import mudmk3.application.floor.provided.RoomFloor;
import mudmk3.application.inventory.provided.InventoryEditor;
import mudmk3.application.npc.provided.NpcEditor;
import mudmk3.application.npc.provided.ShopItemRequest;
import mudmk3.application.npc.provided.ShopSaveRequest;
import mudmk3.application.room.provided.RoomEditor;
import mudmk3.application.world.provided.StartingItemRequest;
import mudmk3.application.world.provided.WorldSettingsEditor;
import mudmk3.application.zone.provided.ZoneEditor;
import mudmk3.domain.item.BagGrade;
import mudmk3.domain.item.DuplicateItemTypeNameException;
import mudmk3.domain.item.ItemCategory;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.item.SpiritStoneGrade;
import mudmk3.domain.npc.ShopKind;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static mudmk3.domain.floor.FloorFixture.createFloorPlacementSaveRequest;
import static mudmk3.domain.item.ItemTypeFixture.createItemTypeSaveRequest;
import static mudmk3.domain.npc.NpcFixture.createNpcSaveRequest;
import static mudmk3.domain.room.RoomFixture.createRoomSaveRequest;
import static mudmk3.domain.zone.ZoneFixture.createZoneSaveRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@ApplicationServiceTest
record ItemTypeEditorTest(ItemTypeEditor itemTypeEditor, ItemTypeFinder itemTypeFinder, InventoryEditor inventoryEditor,
                          WorldSettingsEditor worldSettingsEditor, FloorPlacementEditor floorPlacementEditor,
                          RoomFloor roomFloor, ZoneEditor zoneEditor, RoomEditor roomEditor, NpcEditor npcEditor,
                          MutableClock clock, EntityManager entityManager) {

    @Test
    void spiritStonesSeeded() {
        assertThat(SpiritStoneGrade.values())
                .extracting(grade -> itemTypeFinder.spiritStone(grade))
                .extracting(ItemType::getName, ItemType::getBaseValue, ItemType::getCategory)
                .containsExactly(
                        tuple("하품 영석", 1L, ItemCategory.SPIRIT_STONE),
                        tuple("중품 영석", 100L, ItemCategory.SPIRIT_STONE),
                        tuple("상품 영석", 10_000L, ItemCategory.SPIRIT_STONE),
                        tuple("극품 영석", 1_000_000L, ItemCategory.SPIRIT_STONE));
        assertThat(itemTypeFinder.spiritStone(SpiritStoneGrade.LOW).getDescription())
                .isEqualTo("영기가 옅게 서린 돌. 수선자들이 값을 치르는 데 쓴다.");
    }

    @Test
    void create() {
        ItemType itemType = itemTypeEditor.create(new ItemTypeSaveRequest(
                "  늑대   가죽 ", "거칠고 질긴 늑대의 가죽이다.", ItemCategory.MATERIAL, 2, null));
        entityManager.flush();
        entityManager.clear();

        ItemType found = itemTypeFinder.find(itemType.getId());
        assertThat(found.getName()).isEqualTo("늑대 가죽");
        assertThat(found.getDescription()).isEqualTo("거칠고 질긴 늑대의 가죽이다.");
        assertThat(found.getCategory()).isEqualTo(ItemCategory.MATERIAL);
        assertThat(found.getBaseValue()).isEqualTo(2);
    }

    @Test
    void createBag() {
        ItemType bag = itemTypeEditor.create(createItemTypeSaveRequest("하급 저물대", ItemCategory.BAG, 10, BagGrade.LOW));

        assertThat(bag.bagSlots()).isEqualTo(20);
    }

    @Test
    void createInvalidFail() {
        assertThatThrownBy(() -> itemTypeEditor.create(new ItemTypeSaveRequest("   ", null, ItemCategory.PILL, 1, null)))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> itemTypeEditor.create(new ItemTypeSaveRequest("회춘단", null, null, 1, null)))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> itemTypeEditor.create(new ItemTypeSaveRequest("가".repeat(21), null, ItemCategory.PILL, 1, null)))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> itemTypeEditor.create(new ItemTypeSaveRequest("회춘단", "가".repeat(501), ItemCategory.PILL, 1, null)))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> itemTypeEditor.create(new ItemTypeSaveRequest("회춘단", null, ItemCategory.PILL, -1, null)))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> itemTypeEditor.create(new ItemTypeSaveRequest("회춘단", null, ItemCategory.PILL, 100_000_001, null)))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void createSpiritStoneCategoryFail() {
        assertThatThrownBy(() -> itemTypeEditor.create(createItemTypeSaveRequest("영석", ItemCategory.SPIRIT_STONE, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("영석 종류는 만들 수 없습니다.");
    }

    @Test
    void duplicateNameIgnoringCaseFail() {
        itemTypeEditor.create(createItemTypeSaveRequest("Jade Slip", ItemCategory.SCROLL, 1));

        assertThatThrownBy(() -> itemTypeEditor.create(createItemTypeSaveRequest(" jade  slip", ItemCategory.SCROLL, 1)))
                .isInstanceOf(DuplicateItemTypeNameException.class)
                .hasMessage("이미 있는 물건 이름입니다: jade slip");
        assertThatThrownBy(() -> itemTypeEditor.create(createItemTypeSaveRequest("하품 영석", ItemCategory.MATERIAL, 1)))
                .isInstanceOf(DuplicateItemTypeNameException.class)
                .hasMessage("이미 있는 물건 이름입니다: 하품 영석");
    }

    @Test
    void update() {
        ItemType itemType = itemTypeEditor.create(createItemTypeSaveRequest("회춘단", ItemCategory.PILL, 3));

        itemTypeEditor.update(itemType.getId(), new ItemTypeSaveRequest("회기단", null, ItemCategory.ELIXIR, 4, null));
        entityManager.flush();
        entityManager.clear();

        ItemType found = itemTypeFinder.find(itemType.getId());
        assertThat(found.getName()).isEqualTo("회기단");
        assertThat(found.getDescription()).isEmpty();
        assertThat(found.getCategory()).isEqualTo(ItemCategory.ELIXIR);
        assertThat(found.getBaseValue()).isEqualTo(4);
    }

    @Test
    void updateKeepsOwnName() {
        ItemType itemType = itemTypeEditor.create(createItemTypeSaveRequest("Jade Slip", ItemCategory.SCROLL, 1));

        ItemType updated = itemTypeEditor.update(itemType.getId(),
                new ItemTypeSaveRequest("jade slip", "바뀐 묘사다.", ItemCategory.SCROLL, 1, null));

        assertThat(updated.getName()).isEqualTo("jade slip");
        assertThat(updated.getDescription()).isEqualTo("바뀐 묘사다.");
    }

    @Test
    void updateToOtherNameFail() {
        itemTypeEditor.create(createItemTypeSaveRequest("회춘단", ItemCategory.PILL, 3));
        ItemType other = itemTypeEditor.create(createItemTypeSaveRequest("회기단", ItemCategory.PILL, 4));

        assertThatThrownBy(() -> itemTypeEditor.update(other.getId(), createItemTypeSaveRequest("회춘단", ItemCategory.PILL, 4)))
                .isInstanceOf(DuplicateItemTypeNameException.class)
                .hasMessage("이미 있는 물건 이름입니다: 회춘단");
    }

    @Test
    void updateSpiritStoneFail() {
        ItemType stone = itemTypeFinder.spiritStone(SpiritStoneGrade.LOW);

        assertThatThrownBy(() -> itemTypeEditor.update(stone.getId(), createItemTypeSaveRequest("돌", ItemCategory.MATERIAL, 1)))
                .isInstanceOf(SpiritStoneChangeException.class)
                .hasMessage("영석은 엔진이 정한 물건이라 고치거나 지울 수 없습니다.");
    }

    @Test
    void deleteSpiritStoneFail() {
        ItemType stone = itemTypeFinder.spiritStone(SpiritStoneGrade.TOP);

        assertThatThrownBy(() -> itemTypeEditor.delete(stone.getId()))
                .isInstanceOf(SpiritStoneChangeException.class)
                .hasMessage("영석은 엔진이 정한 물건이라 고치거나 지울 수 없습니다.");
    }

    @Test
    void delete() {
        ItemType itemType = itemTypeEditor.create(createItemTypeSaveRequest("회춘단", ItemCategory.PILL, 3));

        itemTypeEditor.delete(itemType.getId());
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> itemTypeFinder.find(itemType.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unknownIdFail() {
        assertThatThrownBy(() -> itemTypeFinder.find(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("물건 종류를 찾을 수 없습니다. id: 999");
        assertThatThrownBy(() -> itemTypeEditor.update(999L, createItemTypeSaveRequest("회춘단", ItemCategory.PILL, 3)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("물건 종류를 찾을 수 없습니다. id: 999");
        assertThatThrownBy(() -> itemTypeEditor.delete(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("물건 종류를 찾을 수 없습니다. id: 999");
    }

    @Test
    void findAllOrder() {
        itemTypeEditor.create(createItemTypeSaveRequest("회춘단", ItemCategory.PILL, 3));
        itemTypeEditor.create(createItemTypeSaveRequest("늑대 가죽", ItemCategory.MATERIAL, 2));
        itemTypeEditor.create(createItemTypeSaveRequest("비검", ItemCategory.ARTIFACT, 50));

        assertThat(itemTypeFinder.findAll()).extracting(ItemType::getName)
                .containsExactly("하품 영석", "중품 영석", "상품 영석", "극품 영석", "늑대 가죽", "비검", "회춘단");
    }

    @Test
    void findAllById() {
        ItemType pill = itemTypeEditor.create(createItemTypeSaveRequest("회춘단", ItemCategory.PILL, 3));
        ItemType hide = itemTypeEditor.create(createItemTypeSaveRequest("늑대 가죽", ItemCategory.MATERIAL, 2));

        Map<Long, ItemType> found = itemTypeFinder.findAllById(List.of(pill.getId(), hide.getId(), 999L));

        assertThat(found).containsOnlyKeys(pill.getId(), hide.getId());
        assertThat(found.get(hide.getId()).getName()).isEqualTo("늑대 가죽");
        assertThat(itemTypeFinder.findAllById(List.of())).isEmpty();
    }

    @Test
    void deleteItemTypeInInventoryFail() {
        ItemType itemType = itemTypeEditor.create(createItemTypeSaveRequest("회춘단", ItemCategory.PILL, 3));
        inventoryEditor.addUpTo(1L, itemType.getId(), 1);

        assertThatThrownBy(() -> itemTypeEditor.delete(itemType.getId()))
                .isInstanceOf(ItemTypeInUseException.class)
                .hasMessage("쓰이는 물건은 지울 수 없습니다: 회춘단");
    }

    @Test
    void changeCategoryInInventoryFail() {
        ItemType itemType = itemTypeEditor.create(createItemTypeSaveRequest("회춘단", ItemCategory.PILL, 3));
        ItemType bag = itemTypeEditor.create(createItemTypeSaveRequest("하급 저물대", ItemCategory.BAG, 10, BagGrade.LOW));
        inventoryEditor.addUpTo(1L, itemType.getId(), 1);
        inventoryEditor.addUpTo(1L, bag.getId(), 1);

        assertThatThrownBy(() -> itemTypeEditor.update(itemType.getId(), createItemTypeSaveRequest("회춘단", ItemCategory.MATERIAL, 3)))
                .isInstanceOf(ItemTypeInUseException.class)
                .hasMessage("쓰이는 물건은 종류·저물대 등급을 바꿀 수 없습니다: 회춘단");
        assertThatThrownBy(() -> itemTypeEditor.update(bag.getId(),
                createItemTypeSaveRequest("하급 저물대", ItemCategory.BAG, 10, BagGrade.HIGH)))
                .isInstanceOf(ItemTypeInUseException.class);
    }

    @Test
    void deleteItemTypeInStartingItemsFail() {
        ItemType itemType = itemTypeEditor.create(createItemTypeSaveRequest("회춘단", ItemCategory.PILL, 3));
        worldSettingsEditor.replaceStartingItems(List.of(new StartingItemRequest(itemType.getId(), 3)));

        assertThatThrownBy(() -> itemTypeEditor.delete(itemType.getId()))
                .isInstanceOf(ItemTypeInUseException.class)
                .hasMessage("쓰이는 물건은 지울 수 없습니다: 회춘단");
        assertThatThrownBy(() -> itemTypeEditor.update(itemType.getId(), createItemTypeSaveRequest("회춘단", ItemCategory.MATERIAL, 3)))
                .isInstanceOf(ItemTypeInUseException.class)
                .hasMessage("쓰이는 물건은 종류·저물대 등급을 바꿀 수 없습니다: 회춘단");
    }

    @Test
    void deleteItemTypeRemovedFromStartingItems() {
        ItemType itemType = itemTypeEditor.create(createItemTypeSaveRequest("회춘단", ItemCategory.PILL, 3));
        worldSettingsEditor.replaceStartingItems(List.of(new StartingItemRequest(itemType.getId(), 3)));
        worldSettingsEditor.replaceStartingItems(List.of());

        itemTypeEditor.delete(itemType.getId());

        assertThat(itemTypeFinder.findAllById(List.of(itemType.getId()))).isEmpty();
    }

    @Test
    void renameInInventory() {
        ItemType itemType = itemTypeEditor.create(createItemTypeSaveRequest("회춘단", ItemCategory.PILL, 3));
        inventoryEditor.addUpTo(1L, itemType.getId(), 1);

        itemTypeEditor.update(itemType.getId(), new ItemTypeSaveRequest("회춘단(연기 초)", "바뀐 묘사다.", ItemCategory.PILL, 5, null));
        entityManager.flush();
        entityManager.clear();

        assertThat(itemTypeFinder.find(itemType.getId()).getName()).isEqualTo("회춘단(연기 초)");
    }

    @Test
    void deleteItemTypeOnFloorFail() {
        ItemType placed = itemTypeEditor.create(createItemTypeSaveRequest("늑대 가죽", ItemCategory.MATERIAL, 2));
        ItemType dropped = itemTypeEditor.create(createItemTypeSaveRequest("회춘단", ItemCategory.PILL, 3));
        Long zoneId = zoneEditor.create(createZoneSaveRequest("청석숲")).getId();
        Long roomId = roomEditor.create(createRoomSaveRequest(zoneId, "샘터")).getId();
        floorPlacementEditor.create(roomId, createFloorPlacementSaveRequest(placed.getId(), 1, null));
        // 다 주워 남은 수가 0이어도 배치는 쓰임으로 센다
        roomFloor.take(roomId, placed.getId(), 1);
        roomFloor.drop(roomId, dropped.getId(), 1);
        clock.advance(Duration.ofMinutes(4).plusSeconds(59));

        assertThatThrownBy(() -> itemTypeEditor.delete(placed.getId()))
                .isInstanceOf(ItemTypeInUseException.class)
                .hasMessage("쓰이는 물건은 지울 수 없습니다: 늑대 가죽");
        assertThatThrownBy(() -> itemTypeEditor.delete(dropped.getId()))
                .isInstanceOf(ItemTypeInUseException.class)
                .hasMessage("쓰이는 물건은 지울 수 없습니다: 회춘단");
        assertThatThrownBy(() -> itemTypeEditor.update(dropped.getId(), createItemTypeSaveRequest("회춘단", ItemCategory.MATERIAL, 3)))
                .isInstanceOf(ItemTypeInUseException.class)
                .hasMessage("쓰이는 물건은 종류·저물대 등급을 바꿀 수 없습니다: 회춘단");
    }

    @Test
    void deleteItemTypeWithOnlyExpiredDropsSucceeds() {
        ItemType itemType = itemTypeEditor.create(createItemTypeSaveRequest("회춘단", ItemCategory.PILL, 3));
        roomFloor.drop(3L, itemType.getId(), 2);
        entityManager.flush();
        clock.advance(Duration.ofMinutes(5));

        itemTypeEditor.delete(itemType.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(itemTypeFinder.findAllById(List.of(itemType.getId()))).isEmpty();
        assertThat(roomFloor.look(3L)).isEmpty();
    }

    @Test
    void deleteItemTypeInShopFail() {
        ItemType itemType = itemTypeEditor.create(createItemTypeSaveRequest("회춘단", ItemCategory.PILL, 3));
        openShop("왕씨", itemType, null);

        assertThatThrownBy(() -> itemTypeEditor.delete(itemType.getId()))
                .isInstanceOf(ItemTypeInUseException.class)
                .hasMessage("쓰이는 물건은 지울 수 없습니다: 회춘단");
        assertThatThrownBy(() -> itemTypeEditor.update(itemType.getId(), createItemTypeSaveRequest("회춘단", ItemCategory.ELIXIR, 3)))
                .isInstanceOf(ItemTypeInUseException.class)
                .hasMessage("쓰이는 물건은 종류·저물대 등급을 바꿀 수 없습니다: 회춘단");
    }

    @Test
    void lowerBaseValueKeepsFairPrices() {
        ItemType itemType = itemTypeEditor.create(createItemTypeSaveRequest("회춘단", ItemCategory.PILL, 3));
        openShop("왕씨", itemType, 2);
        openShop("이씨", itemType, null);

        ItemType updated = itemTypeEditor.update(itemType.getId(), createItemTypeSaveRequest("회춘단", ItemCategory.PILL, 1));

        assertThat(updated.getBaseValue()).isEqualTo(1);
    }

    @Test
    void raiseBaseValueBreaksFixedPriceFail() {
        ItemType itemType = itemTypeEditor.create(createItemTypeSaveRequest("회춘단", ItemCategory.PILL, 3));
        openShop("왕씨", itemType, 2);
        openShop("이씨", itemType, null);

        // 기준가 4: 매입가 1, 왕씨 값 2 는 정확히 2배라 안 된다. 값을 정하지 않은 이씨는 기준가를 따라가 괜찮다
        assertThatThrownBy(() -> itemTypeEditor.update(itemType.getId(), createItemTypeSaveRequest("회춘단", ItemCategory.PILL, 4)))
                .isInstanceOf(ItemTypeInUseException.class)
                .hasMessage("상점 값이 매입가 규칙에 맞지 않게 됩니다: 회춘단 (왕씨 2)");
        entityManager.clear();
        assertThat(itemTypeFinder.find(itemType.getId()).getBaseValue()).isEqualTo(3);
        // 기준가를 그대로 두면 다른 것은 고칠 수 있다
        itemTypeEditor.update(itemType.getId(), new ItemTypeSaveRequest("회춘단", "바뀐 묘사다.", ItemCategory.PILL, 3, null));
    }

    @Test
    void zeroBaseWithNullPriceFail() {
        ItemType itemType = itemTypeEditor.create(createItemTypeSaveRequest("회춘단", ItemCategory.PILL, 3));
        openShop("왕씨", itemType, null);
        openShop("이씨", itemType, 2);

        assertThatThrownBy(() -> itemTypeEditor.update(itemType.getId(), createItemTypeSaveRequest("회춘단", ItemCategory.PILL, 0)))
                .isInstanceOf(ItemTypeInUseException.class)
                .hasMessage("기준가가 0이면 상점이 값을 정해야 합니다: 회춘단 (왕씨)");
    }

    @Test
    void zeroBaseWithFixedPrices() {
        ItemType itemType = itemTypeEditor.create(createItemTypeSaveRequest("회춘단", ItemCategory.PILL, 3));
        openShop("이씨", itemType, 2);

        assertThat(itemTypeEditor.update(itemType.getId(), createItemTypeSaveRequest("회춘단", ItemCategory.PILL, 0))
                .getBaseValue()).isZero();
    }

    private void openShop(String npcName, ItemType itemType, @Nullable Integer price) {
        Long npcId = npcEditor.create(createNpcSaveRequest(npcName, null)).getId();
        npcEditor.openShop(npcId, new ShopSaveRequest(ShopKind.PILL_SHOP, List.of(new ShopItemRequest(itemType.getId(), price))));
    }
}
