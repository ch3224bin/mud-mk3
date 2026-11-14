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
import static mudmk3.domain.inventory.InventoryFixture.createOverCapacityInventory;
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
    private final ItemType bracelet = ItemTypeFixture.createBag(BagGrade.BRACELET_LOW);

    @Test
    void emptyHasTwoSlots() {
        Inventory inventory = createInventory();

        assertThat(inventory.getCharacterId()).isEqualTo(1L);
        assertThat(inventory.capacity(false)).isEqualTo(2);
        assertThat(inventory.usedSlots()).isZero();
        assertThat(inventory.bagCount()).isZero();
        assertThat(inventory.getEntries()).isEmpty();
        assertThat(inventory.spiritStones()).isEmpty();
        assertThat(inventory.spiritStoneValue()).isZero();
    }

    @Test
    void bagAddsSlots() {
        Inventory inventory = createInventory();

        inventory.add(bag, 1, false);

        assertThat(inventory.capacity(false)).isEqualTo(22);
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

        inventory.addAll(items, false);

        assertThat(inventory.usedSlots()).isEqualTo(3);
        assertThat(inventory.capacity(false)).isEqualTo(22);
        assertThat(inventory.getEntries()).extracting(InventoryEntry::getItemType, InventoryEntry::getCount)
                .containsExactly(tuple(lowStone, 30), tuple(pill, 3), tuple(sword, 1), tuple(bag, 1));
    }

    @Test
    void addAllNoRoomFail() {
        Inventory inventory = createInventory();
        SequencedMap<ItemType, Integer> items = new LinkedHashMap<>();
        items.put(pill, 3);
        items.put(sword, 2);

        assertThatThrownBy(() -> inventory.addAll(items, false)).isInstanceOf(IllegalStateException.class);
        assertThat(inventory.getEntries()).isEmpty();
    }

    @Test
    void stackFillsFirst() {
        Inventory inventory = createInventory();

        inventory.add(pill, 98, false);
        inventory.add(pill, 3, false);

        assertThat(inventory.getEntries()).extracting(InventoryEntry::getItemType, InventoryEntry::getCount)
                .containsExactly(tuple(pill, 101));
        assertThat(inventory.usedSlots()).isEqualTo(2);
    }

    @Test
    void stonesTakeSlots() {
        Inventory inventory = createInventory();

        inventory.add(lowStone, 10_001, false);

        assertThat(inventory.usedSlots()).isEqualTo(2);
        assertThat(inventory.spiritStones()).isEqualTo(Map.of(LOW, 10_001L));
        assertThat(inventory.spiritStoneValue()).isEqualTo(10_001);
    }

    @Test
    void roomForPartial() {
        Inventory inventory = createInventory();
        inventory.add(lowStone, 1, false);

        assertThat(inventory.roomFor(sword, false)).isEqualTo(1);
        assertThat(inventory.roomFor(lowStone, false)).isEqualTo(19_999);
        assertThat(inventory.roomFor(bag, false)).isEqualTo(3);
        assertThat(inventory.checkAdd(sword, 1, false)).isEqualTo(AddCheck.OK);
        assertThat(inventory.checkAdd(sword, 2, false)).isEqualTo(AddCheck.NO_ROOM);
    }

    @Test
    void addNoRoomFail() {
        Inventory inventory = createInventory();
        inventory.add(sword, 2, false);

        assertThatThrownBy(() -> inventory.add(pill, 1, false)).isInstanceOf(IllegalStateException.class);
        assertThat(inventory.getEntries()).extracting(InventoryEntry::getItemType).containsExactly(sword);
    }

    @Test
    void checkAddHugeCount() {
        Inventory inventory = createInventory();

        assertThat(inventory.checkAdd(sword, Integer.MAX_VALUE, false)).isEqualTo(AddCheck.NO_ROOM);
        assertThat(inventory.checkAdd(lowStone, 3_000_000_000L, false)).isEqualTo(AddCheck.NO_ROOM);
        assertThat(inventory.checkAdd(pill, Long.MAX_VALUE, false)).isEqualTo(AddCheck.NO_ROOM);
        assertThat(inventory.checkAdd(bag, Long.MAX_VALUE, false)).isEqualTo(AddCheck.BAG_LIMIT);
        assertThat(inventory.checkAdd(bag, Integer.MAX_VALUE, false)).isEqualTo(AddCheck.BAG_LIMIT);
        assertThatThrownBy(() -> inventory.checkAdd(pill, 0, false)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fourthBagFail() {
        Inventory inventory = createInventory();
        inventory.add(bag, 3, false);

        assertThat(inventory.roomFor(bag, false)).isZero();
        assertThat(inventory.checkAdd(bag, 1, false)).isEqualTo(AddCheck.BAG_LIMIT);
        assertThatThrownBy(() -> inventory.add(bag, 1, false)).isInstanceOf(IllegalStateException.class);
        assertThat(inventory.bagCount()).isEqualTo(3);
    }

    @Test
    void removeBagInUseFail() {
        Inventory inventory = createInventory();
        inventory.add(bag, 1, false);
        inventory.add(sword, 21, false);

        assertThat(inventory.checkRemove(bag, 1, false)).isEqualTo(RemoveCheck.BAG_IN_USE);
        assertThatThrownBy(() -> inventory.remove(bag, 1, false)).isInstanceOf(IllegalStateException.class);
        assertThat(inventory.bagCount()).isEqualTo(1);
        assertThat(inventory.usedSlots()).isEqualTo(21);
    }

    @Test
    void removeMoreThanHeldFail() {
        Inventory inventory = createInventory();
        inventory.add(pill, 2, false);

        assertThatThrownBy(() -> inventory.remove(pill, 3, false)).isInstanceOf(IllegalStateException.class);
        assertThat(inventory.getEntries()).extracting(InventoryEntry::getCount).containsExactly(2);
    }

    @Test
    void sameItemTypeMergesIntoOneEntry() {
        Inventory inventory = createInventory();
        inventory.add(bag, 1, false);
        inventory.add(sword, 1, false);
        inventory.add(pill, 2, false);

        inventory.add(sword, 2, false);
        inventory.addAll(new LinkedHashMap<>(Map.of(pill, 3)), false);

        assertThat(inventory.getEntries())
                .extracting(entry -> entry.getItemType().getName(), InventoryEntry::getCount)
                .containsExactly(tuple(bag.getName(), 1), tuple("비검", 3), tuple("회춘단(연기 초)", 5));
        assertThat(inventory.usedSlots()).isEqualTo(4);
    }

    @Test
    void removeTakesFromTheOneEntry() {
        Inventory inventory = createInventory();
        inventory.add(sword, 2, false);

        inventory.remove(sword, 1, false);

        assertThat(inventory.getEntries()).extracting(InventoryEntry::getCount).containsExactly(1);
        inventory.remove(sword, 1, false);
        assertThat(inventory.getEntries()).isEmpty();
    }

    @Test
    void purchaseOrder() {
        Inventory notEnough = createInventory();
        notEnough.add(lowStone, 9, false);
        assertThat(notEnough.checkPurchase(pill, 5, 15, lowStone, false)).isEqualTo(new PurchaseResult.NotEnough(9));

        Inventory bagLimit = createInventory();
        bagLimit.add(bag, 3, false);
        bagLimit.add(lowStone, 100, false);
        assertThat(bagLimit.checkPurchase(bag, 1, 10, lowStone, false)).isEqualTo(new PurchaseResult.BagLimit());

        Inventory noRoom = createInventory();
        noRoom.add(lowStone, 100, false);
        noRoom.add(hide, 1, false);
        assertThat(noRoom.checkPurchase(sword, 1, 10, lowStone, false)).isEqualTo(new PurchaseResult.NoRoom());

        Inventory noRoomForChange = createInventory();
        noRoomForChange.add(stone(MIDDLE), 1, false);
        noRoomForChange.add(hide, 1, false);
        assertThat(noRoomForChange.checkPurchase(pill, 1, 3, lowStone, false)).isEqualTo(new PurchaseResult.NoRoomForChange(97, 1));

        // 거스름 999,997개는 100칸을 더 쓰지만 빈 칸이 둘이라 모자란 칸은 98
        Inventory shortOfSlots = createInventory();
        shortOfSlots.add(bag, 1, false);
        shortOfSlots.add(stone(TOP), 1, false);
        shortOfSlots.add(hide, 99 * 19, false);
        assertThat(shortOfSlots.checkPurchase(pill, 1, 3, lowStone, false)).isEqualTo(new PurchaseResult.NoRoomForChange(999_997, 98));

        Inventory needsConfirm = createInventory();
        needsConfirm.add(stone(HIGH), 1, false);
        assertThat(needsConfirm.checkPurchase(pill, 1, 120, lowStone, false)).isEqualTo(
                new PurchaseResult.NeedsConfirm(new SpiritStonePayment(Map.of(HIGH, 1L), 9_880)));

        Inventory bought = createInventory();
        bought.add(lowStone, 50, false);
        assertThat(bought.checkPurchase(pill, 5, 15, lowStone, false)).isEqualTo(
                new PurchaseResult.Bought(new SpiritStonePayment(Map.of(LOW, 15L), 0)));
    }

    @Test
    void purchaseExactHighStoneNoConfirm() {
        Inventory inventory = createInventory();
        inventory.add(stone(HIGH), 1, false);

        assertThat(inventory.checkPurchase(pill, 1, 10_000, lowStone, false)).isEqualTo(
                new PurchaseResult.Bought(new SpiritStonePayment(Map.of(HIGH, 1L), 0)));
    }

    @Test
    void purchaseWithChange() {
        Inventory inventory = createInventory();
        inventory.add(bag, 1, false);
        inventory.add(lowStone, 50, false);
        inventory.add(stone(MIDDLE), 2, false);

        PurchaseResult result = inventory.checkPurchase(pill, 1, 120, lowStone, false);
        SpiritStonePayment payment = ((PurchaseResult.Bought) result).payment();
        inventory.purchase(pill, 1, payment, stones(), false);

        assertThat(inventory.spiritStones()).isEqualTo(Map.of(LOW, 30L, MIDDLE, 1L));
        assertThat(inventory.getEntries()).extracting(InventoryEntry::getItemType, InventoryEntry::getCount)
                .containsExactly(tuple(lowStone, 30), tuple(stone(MIDDLE), 1), tuple(bag, 1), tuple(pill, 1));
    }

    @Test
    void purchaseUnplannedPaymentFail() {
        Inventory inventory = createInventory();
        inventory.add(lowStone, 10, false);

        assertThatThrownBy(() -> inventory.purchase(pill, 1, new SpiritStonePayment(Map.of(LOW, 20L), 0), stones(), false))
                .isInstanceOf(IllegalStateException.class);
        assertThat(inventory.spiritStones()).isEqualTo(Map.of(LOW, 10L));
        assertThat(inventory.getEntries()).hasSize(1);
    }

    @Test
    void sell() {
        Inventory inventory = createInventory();
        inventory.add(hide, 10, false);

        assertThat(inventory.checkSale(hide, 10, 8, lowStone, false)).isEqualTo(SaleResult.SOLD);
        inventory.sell(hide, 10, 8, lowStone, false);

        assertThat(inventory.getEntries()).extracting(InventoryEntry::getItemType, InventoryEntry::getCount)
                .containsExactly(tuple(lowStone, 8));
    }

    @Test
    void saleNoRoomFail() {
        Inventory inventory = createInventory();
        inventory.add(pill, 3, false);
        inventory.add(hide, 5, false);

        assertThat(inventory.checkSale(pill, 1, 1, lowStone, false)).isEqualTo(SaleResult.NO_ROOM);
        assertThatThrownBy(() -> inventory.sell(pill, 1, 1, lowStone, false)).isInstanceOf(IllegalStateException.class);
        assertThat(inventory.getEntries()).extracting(InventoryEntry::getCount).containsExactly(3, 5);
    }

    @Test
    void saleBagInUseFail() {
        Inventory inventory = createInventory();
        inventory.add(bag, 1, false);
        inventory.add(sword, 3, false);

        assertThat(inventory.checkSale(bag, 1, 2, lowStone, false)).isEqualTo(SaleResult.BAG_IN_USE);
    }

    @Test
    void entriesDisplayOrder() {
        Inventory inventory = createInventory();
        inventory.add(bag, 1, false);
        inventory.add(hide, 1, false);
        inventory.add(stone(MIDDLE), 1, false);
        inventory.add(pill, 1, false);
        inventory.add(lowStone, 1, false);

        assertThat(inventory.getEntries()).extracting(InventoryEntry::getItemType)
                .containsExactly(lowStone, stone(MIDDLE), bag, hide, pill);
        assertThatThrownBy(() -> inventory.getEntries().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void zeroEntriesRemoved() {
        Inventory inventory = createInventory();
        inventory.add(pill, 3, false);
        inventory.add(hide, 1, false);

        inventory.remove(pill, 3, false);

        assertThat(inventory.getEntries()).extracting(InventoryEntry::getItemType).containsExactly(hide);
    }

    // ---- 저물탁 (4차)

    @Test
    void braceletOpenAddsSlots() {
        Inventory inventory = createInventory();

        inventory.add(bracelet, 1, false);

        assertThat(inventory.capacity(true)).isEqualTo(102);
        assertThat(inventory.capacity(false)).isEqualTo(2);
        assertThat(inventory.braceletCount()).isEqualTo(1);
        assertThat(inventory.bagCount()).isZero();
    }

    @Test
    void braceletTakesNoSlot() {
        Inventory inventory = createInventory();
        inventory.add(bracelet, 1, false);
        inventory.add(sword, 2, false);

        assertThat(inventory.usedSlots()).isEqualTo(2);
        assertThat(inventory.roomFor(sword, false)).isZero();
        assertThat(inventory.roomFor(sword, true)).isEqualTo(100);
        inventory.add(sword, 100, true);
        assertThat(inventory.usedSlots()).isEqualTo(102);
    }

    @Test
    void secondBraceletFail() {
        Inventory inventory = createInventory();
        ItemType middleBracelet = ItemTypeFixture.createBag(BagGrade.BRACELET_MIDDLE);
        inventory.add(bracelet, 1, true);

        assertThat(inventory.roomFor(bracelet, true)).isZero();
        assertThat(inventory.roomFor(middleBracelet, false)).isZero();
        assertThat(inventory.checkAdd(bracelet, 1, true)).isEqualTo(AddCheck.BRACELET_LIMIT);
        assertThat(inventory.checkAdd(middleBracelet, 1, false)).isEqualTo(AddCheck.BRACELET_LIMIT);
        assertThat(inventory.checkAdd(middleBracelet, Long.MAX_VALUE, false)).isEqualTo(AddCheck.BRACELET_LIMIT);
        assertThatThrownBy(() -> inventory.add(middleBracelet, 1, true)).isInstanceOf(IllegalStateException.class);
        assertThat(inventory.braceletCount()).isEqualTo(1);
        assertThat(createInventory().roomFor(bracelet, false)).isEqualTo(1);
    }

    @Test
    void bagLimitIgnoresBracelet() {
        Inventory inventory = createInventory();
        inventory.add(bag, 3, false);

        assertThat(inventory.roomFor(bracelet, false)).isEqualTo(1);
        inventory.add(bracelet, 1, false);

        assertThat(inventory.bagCount()).isEqualTo(3);
        assertThat(inventory.braceletCount()).isEqualTo(1);
        assertThat(inventory.capacity(true)).isEqualTo(162);
        assertThat(inventory.capacity(false)).isEqualTo(62);
        assertThat(inventory.checkAdd(bag, 1, true)).isEqualTo(AddCheck.BAG_LIMIT);
    }

    @Test
    void overCapacityAddFail() {
        Inventory inventory = createOverCapacityInventory(bracelet, bag, sword);
        inventory.add(pill, 1, true);

        assertThat(inventory.usedSlots()).isEqualTo(131);
        assertThat(inventory.capacity(false)).isEqualTo(42);
        assertThat(inventory.roomFor(sword, false)).isZero();
        assertThat(inventory.roomFor(pill, false)).isZero();
        assertThat(inventory.roomFor(bag, false)).isZero();
        assertThat(inventory.checkAdd(sword, 1, false)).isEqualTo(AddCheck.NO_ROOM);
        assertThat(inventory.checkAdd(pill, 1, false)).isEqualTo(AddCheck.NO_ROOM);
        assertThatThrownBy(() -> inventory.add(pill, 1, false)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void overCapacityRemoveItemOk() {
        Inventory inventory = createOverCapacityInventory(bracelet, bag, sword);

        assertThat(inventory.checkRemove(sword, 1, false)).isEqualTo(RemoveCheck.OK);
        inventory.remove(sword, 1, false);

        assertThat(inventory.usedSlots()).isEqualTo(129);
        assertThat(inventory.capacity(false)).isEqualTo(42);
    }

    @Test
    void overCapacityRemoveBagFail() {
        Inventory inventory = createOverCapacityInventory(bracelet, bag, sword);

        assertThat(inventory.checkRemove(bag, 1, false)).isEqualTo(RemoveCheck.BAG_IN_USE);
        assertThatThrownBy(() -> inventory.remove(bag, 1, false)).isInstanceOf(IllegalStateException.class);
        assertThat(inventory.bagCount()).isEqualTo(2);
    }

    @Test
    void removeOpenBraceletInUseFail() {
        Inventory inventory = createInventory();
        inventory.add(bracelet, 1, true);
        inventory.add(sword, 3, true);

        assertThat(inventory.checkRemove(bracelet, 1, true)).isEqualTo(RemoveCheck.BRACELET_IN_USE);
        assertThatThrownBy(() -> inventory.remove(bracelet, 1, true)).isInstanceOf(IllegalStateException.class);
        assertThat(inventory.checkSale(bracelet, 1, 5, lowStone, true)).isEqualTo(SaleResult.BRACELET_IN_USE);
        assertThat(inventory.braceletCount()).isEqualTo(1);
    }

    @Test
    void removeClosedBraceletWhileOverOk() {
        Inventory inventory = createOverCapacityInventory(bracelet, bag, sword);

        assertThat(inventory.checkRemove(bracelet, 1, false)).isEqualTo(RemoveCheck.OK);
        inventory.remove(bracelet, 1, false);

        assertThat(inventory.braceletCount()).isZero();
        assertThat(inventory.capacity(false)).isEqualTo(42);
        assertThat(inventory.usedSlots()).isEqualTo(130);
    }

    @Test
    void overCapacitySaleNoNewSlotOk() {
        Inventory inventory = createOverCapacityInventory(bracelet, bag, sword);
        inventory.add(lowStone, 1, true);

        assertThat(inventory.checkSale(sword, 1, 20, lowStone, false)).isEqualTo(SaleResult.SOLD);
        inventory.sell(sword, 1, 20, lowStone, false);

        assertThat(inventory.usedSlots()).isEqualTo(130);
        assertThat(inventory.spiritStones()).isEqualTo(Map.of(LOW, 21L));
    }

    @Test
    void overCapacitySaleNewSlotFail() {
        Inventory inventory = createOverCapacityInventory(bracelet, bag, sword);
        inventory.add(pill, 2, true);

        assertThat(inventory.checkSale(pill, 1, 1, lowStone, false)).isEqualTo(SaleResult.NO_ROOM);
        assertThat(inventory.checkSale(bag, 1, 5, lowStone, false)).isEqualTo(SaleResult.BAG_IN_USE);
        assertThat(inventory.checkSale(bracelet, 1, 1, lowStone, false)).isEqualTo(SaleResult.NO_ROOM);
        assertThatThrownBy(() -> inventory.sell(pill, 1, 1, lowStone, false)).isInstanceOf(IllegalStateException.class);
        assertThat(inventory.usedSlots()).isEqualTo(131);
    }

    @Test
    void purchaseBraceletLimit() {
        Inventory inventory = createInventory();
        inventory.add(bracelet, 1, true);
        inventory.add(lowStone, 1_000, true);

        assertThat(inventory.checkPurchase(ItemTypeFixture.createBag(BagGrade.BRACELET_HIGH), 1, 10, lowStone, true))
                .isEqualTo(new PurchaseResult.BraceletLimit());
        assertThat(inventory.checkPurchase(bag, 1, 10, lowStone, false)).isInstanceOf(PurchaseResult.Bought.class);
    }

    @Test
    void giveReceiverBraceletLimit() {
        Inventory giver = createInventory();
        giver.add(bracelet, 1, false);
        Inventory receiver = createInventory();
        receiver.add(ItemTypeFixture.createBag(BagGrade.BRACELET_MIDDLE), 1, false);

        assertThat(giver.checkRemove(bracelet, 1, false)).isEqualTo(RemoveCheck.OK);
        assertThat(receiver.checkAdd(bracelet, 1, false)).isEqualTo(AddCheck.BRACELET_LIMIT);
    }
}
