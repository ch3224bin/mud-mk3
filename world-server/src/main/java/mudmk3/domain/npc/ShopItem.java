package mudmk3.domain.npc;

import mudmk3.domain.item.ItemType;
import mudmk3.domain.item.Pricing;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * 상점이 파는 물건 한 줄: 물건 종류 id와 판매가(하품 영석, 1~100,000,000). 판매가가 없으면 룰북 계산값(기준가)을 따라간다
 */
public record ShopItem(Long itemTypeId, @Nullable Integer price) {

    public static final int PRICE_MAX = 100_000_000;

    public ShopItem {
        requireNonNull(itemTypeId);
        if (price != null && (price < 1 || price > PRICE_MAX)) {
            throw new IllegalArgumentException("판매가는 1 이상 100,000,000 이하로 정하십시오.");
        }
    }

    /** 실제로 파는 값: 정한 값, 없으면 지금 기준가로 계산한 값 */
    public long effectivePrice(ItemType itemType) {
        return price != null ? price : Pricing.defaultSellPrice(itemType);
    }
}
