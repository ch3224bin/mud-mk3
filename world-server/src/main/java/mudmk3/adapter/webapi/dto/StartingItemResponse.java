package mudmk3.adapter.webapi.dto;

import mudmk3.domain.item.ItemType;
import mudmk3.domain.world.StartingItem;
import org.jspecify.annotations.Nullable;

/** itemName 은 물건 종류가 지워져 없으면 null */
public record StartingItemResponse(Long itemTypeId, String itemName, int count) {

    public static StartingItemResponse of(StartingItem startingItem, @Nullable ItemType itemType) {
        return new StartingItemResponse(startingItem.itemTypeId(), itemType == null ? null : itemType.getName(), startingItem.count());
    }
}
