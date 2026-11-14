package mudmk3.application.npc.provided;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

/** 상점이 파는 물건 한 줄 요청. 판매가가 없으면 기준가를 따라간다 */
public record ShopItemRequest(
        @NotNull Long itemTypeId,
        @Nullable @Min(1) @Max(100_000_000) Integer price
) {
}
