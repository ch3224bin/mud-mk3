package mudmk3.application.inventory.provided;

import jakarta.persistence.EntityManager;
import mudmk3.ApplicationServiceTest;
import mudmk3.application.item.provided.ItemTypeEditor;
import mudmk3.application.item.provided.ItemTypeFinder;
import mudmk3.domain.inventory.GiveResult;
import mudmk3.domain.inventory.Inventory;
import mudmk3.domain.inventory.InventoryEntry;
import mudmk3.domain.inventory.PurchaseResult;
import mudmk3.domain.inventory.RemoveCheck;
import mudmk3.domain.inventory.SaleResult;
import mudmk3.domain.inventory.SpiritStonePayment;
import mudmk3.domain.item.BagGrade;
import mudmk3.domain.item.ItemCategory;
import mudmk3.domain.item.ItemType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static mudmk3.domain.item.ItemTypeFixture.createItemTypeSaveRequest;
import static mudmk3.domain.item.SpiritStoneGrade.HIGH;
import static mudmk3.domain.item.SpiritStoneGrade.LOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@ApplicationServiceTest
record InventoryEditorTest(InventoryEditor inventoryEditor, InventoryFinder inventoryFinder,
                           ItemTypeEditor itemTypeEditor, ItemTypeFinder itemTypeFinder, EntityManager entityManager) {

    private static final long HAN = 1L;
    private static final long YEO = 2L;

    @Test
    void findMissingIsEmpty() {
        Inventory inventory = inventoryFinder.find(HAN);

        assertThat(inventory.getId()).isNull();
        assertThat(inventory.capacity(false)).isEqualTo(2);
        assertThat(inventory.getEntries()).isEmpty();
    }

    @Test
    void addUpToCreatesInventory() {
        ItemType sword = create("비검", ItemCategory.ARTIFACT);

        assertThat(inventoryEditor.addUpTo(HAN, sword.getId(), 3, false)).isEqualTo(2);
        assertThat(inventoryEditor.addUpTo(HAN, sword.getId(), 1, false)).isZero();
        reload();

        Inventory inventory = inventoryFinder.find(HAN);
        assertThat(inventory.getId()).isNotNull();
        assertThat(inventory.getEntries()).extracting(entry -> entry.getItemType().getName(), InventoryEntry::getCount)
                .containsExactly(tuple("비검", 2));
    }

    @Test
    void removeBagInUse() {
        ItemType bag = itemTypeEditor.create(createItemTypeSaveRequest("하급 저물대", ItemCategory.BAG, 10, BagGrade.LOW));
        ItemType sword = create("비검", ItemCategory.ARTIFACT);
        inventoryEditor.addUpTo(HAN, bag.getId(), 1, false);
        inventoryEditor.addUpTo(HAN, sword.getId(), 3, false);

        assertThat(inventoryEditor.remove(HAN, bag.getId(), 1, false)).isEqualTo(RemoveCheck.BAG_IN_USE);
        assertThat(inventoryEditor.remove(HAN, sword.getId(), 2, false)).isEqualTo(RemoveCheck.OK);
        reload();

        assertThat(inventoryFinder.find(HAN).getEntries()).extracting(InventoryEntry::getCount).containsExactly(1, 1);
    }

    @Test
    void giveMovesBothSides() {
        ItemType pill = create("회춘단", ItemCategory.PILL);
        inventoryEditor.addUpTo(HAN, pill.getId(), 5, false);

        assertThat(inventoryEditor.give(HAN, YEO, pill.getId(), 2, false, false)).isEqualTo(GiveResult.GIVEN);
        reload();

        assertThat(inventoryFinder.find(HAN).getEntries()).extracting(InventoryEntry::getCount).containsExactly(3);
        assertThat(inventoryFinder.find(YEO).getEntries()).extracting(InventoryEntry::getCount).containsExactly(2);
    }

    @Test
    void giveReceiverNoRoom() {
        ItemType pill = create("회춘단", ItemCategory.PILL);
        ItemType sword = create("비검", ItemCategory.ARTIFACT);
        inventoryEditor.addUpTo(HAN, pill.getId(), 5, false);
        inventoryEditor.addUpTo(YEO, sword.getId(), 2, false);

        assertThat(inventoryEditor.give(HAN, YEO, pill.getId(), 2, false, false)).isEqualTo(GiveResult.RECEIVER_NO_ROOM);
        reload();

        assertThat(inventoryFinder.find(HAN).getEntries()).extracting(InventoryEntry::getCount).containsExactly(5);
        assertThat(inventoryFinder.find(YEO).getEntries()).extracting(entry -> entry.getItemType().getName())
                .containsExactly("비검");
    }

    @Test
    void giveBagToFullReceiverFail() {
        ItemType bag = itemTypeEditor.create(createItemTypeSaveRequest("하급 저물대", ItemCategory.BAG, 10, BagGrade.LOW));
        inventoryEditor.addUpTo(HAN, bag.getId(), 1, false);
        inventoryEditor.addUpTo(YEO, bag.getId(), 3, false);

        assertThat(inventoryEditor.give(HAN, YEO, bag.getId(), 1, false, false)).isEqualTo(GiveResult.RECEIVER_BAG_LIMIT);
        reload();

        assertThat(inventoryFinder.find(HAN).bagCount()).isEqualTo(1);
        assertThat(inventoryFinder.find(YEO).bagCount()).isEqualTo(3);
    }

    @Test
    void giveBagInUseFail() {
        ItemType bag = itemTypeEditor.create(createItemTypeSaveRequest("하급 저물대", ItemCategory.BAG, 10, BagGrade.LOW));
        ItemType sword = create("비검", ItemCategory.ARTIFACT);
        inventoryEditor.addUpTo(HAN, bag.getId(), 1, false);
        inventoryEditor.addUpTo(HAN, sword.getId(), 3, false);

        assertThat(inventoryEditor.give(HAN, YEO, bag.getId(), 1, false, false)).isEqualTo(GiveResult.GIVER_BAG_IN_USE);
    }

    @Test
    void giveGiverCheckedFirst() {
        ItemType bag = itemTypeEditor.create(createItemTypeSaveRequest("하급 저물대", ItemCategory.BAG, 10, BagGrade.LOW));
        ItemType sword = create("비검", ItemCategory.ARTIFACT);
        inventoryEditor.addUpTo(HAN, bag.getId(), 1, false);
        inventoryEditor.addUpTo(HAN, sword.getId(), 3, false);
        inventoryEditor.addUpTo(YEO, bag.getId(), 3, false);

        assertThat(inventoryEditor.give(HAN, YEO, bag.getId(), 1, false, false)).isEqualTo(GiveResult.GIVER_BAG_IN_USE);
        reload();

        assertThat(inventoryFinder.find(HAN).bagCount()).isEqualTo(1);
        assertThat(inventoryFinder.find(YEO).bagCount()).isEqualTo(3);
    }

    @Test
    void giveReceiverBagLimitBeforeNoRoom() {
        ItemType bag = itemTypeEditor.create(createItemTypeSaveRequest("하급 저물대", ItemCategory.BAG, 10, BagGrade.LOW));
        inventoryEditor.addUpTo(HAN, bag.getId(), 1, false);
        inventoryEditor.addUpTo(YEO, bag.getId(), 3, false);

        assertThat(inventoryEditor.give(HAN, YEO, bag.getId(), 1, false, false)).isEqualTo(GiveResult.RECEIVER_BAG_LIMIT);
    }

    @Test
    void addUpToRespectsBraceletOpen() {
        ItemType bracelet = itemTypeEditor.create(
                createItemTypeSaveRequest("하급 저물탁", ItemCategory.BAG, 10, BagGrade.BRACELET_LOW));
        ItemType sword = create("비검", ItemCategory.ARTIFACT);
        assertThat(inventoryEditor.addUpTo(HAN, bracelet.getId(), 2, false)).isEqualTo(1);

        assertThat(inventoryEditor.addUpTo(HAN, sword.getId(), 5, false)).isEqualTo(2);
        assertThat(inventoryEditor.addUpTo(HAN, sword.getId(), 200, true)).isEqualTo(100);
        reload();

        Inventory inventory = inventoryFinder.find(HAN);
        assertThat(inventory.usedSlots()).isEqualTo(102);
        assertThat(inventory.capacity(true)).isEqualTo(102);
        assertThat(inventory.capacity(false)).isEqualTo(2);
        assertThat(inventoryEditor.remove(HAN, bracelet.getId(), 1, true)).isEqualTo(RemoveCheck.BRACELET_IN_USE);
        assertThat(inventoryEditor.remove(HAN, bracelet.getId(), 1, false)).isEqualTo(RemoveCheck.OK);
    }

    @Test
    void giveBraceletToReceiverWithBraceletFail() {
        ItemType low = itemTypeEditor.create(createItemTypeSaveRequest("하급 저물탁", ItemCategory.BAG, 10, BagGrade.BRACELET_LOW));
        ItemType high = itemTypeEditor.create(createItemTypeSaveRequest("상급 저물탁", ItemCategory.BAG, 10, BagGrade.BRACELET_HIGH));
        ItemType sword = create("비검", ItemCategory.ARTIFACT);
        inventoryEditor.addUpTo(HAN, low.getId(), 1, true);
        inventoryEditor.addUpTo(YEO, high.getId(), 1, true);

        assertThat(inventoryEditor.give(HAN, YEO, low.getId(), 1, true, true)).isEqualTo(GiveResult.RECEIVER_BRACELET_LIMIT);
        inventoryEditor.addUpTo(HAN, sword.getId(), 3, true);
        assertThat(inventoryEditor.give(HAN, 3L, low.getId(), 1, true, false)).isEqualTo(GiveResult.GIVER_BRACELET_IN_USE);
        assertThat(inventoryEditor.give(HAN, 3L, low.getId(), 1, false, false)).isEqualTo(GiveResult.GIVEN);
        reload();

        assertThat(inventoryFinder.find(HAN).braceletCount()).isZero();
        assertThat(inventoryFinder.find(YEO).braceletCount()).isEqualTo(1);
        assertThat(inventoryFinder.find(3L).braceletCount()).isEqualTo(1);
    }

    @Test
    void addUpToZeroFail() {
        ItemType pill = create("회춘단", ItemCategory.PILL);

        assertThatThrownBy(() -> inventoryEditor.addUpTo(HAN, pill.getId(), 0, false)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void purchaseNeedsConfirmThenBought() {
        ItemType pill = create("회춘단", ItemCategory.PILL);
        inventoryEditor.addUpTo(HAN, itemTypeFinder.spiritStone(HIGH).getId(), 1, false);
        SpiritStonePayment payment = new SpiritStonePayment(Map.of(HIGH, 1L), 9_880);

        assertThat(inventoryEditor.purchase(HAN, pill.getId(), 1, 120, false, false))
                .isEqualTo(new PurchaseResult.NeedsConfirm(payment));
        assertThat(inventoryFinder.find(HAN).spiritStones()).isEqualTo(Map.of(HIGH, 1L));

        assertThat(inventoryEditor.purchase(HAN, pill.getId(), 1, 120, true, false))
                .isEqualTo(new PurchaseResult.Bought(payment));
        reload();

        assertThat(inventoryFinder.find(HAN).getEntries()).extracting(entry -> entry.getItemType().getName(), InventoryEntry::getCount)
                .containsExactly(tuple("하품 영석", 9_880), tuple("회춘단", 1));
    }

    @Test
    void purchaseNotEnough() {
        ItemType pill = create("회춘단", ItemCategory.PILL);
        inventoryEditor.addUpTo(HAN, itemTypeFinder.spiritStone(LOW).getId(), 9, false);

        assertThat(inventoryEditor.purchase(HAN, pill.getId(), 5, 15, true, false)).isEqualTo(new PurchaseResult.NotEnough(9));
    }

    @Test
    void sellPaysLowStones() {
        ItemType hide = create("늑대 가죽", ItemCategory.MATERIAL);
        inventoryEditor.addUpTo(HAN, hide.getId(), 10, false);

        assertThat(inventoryEditor.sell(HAN, hide.getId(), 10, 8, false)).isEqualTo(SaleResult.SOLD);
        reload();

        assertThat(inventoryFinder.find(HAN).getEntries()).extracting(entry -> entry.getItemType().getName(), InventoryEntry::getCount)
                .containsExactly(tuple("하품 영석", 8));
    }

    @Test
    void addAndGiveMergeIntoOneEntry() {
        ItemType hide = create("늑대 가죽", ItemCategory.MATERIAL);
        inventoryEditor.addUpTo(HAN, hide.getId(), 1, false);
        inventoryEditor.addUpTo(YEO, hide.getId(), 1, false);
        reload();

        inventoryEditor.addUpTo(HAN, hide.getId(), 1, false);
        inventoryEditor.give(YEO, HAN, hide.getId(), 1, false, false);
        reload();

        assertThat(inventoryFinder.find(HAN).getEntries()).extracting(entry -> entry.getItemType().getName(), InventoryEntry::getCount)
                .containsExactly(tuple("늑대 가죽", 3));
        assertThat(inventoryFinder.find(YEO).getEntries()).isEmpty();
    }

    private ItemType create(String name, ItemCategory category) {
        return itemTypeEditor.create(createItemTypeSaveRequest(name, category, 3));
    }

    private void reload() {
        entityManager.flush();
        entityManager.clear();
    }
}
