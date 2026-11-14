package mudmk3.application.inventory;

import lombok.RequiredArgsConstructor;
import mudmk3.application.inventory.provided.InventoryEditor;
import mudmk3.application.inventory.provided.InventoryFinder;
import mudmk3.application.inventory.required.InventoryRepository;
import mudmk3.application.item.provided.ItemTypeFinder;
import mudmk3.domain.inventory.AddCheck;
import mudmk3.domain.inventory.GiveResult;
import mudmk3.domain.inventory.Inventory;
import mudmk3.domain.inventory.PurchaseResult;
import mudmk3.domain.inventory.RemoveCheck;
import mudmk3.domain.inventory.SaleResult;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.item.SpiritStoneGrade;
import mudmk3.support.stereotype.ApplicationService;

import java.util.EnumMap;
import java.util.Map;

import static org.springframework.util.Assert.state;

@ApplicationService
@RequiredArgsConstructor
public class InventoryModifyService implements InventoryEditor {

    private final InventoryFinder inventoryFinder;
    private final InventoryRepository inventoryRepository;
    private final ItemTypeFinder itemTypeFinder;

    @Override
    public int addUpTo(Long characterId, Long itemTypeId, int count) {
        if (count < 1) throw new IllegalArgumentException("수는 1 이상이어야 합니다: " + count);
        Inventory inventory = findOrCreate(characterId);
        ItemType itemType = itemTypeFinder.find(itemTypeId);
        int added = Math.min(count, inventory.roomFor(itemType));
        if (added > 0) {
            inventory.add(itemType, added, false);
        }
        return added;
    }

    @Override
    public RemoveCheck remove(Long characterId, Long itemTypeId, boolean bound, int count) {
        Inventory inventory = findOrCreate(characterId);
        ItemType itemType = itemTypeFinder.find(itemTypeId);
        RemoveCheck check = inventory.checkRemove(itemType, bound, count);
        if (check == RemoveCheck.OK) {
            inventory.remove(itemType, bound, count);
        }
        return check;
    }

    @Override
    public PurchaseResult purchase(Long characterId, Long itemTypeId, int count, long totalPrice, boolean confirmed) {
        Inventory inventory = findOrCreate(characterId);
        ItemType itemType = itemTypeFinder.find(itemTypeId);
        Map<SpiritStoneGrade, ItemType> stones = spiritStones();
        PurchaseResult result = inventory.checkPurchase(itemType, count, totalPrice, stones.get(SpiritStoneGrade.LOW));
        return switch (result) {
            case PurchaseResult.Bought bought -> {
                inventory.purchase(itemType, count, bought.payment(), stones);
                yield bought;
            }
            case PurchaseResult.NeedsConfirm needsConfirm when confirmed -> {
                inventory.purchase(itemType, count, needsConfirm.payment(), stones);
                yield new PurchaseResult.Bought(needsConfirm.payment());
            }
            default -> result;
        };
    }

    @Override
    public SaleResult sell(Long characterId, Long itemTypeId, boolean bound, int count, long payout) {
        Inventory inventory = findOrCreate(characterId);
        ItemType itemType = itemTypeFinder.find(itemTypeId);
        ItemType lowStone = itemTypeFinder.spiritStone(SpiritStoneGrade.LOW);
        SaleResult result = inventory.checkSale(itemType, bound, count, payout, lowStone);
        if (result == SaleResult.SOLD) {
            inventory.sell(itemType, bound, count, payout, lowStone);
        }
        return result;
    }

    @Override
    public GiveResult give(Long fromCharacterId, Long toCharacterId, Long itemTypeId, boolean bound, int count) {
        state(!fromCharacterId.equals(toCharacterId), "자기 자신에게는 줄 수 없습니다.");
        state(!bound, "귀속된 물건은 남에게 줄 수 없습니다.");
        Inventory giver = findOrCreate(fromCharacterId);
        Inventory receiver = findOrCreate(toCharacterId);
        ItemType itemType = itemTypeFinder.find(itemTypeId);

        if (giver.checkRemove(itemType, false, count) == RemoveCheck.BAG_IN_USE) return GiveResult.GIVER_BAG_IN_USE;
        AddCheck addCheck = receiver.checkAdd(itemType, count);
        if (addCheck == AddCheck.BAG_LIMIT) return GiveResult.RECEIVER_BAG_LIMIT;
        if (addCheck == AddCheck.NO_ROOM) return GiveResult.RECEIVER_NO_ROOM;

        giver.remove(itemType, false, count);
        receiver.add(itemType, count, false);
        return GiveResult.GIVEN;
    }

    @Override
    public void bind(Long characterId, Long itemTypeId) {
        findOrCreate(characterId).bind(itemTypeFinder.find(itemTypeId));
    }

    @Override
    public void unbind(Long characterId, Long itemTypeId) {
        findOrCreate(characterId).unbind(itemTypeFinder.find(itemTypeId));
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
