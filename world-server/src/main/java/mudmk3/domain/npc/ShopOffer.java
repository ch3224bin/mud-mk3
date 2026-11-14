package mudmk3.domain.npc;

import mudmk3.domain.item.ItemType;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/** 상점을 열 때 받는 파는 물건 한 줄: 물건 종류와 판매가(없으면 기준가를 따라감) */
public record ShopOffer(ItemType itemType, @Nullable Integer price) {

    public ShopOffer {
        requireNonNull(itemType);
    }
}
