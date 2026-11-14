package mudmk3.domain.inventory;

import mudmk3.domain.item.BagGrade;
import mudmk3.domain.item.ItemCategory;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.item.ItemTypeFixture;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;

import static mudmk3.domain.inventory.InventoryFixture.createInventory;
import static mudmk3.domain.inventory.InventoryFixture.stone;
import static mudmk3.domain.inventory.InventoryFixture.stones;
import static mudmk3.domain.item.SpiritStoneGrade.HIGH;
import static mudmk3.domain.item.SpiritStoneGrade.LOW;
import static mudmk3.domain.item.SpiritStoneGrade.MIDDLE;
import static mudmk3.domain.item.SpiritStoneGrade.TOP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class InventoryTest {

    private final ItemType pill = ItemTypeFixture.createPill();
    private final ItemType hide = ItemTypeFixture.createWolfHide();
    private final ItemType sword = ItemTypeFixture.createItemType("비검", ItemCategory.ARTIFACT, 50);
    private final ItemType bag = ItemTypeFixture.createBag(BagGrade.LOW);
    private final ItemType lowStone = stone(LOW);

    @Test
    void emptyHasTwoSlots() {
        Inventory inventory = createInventory();

        assertThat(inventory.getCharacterId()).isEqualTo(1L);
        assertThat(inventory.capacity()).isEqualTo(2);
        assertThat(inventory.usedSlots()).isZero();
        assertThat(inventory.bagCount()).isZero();
        assertThat(inventory.getEntries()).isEmpty();
        assertThat(inventory.spiritStones()).isEmpty();
        assertThat(inventory.spiritStoneValue()).isZero();
    }

    @Test
    void bagAddsSlots() {
        Inventory inventory = createInventory();

        inventory.add(bag, 1);

        assertThat(inventory.capacity()).isEqualTo(22);
        assertThat(inventory.usedSlots()).isZero();
        assertThat(inventory.bagCount()).isEqualTo(1);
    }

    @Test
    void addAllAtOnce() {
        Inventory inventory = createInventory();
        SequencedMap<ItemType, Integer> items = new LinkedHashMap<>();
        items.put(pill, 3);
        items.put(sword, 1);
        items.put(lowStone, 30);
        items.put(bag, 1);

        inventory.addAll(items);

        assertThat(inventory.usedSlots()).isEqualTo(3);
        assertThat(inventory.capacity()).isEqualTo(22);
        assertThat(inventory.getEntries()).extracting(InventoryEntry::getItemType, InventoryEntry::getCount)
                .containsExactly(tuple(lowStone, 30), tuple(pill, 3), tuple(sword, 1), tuple(bag, 1));
    }

    @Test
    void addAllNoRoomFail() {
        Inventory inventory = createInventory();
        SequencedMap<ItemType, Integer> items = new LinkedHashMap<>();
        items.put(pill, 3);
        items.put(sword, 2);

        assertThatThrownBy(() -> inventory.addAll(items)).isInstanceOf(IllegalStateException.class);
        assertThat(inventory.getEntries()).isEmpty();
    }

    @Test
    void stackFillsFirst() {
        Inventory inventory = createInventory();

        inventory.add(pill, 98);
        inventory.add(pill, 3);

        assertThat(inventory.getEntries()).extracting(InventoryEntry::getItemType, InventoryEntry::getCount)
                .containsExactly(tuple(pill, 101));
        assertThat(inventory.usedSlots()).isEqualTo(2);
    }

    @Test
    void stonesTakeSlots() {
        Inventory inventory = createInventory();

        inventory.add(lowStone, 10_001);

        assertThat(inventory.usedSlots()).isEqualTo(2);
        assertThat(inventory.spiritStones()).isEqualTo(Map.of(LOW, 10_001L));
        assertThat(inventory.spiritStoneValue()).isEqualTo(10_001);
    }

    @Test
    void roomForPartial() {
        Inventory inventory = createInventory();
        inventory.add(lowStone, 1);

        assertThat(inventory.roomFor(sword)).isEqualTo(1);
        assertThat(inventory.roomFor(lowStone)).isEqualTo(19_999);
        assertThat(inventory.roomFor(bag)).isEqualTo(3);
        assertThat(inventory.checkAdd(sword, 1)).isEqualTo(AddCheck.OK);
        assertThat(inventory.checkAdd(sword, 2)).isEqualTo(AddCheck.NO_ROOM);
    }

    @Test
    void addNoRoomFail() {
        Inventory inventory = createInventory();
        inventory.add(sword, 2);

        assertThatThrownBy(() -> inventory.add(pill, 1)).isInstanceOf(IllegalStateException.class);
        assertThat(inventory.getEntries()).extracting(InventoryEntry::getItemType).containsExactly(sword);
    }

    @Test
    void checkAddHugeCount() {
        Inventory inventory = createInventory();

        assertThat(inventory.checkAdd(sword, Integer.MAX_VALUE)).isEqualTo(AddCheck.NO_ROOM);
        assertThat(inventory.checkAdd(lowStone, 3_000_000_000L)).isEqualTo(AddCheck.NO_ROOM);
        assertThat(inventory.checkAdd(pill, Long.MAX_VALUE)).isEqualTo(AddCheck.NO_ROOM);
        assertThat(inventory.checkAdd(bag, Long.MAX_VALUE)).isEqualTo(AddCheck.BAG_LIMIT);
        assertThat(inventory.checkAdd(bag, Integer.MAX_VALUE)).isEqualTo(AddCheck.BAG_LIMIT);
        assertThatThrownBy(() -> inventory.checkAdd(pill, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fourthBagFail() {
        Inventory inventory = createInventory();
        inventory.add(bag, 3);

        assertThat(inventory.roomFor(bag)).isZero();
        assertThat(inventory.checkAdd(bag, 1)).isEqualTo(AddCheck.BAG_LIMIT);
        assertThatThrownBy(() -> inventory.add(bag, 1)).isInstanceOf(IllegalStateException.class);
        assertThat(inventory.bagCount()).isEqualTo(3);
    }

    @Test
    void removeBagInUseFail() {
        Inventory inventory = createInventory();
        inventory.add(bag, 1);
        inventory.add(sword, 21);

        assertThat(inventory.checkRemove(bag, 1)).isEqualTo(RemoveCheck.BAG_IN_USE);
        assertThatThrownBy(() -> inventory.remove(bag, 1)).isInstanceOf(IllegalStateException.class);
        assertThat(inventory.bagCount()).isEqualTo(1);
        assertThat(inventory.usedSlots()).isEqualTo(21);
    }

    @Test
    void removeMoreThanHeldFail() {
        Inventory inventory = createInventory();
        inventory.add(pill, 2);

        assertThatThrownBy(() -> inventory.remove(pill, 3)).isInstanceOf(IllegalStateException.class);
        assertThat(inventory.getEntries()).extracting(InventoryEntry::getCount).containsExactly(2);
    }

    @Test
    void sameItemTypeMergesIntoOneEntry() {
        Inventory inventory = createInventory();
        inventory.add(bag, 1);
        inventory.add(sword, 1);
        inventory.add(pill, 2);

        inventory.add(sword, 2);
        inventory.addAll(new LinkedHashMap<>(Map.of(pill, 3)));

        assertThat(inventory.getEntries())
                .extracting(entry -> entry.getItemType().getName(), InventoryEntry::getCount)
                .containsExactly(tuple(bag.getName(), 1), tuple("비검", 3), tuple("회춘단(연기 초)", 5));
        assertThat(inventory.usedSlots()).isEqualTo(4);
    }

    @Test
    void removeTakesFromTheOneEntry() {
        Inventory inventory = createInventory();
        inventory.add(sword, 2);

        inventory.remove(sword, 1);

        assertThat(inventory.getEntries()).extracting(InventoryEntry::getCount).containsExactly(1);
        inventory.remove(sword, 1);
        assertThat(inventory.getEntries()).isEmpty();
    }

    @Test
    void purchaseOrder() {
        Inventory notEnough = createInventory();
        notEnough.add(lowStone, 9);
        assertThat(notEnough.checkPurchase(pill, 5, 15, lowStone)).isEqualTo(new PurchaseResult.NotEnough(9));

        Inventory bagLimit = createInventory();
        bagLimit.add(bag, 3);
        bagLimit.add(lowStone, 100);
        assertThat(bagLimit.checkPurchase(bag, 1, 10, lowStone)).isEqualTo(new PurchaseResult.BagLimit());

        Inventory noRoom = createInventory();
        noRoom.add(lowStone, 100);
        noRoom.add(hide, 1);
        assertThat(noRoom.checkPurchase(sword, 1, 10, lowStone)).isEqualTo(new PurchaseResult.NoRoom());

        Inventory noRoomForChange = createInventory();
        noRoomForChange.add(stone(MIDDLE), 1);
        noRoomForChange.add(hide, 1);
        assertThat(noRoomForChange.checkPurchase(pill, 1, 3, lowStone)).isEqualTo(new PurchaseResult.NoRoomForChange(97, 1));

        // 거스름 999,997개는 100칸을 더 쓰지만 빈 칸이 둘이라 모자란 칸은 98
        Inventory shortOfSlots = createInventory();
        shortOfSlots.add(bag, 1);
        shortOfSlots.add(stone(TOP), 1);
        shortOfSlots.add(hide, 99 * 19);
        assertThat(shortOfSlots.checkPurchase(pill, 1, 3, lowStone)).isEqualTo(new PurchaseResult.NoRoomForChange(999_997, 98));

        Inventory needsConfirm = createInventory();
        needsConfirm.add(stone(HIGH), 1);
        assertThat(needsConfirm.checkPurchase(pill, 1, 120, lowStone)).isEqualTo(
                new PurchaseResult.NeedsConfirm(new SpiritStonePayment(Map.of(HIGH, 1L), 9_880)));

        Inventory bought = createInventory();
        bought.add(lowStone, 50);
        assertThat(bought.checkPurchase(pill, 5, 15, lowStone)).isEqualTo(
                new PurchaseResult.Bought(new SpiritStonePayment(Map.of(LOW, 15L), 0)));
    }

    @Test
    void purchaseExactHighStoneNoConfirm() {
        Inventory inventory = createInventory();
        inventory.add(stone(HIGH), 1);

        assertThat(inventory.checkPurchase(pill, 1, 10_000, lowStone)).isEqualTo(
                new PurchaseResult.Bought(new SpiritStonePayment(Map.of(HIGH, 1L), 0)));
    }

    @Test
    void purchaseWithChange() {
        Inventory inventory = createInventory();
        inventory.add(bag, 1);
        inventory.add(lowStone, 50);
        inventory.add(stone(MIDDLE), 2);

        PurchaseResult result = inventory.checkPurchase(pill, 1, 120, lowStone);
        SpiritStonePayment payment = ((PurchaseResult.Bought) result).payment();
        inventory.purchase(pill, 1, payment, stones());

        assertThat(inventory.spiritStones()).isEqualTo(Map.of(LOW, 30L, MIDDLE, 1L));
        assertThat(inventory.getEntries()).extracting(InventoryEntry::getItemType, InventoryEntry::getCount)
                .containsExactly(tuple(lowStone, 30), tuple(stone(MIDDLE), 1), tuple(bag, 1), tuple(pill, 1));
    }

    @Test
    void purchaseUnplannedPaymentFail() {
        Inventory inventory = createInventory();
        inventory.add(lowStone, 10);

        assertThatThrownBy(() -> inventory.purchase(pill, 1, new SpiritStonePayment(Map.of(LOW, 20L), 0), stones()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(inventory.spiritStones()).isEqualTo(Map.of(LOW, 10L));
        assertThat(inventory.getEntries()).hasSize(1);
    }

    @Test
    void sell() {
        Inventory inventory = createInventory();
        inventory.add(hide, 10);

        assertThat(inventory.checkSale(hide, 10, 8, lowStone)).isEqualTo(SaleResult.SOLD);
        inventory.sell(hide, 10, 8, lowStone);

        assertThat(inventory.getEntries()).extracting(InventoryEntry::getItemType, InventoryEntry::getCount)
                .containsExactly(tuple(lowStone, 8));
    }

    @Test
    void saleNoRoomFail() {
        Inventory inventory = createInventory();
        inventory.add(pill, 3);
        inventory.add(hide, 5);

        assertThat(inventory.checkSale(pill, 1, 1, lowStone)).isEqualTo(SaleResult.NO_ROOM);
        assertThatThrownBy(() -> inventory.sell(pill, 1, 1, lowStone)).isInstanceOf(IllegalStateException.class);
        assertThat(inventory.getEntries()).extracting(InventoryEntry::getCount).containsExactly(3, 5);
    }

    @Test
    void saleBagInUseFail() {
        Inventory inventory = createInventory();
        inventory.add(bag, 1);
        inventory.add(sword, 3);

        assertThat(inventory.checkSale(bag, 1, 2, lowStone)).isEqualTo(SaleResult.BAG_IN_USE);
    }

    @Test
    void entriesDisplayOrder() {
        Inventory inventory = createInventory();
        inventory.add(bag, 1);
        inventory.add(hide, 1);
        inventory.add(stone(MIDDLE), 1);
        inventory.add(pill, 1);
        inventory.add(lowStone, 1);

        assertThat(inventory.getEntries()).extracting(InventoryEntry::getItemType)
                .containsExactly(lowStone, stone(MIDDLE), bag, hide, pill);
        assertThatThrownBy(() -> inventory.getEntries().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void zeroEntriesRemoved() {
        Inventory inventory = createInventory();
        inventory.add(pill, 3);
        inventory.add(hide, 1);

        inventory.remove(pill, 3);

        assertThat(inventory.getEntries()).extracting(InventoryEntry::getItemType).containsExactly(hide);
    }
}
