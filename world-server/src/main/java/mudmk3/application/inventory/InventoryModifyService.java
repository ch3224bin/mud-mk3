package mudmk3.application.inventory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mudmk3.application.inventory.provided.InventoryEditor;
import mudmk3.application.inventory.provided.InventoryFinder;
import mudmk3.application.inventory.required.InventoryRepository;
import mudmk3.application.item.provided.ItemTypeFinder;
import mudmk3.application.world.provided.WorldSettingsFinder;
import mudmk3.domain.inventory.AddCheck;
import mudmk3.domain.inventory.GiveResult;
import mudmk3.domain.inventory.Inventory;
import mudmk3.domain.inventory.PurchaseResult;
import mudmk3.domain.inventory.RemoveCheck;
import mudmk3.domain.inventory.SaleResult;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.item.SpiritStoneGrade;
import mudmk3.domain.world.StartingItem;
import mudmk3.support.stereotype.ApplicationService;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;

import static org.springframework.util.Assert.state;

@Slf4j
@ApplicationService
@RequiredArgsConstructor
public class InventoryModifyService implements InventoryEditor {

    private final InventoryFinder inventoryFinder;
    private final InventoryRepository inventoryRepository;
    private final ItemTypeFinder itemTypeFinder;
    private final WorldSettingsFinder worldSettingsFinder;

    @Override
    public void createWithStartingItems(Long characterId) {
        state(inventoryRepository.findByCharacterId(characterId).isEmpty(), () -> "이미 소지품이 있습니다. 캐릭터 id: " + characterId);
        List<StartingItem> startingItems = worldSettingsFinder.findStartingItems();
        Map<Long, ItemType> itemTypes = itemTypeFinder.findAllById(
                startingItems.stream().map(StartingItem::itemTypeId).toList());
        SequencedMap<ItemType, Integer> items = new LinkedHashMap<>();
        for (StartingItem startingItem : startingItems) {
            ItemType itemType = itemTypes.get(startingItem.itemTypeId());
            if (itemType == null) {
                log.warn("시작 소지품의 물건 종류가 없어 건너뜁니다. id: {}", startingItem.itemTypeId());
                continue;
            }
            items.put(itemType, startingItem.count());
        }
        Inventory inventory = Inventory.empty(characterId);
        if (inventory.checkAddAll(items, false) == AddCheck.OK) {
            inventory.addAll(items, false);
        } else {
            // 지워진 저물대 등으로 칸이 모자라면 순서대로 들어가는 것만 준다
            items.forEach((itemType, count) -> {
                if (inventory.checkAdd(itemType, count, false) == AddCheck.OK) {
                    inventory.add(itemType, count, false);
                } else {
                    log.warn("시작 소지품이 칸에 들어가지 않아 건너뜁니다. id: {}, 수: {}", itemType.getId(), count);
                }
            });
        }
        inventoryRepository.save(inventory);
    }

    @Override
    public void deleteOf(Long characterId) {
        inventoryRepository.findByCharacterId(characterId).ifPresent(inventoryRepository::delete);
    }

    @Override
    public int addUpTo(Long characterId, Long itemTypeId, int count, boolean braceletOpen) {
        if (count < 1) throw new IllegalArgumentException("수는 1 이상이어야 합니다: " + count);
        Inventory inventory = findOrCreate(characterId);
        ItemType itemType = itemTypeFinder.find(itemTypeId);
        int added = Math.min(count, inventory.roomFor(itemType, braceletOpen));
        if (added > 0) {
            inventory.add(itemType, added, braceletOpen);
        }
        return added;
    }

    @Override
    public RemoveCheck remove(Long characterId, Long itemTypeId, int count, boolean braceletOpen) {
        Inventory inventory = findOrCreate(characterId);
        ItemType itemType = itemTypeFinder.find(itemTypeId);
        RemoveCheck check = inventory.checkRemove(itemType, count, braceletOpen);
        if (check == RemoveCheck.OK) {
            inventory.remove(itemType, count, braceletOpen);
        }
        return check;
    }

