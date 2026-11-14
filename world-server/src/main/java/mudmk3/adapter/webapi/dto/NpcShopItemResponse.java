package mudmk3.adapter.webapi.dto;

import mudmk3.domain.item.ItemType;
import mudmk3.domain.npc.ShopItem;
import org.jspecify.annotations.Nullable;

/** 상점이 파는 물건 한 줄. price 는 정한 값(없으면 null), effectivePrice 는 실제로 파는 값 */
public record NpcShopItemResponse(Long itemTypeId, String itemName, @Nullable Integer price, long effectivePrice) {

    public static NpcShopItemResponse of(ShopItem shopItem, ItemType itemType) {
        return new NpcShopItemResponse(shopItem.itemTypeId(), itemType.getName(), shopItem.price(),
                shopItem.effectivePrice(itemType));
    }
}
