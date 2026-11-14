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
        assertThat(inventory.capacity()).isEqualTo(2);
        assertThat(inventory.getEntries()).isEmpty();
    }

    @Test
    void addUpToCreatesInventory() {
        ItemType sword = create("비검", ItemCategory.ARTIFACT);

        assertThat(inventoryEditor.addUpTo(HAN, sword.getId(), 3)).isEqualTo(2);
        assertThat(inventoryEditor.addUpTo(HAN, sword.getId(), 1)).isZero();
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
        inventoryEditor.addUpTo(HAN, bag.getId(), 1);
        inventoryEditor.addUpTo(HAN, sword.getId(), 3);

        assertThat(inventoryEditor.remove(HAN, bag.getId(), false, 1)).isEqualTo(RemoveCheck.BAG_IN_USE);
        assertThat(inventoryEditor.remove(HAN, sword.getId(), false, 2)).isEqualTo(RemoveCheck.OK);
        reload();

        assertThat(inventoryFinder.find(HAN).getEntries()).extracting(InventoryEntry::getCount).containsExactly(1, 1);
    }

    @Test
    void giveMovesBothSides() {
        ItemType pill = create("회춘단", ItemCategory.PILL);
        inventoryEditor.addUpTo(HAN, pill.getId(), 5);

        assertThat(inventoryEditor.give(HAN, YEO, pill.getId(), false, 2)).isEqualTo(GiveResult.GIVEN);
        reload();

        assertThat(inventoryFinder.find(HAN).getEntries()).extracting(InventoryEntry::getCount).containsExactly(3);
        assertThat(inventoryFinder.find(YEO).getEntries()).extracting(InventoryEntry::getCount).containsExactly(2);
    }

    @Test
    void giveReceiverNoRoom() {
        ItemType pill = create("회춘단", ItemCategory.PILL);
        ItemType sword = create("비검", ItemCategory.ARTIFACT);
        inventoryEditor.addUpTo(HAN, pill.getId(), 5);
        inventoryEditor.addUpTo(YEO, sword.getId(), 2);

        assertThat(inventoryEditor.give(HAN, YEO, pill.getId(), false, 2)).isEqualTo(GiveResult.RECEIVER_NO_ROOM);
        reload();

        assertThat(inventoryFinder.find(HAN).getEntries()).extracting(InventoryEntry::getCount).containsExactly(5);
        assertThat(inventoryFinder.find(YEO).getEntries()).extracting(entry -> entry.getItemType().getName())
                .containsExactly("비검");
    }

    @Test
    void giveBagToFullReceiverFail() {
        ItemType bag = itemTypeEditor.create(createItemTypeSaveRequest("하급 저물대", ItemCategory.BAG, 10, BagGrade.LOW));
        inventoryEditor.addUpTo(HAN, bag.getId(), 1);
        inventoryEditor.addUpTo(YEO, bag.getId(), 3);

        assertThat(inventoryEditor.give(HAN, YEO, bag.getId(), false, 1)).isEqualTo(GiveResult.RECEIVER_BAG_LIMIT);
        reload();

        assertThat(inventoryFinder.find(HAN).bagCount()).isEqualTo(1);
        assertThat(inventoryFinder.find(YEO).bagCount()).isEqualTo(3);
    }

    @Test
    void giveBagInUseFail() {
        ItemType bag = itemTypeEditor.create(createItemTypeSaveRequest("하급 저물대", ItemCategory.BAG, 10, BagGrade.LOW));
        ItemType sword = create("비검", ItemCategory.ARTIFACT);
        inventoryEditor.addUpTo(HAN, bag.getId(), 1);
        inventoryEditor.addUpTo(HAN, sword.getId(), 3);

        assertThat(inventoryEditor.give(HAN, YEO, bag.getId(), false, 1)).isEqualTo(GiveResult.GIVER_BAG_IN_USE);
    }

    @Test
    void giveGiverCheckedFirst() {
        ItemType bag = itemTypeEditor.create(createItemTypeSaveRequest("하급 저물대", ItemCategory.BAG, 10, BagGrade.LOW));
        ItemType sword = create("비검", ItemCategory.ARTIFACT);
        inventoryEditor.addUpTo(HAN, bag.getId(), 1);
        inventoryEditor.addUpTo(HAN, sword.getId(), 3);
        inventoryEditor.addUpTo(YEO, bag.getId(), 3);

        assertThat(inventoryEditor.give(HAN, YEO, bag.getId(), false, 1)).isEqualTo(GiveResult.GIVER_BAG_IN_USE);
        reload();

        assertThat(inventoryFinder.find(HAN).bagCount()).isEqualTo(1);
        assertThat(inventoryFinder.find(YEO).bagCount()).isEqualTo(3);
    }

    @Test
    void giveReceiverBagLimitBeforeNoRoom() {
        ItemType bag = itemTypeEditor.create(createItemTypeSaveRequest("하급 저물대", ItemCategory.BAG, 10, BagGrade.LOW));
        inventoryEditor.addUpTo(HAN, bag.getId(), 1);
        inventoryEditor.addUpTo(YEO, bag.getId(), 3);

        assertThat(inventoryEditor.give(HAN, YEO, bag.getId(), false, 1)).isEqualTo(GiveResult.RECEIVER_BAG_LIMIT);
    }

    @Test
    void addUpToZeroFail() {
        ItemType pill = create("회춘단", ItemCategory.PILL);

        assertThatThrownBy(() -> inventoryEditor.addUpTo(HAN, pill.getId(), 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void purchaseNeedsConfirmThenBought() {
        ItemType pill = create("회춘단", ItemCategory.PILL);
        inventoryEditor.addUpTo(HAN, itemTypeFinder.spiritStone(HIGH).getId(), 1);
        SpiritStonePayment payment = new SpiritStonePayment(Map.of(HIGH, 1L), 9_880);

        assertThat(inventoryEditor.purchase(HAN, pill.getId(), 1, 120, false))
                .isEqualTo(new PurchaseResult.NeedsConfirm(payment));
        assertThat(inventoryFinder.find(HAN).spiritStones()).isEqualTo(Map.of(HIGH, 1L));

        assertThat(inventoryEditor.purchase(HAN, pill.getId(), 1, 120, true))
                .isEqualTo(new PurchaseResult.Bought(payment));
        reload();

        assertThat(inventoryFinder.find(HAN).getEntries()).extracting(entry -> entry.getItemType().getName(), InventoryEntry::getCount)
                .containsExactly(tuple("하품 영석", 9_880), tuple("회춘단", 1));
    }

    @Test
    void purchaseNotEnough() {
        ItemType pill = create("회춘단", ItemCategory.PILL);
        inventoryEditor.addUpTo(HAN, itemTypeFinder.spiritStone(LOW).getId(), 9);

        assertThat(inventoryEditor.purchase(HAN, pill.getId(), 5, 15, true)).isEqualTo(new PurchaseResult.NotEnough(9));
    }

    @Test
    void sellPaysLowStones() {
        ItemType hide = create("늑대 가죽", ItemCategory.MATERIAL);
        inventoryEditor.addUpTo(HAN, hide.getId(), 10);

        assertThat(inventoryEditor.sell(HAN, hide.getId(), false, 10, 8)).isEqualTo(SaleResult.SOLD);
        reload();

        assertThat(inventoryFinder.find(HAN).getEntries()).extracting(entry -> entry.getItemType().getName(), InventoryEntry::getCount)
                .containsExactly(tuple("하품 영석", 8));
    }

    @Test
    void bindAndUnbind() {
        ItemType sword = create("비검", ItemCategory.ARTIFACT);
        inventoryEditor.addUpTo(HAN, sword.getId(), 2);

        inventoryEditor.bind(HAN, sword.getId());
        reload();
        assertThat(inventoryFinder.find(HAN).getEntries()).extracting(InventoryEntry::isBound).containsExactly(false, true);

        inventoryEditor.unbind(HAN, sword.getId());
        reload();
        assertThat(inventoryFinder.find(HAN).getEntries()).extracting(InventoryEntry::isBound, InventoryEntry::getCount)
                .containsExactly(tuple(false, 2));
    }

    private ItemType create(String name, ItemCategory category) {
        return itemTypeEditor.create(createItemTypeSaveRequest(name, category, 3));
    }

    private void reload() {
        entityManager.flush();
        entityManager.clear();
    }
}
