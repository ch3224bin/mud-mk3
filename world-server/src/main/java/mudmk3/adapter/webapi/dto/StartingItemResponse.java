package mudmk3.adapter.webapi.dto;

import mudmk3.domain.item.ItemType;
import mudmk3.domain.world.StartingItem;

public record StartingItemResponse(Long itemTypeId, String itemName, int count) {

    public static StartingItemResponse of(StartingItem startingItem, ItemType itemType) {
        return new StartingItemResponse(startingItem.itemTypeId(), itemType.getName(), startingItem.count());
    }
}
