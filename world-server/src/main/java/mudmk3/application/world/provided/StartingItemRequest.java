package mudmk3.application.world.provided;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import mudmk3.domain.world.StartingItem;

/** 시작 소지품 한 줄 */
public record StartingItemRequest(@NotNull Long itemTypeId, @Min(1) @Max(999_999) int count) {

    public StartingItem toStartingItem() {
        return new StartingItem(itemTypeId, count);
    }
}