    @Override
    public PurchaseResult purchase(Long characterId, Long itemTypeId, int count, long totalPrice, boolean confirmed,
                                   boolean braceletOpen) {
        Inventory inventory = findOrCreate(characterId);
        ItemType itemType = itemTypeFinder.find(itemTypeId);
        Map<SpiritStoneGrade, ItemType> stones = spiritStones();
        PurchaseResult result = inventory.checkPurchase(itemType, count, totalPrice, stones.get(SpiritStoneGrade.LOW),
                braceletOpen);
        return switch (result) {
            case PurchaseResult.Bought bought -> {
                inventory.purchase(itemType, count, bought.payment(), stones, braceletOpen);
                yield bought;
            }
            case PurchaseResult.NeedsConfirm needsConfirm when confirmed -> {
                inventory.purchase(itemType, count, needsConfirm.payment(), stones, braceletOpen);
                yield new PurchaseResult.Bought(needsConfirm.payment());
            }
            default -> result;
        };
    }

    @Override
    public SaleResult sell(Long characterId, Long itemTypeId, int count, long payout, boolean braceletOpen) {
        Inventory inventory = findOrCreate(characterId);
        ItemType itemType = itemTypeFinder.find(itemTypeId);
        ItemType lowStone = itemTypeFinder.spiritStone(SpiritStoneGrade.LOW);
        SaleResult result = inventory.checkSale(itemType, count, payout, lowStone, braceletOpen);
        if (result == SaleResult.SOLD) {
            inventory.sell(itemType, count, payout, lowStone, braceletOpen);
        }
        return result;
    }

    @Override
    public GiveResult give(Long fromCharacterId, Long toCharacterId, Long itemTypeId, int count,
                           boolean giverBraceletOpen, boolean receiverBraceletOpen) {
        state(!fromCharacterId.equals(toCharacterId), "자기 자신에게는 줄 수 없습니다.");
        Inventory giver = findOrCreate(fromCharacterId);
        Inventory receiver = findOrCreate(toCharacterId);
        ItemType itemType = itemTypeFinder.find(itemTypeId);

        RemoveCheck removeCheck = giver.checkRemove(itemType, count, giverBraceletOpen);
        if (removeCheck == RemoveCheck.BAG_IN_USE) return GiveResult.GIVER_BAG_IN_USE;
        if (removeCheck == RemoveCheck.BRACELET_IN_USE) return GiveResult.GIVER_BRACELET_IN_USE;
        AddCheck addCheck = receiver.checkAdd(itemType, count, receiverBraceletOpen);
        if (addCheck == AddCheck.BAG_LIMIT) return GiveResult.RECEIVER_BAG_LIMIT;
        if (addCheck == AddCheck.BRACELET_LIMIT) return GiveResult.RECEIVER_BRACELET_LIMIT;
        if (addCheck == AddCheck.NO_ROOM) return GiveResult.RECEIVER_NO_ROOM;

        giver.remove(itemType, count, giverBraceletOpen);
        receiver.add(itemType, count, receiverBraceletOpen);
        return GiveResult.GIVEN;
    }

    /** 소지품이 없으면(2차 이전 캐릭터 등) 빈 소지품을 만들어 저장한다 */
    private Inventory findOrCreate(Long characterId) {
        Inventory inventory = inventoryFinder.find(characterId);
        return inventory.getId() == null ? inventoryRepository.save(inventory) : inventory;
    }

    private Map<SpiritStoneGrade, ItemType> spiritStones() {
        Map<SpiritStoneGrade, ItemType> stones = new EnumMap<>(SpiritStoneGrade.class);
        for (SpiritStoneGrade grade : SpiritStoneGrade.values()) {
            stones.put(grade, itemTypeFinder.spiritStone(grade));
        }
        return stones;
    }
}
