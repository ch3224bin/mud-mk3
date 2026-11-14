package mudmk3.adapter.webapi.dto;

import mudmk3.domain.item.BagGrade;
import mudmk3.domain.item.ItemCategory;
import mudmk3.domain.item.ItemType;
import org.jspecify.annotations.Nullable;

/** 아이템 종류. {@code stackSize} 는 저물대 0, {@code bagSlots} 는 저물대 밖 0, {@code buyRatePercent} 는 사지 않으면 0 */
public record ItemTypeResponse(
        Long id,
        String name,
        String description,
        ItemCategory category,
        String categoryName,
        int stackSize,
        long baseValue,
        @Nullable BagGrade bagGrade,
        @Nullable String bagGradeName,
        int bagSlots,
        boolean spiritStone,
        int buyRatePercent
) {

    public static ItemTypeResponse of(ItemType itemType) {
        BagGrade bagGrade = itemType.findBagGrade().orElse(null);
        return new ItemTypeResponse(itemType.getId(), itemType.getName(), itemType.getDescription(),
                itemType.getCategory(), itemType.getCategory().koreanName(), itemType.stackSize(),
                itemType.getBaseValue(), bagGrade,
                bagGrade == null ? null : bagGrade.koreanName(), itemType.bagSlots(), itemType.isSpiritStone(),
                itemType.getCategory().buyRatePercent());
    }
}
