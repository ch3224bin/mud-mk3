package mudmk3.application.npc.provided;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mudmk3.domain.npc.ShopKind;

import java.util.List;

/** NPC의 상점 역할을 정하는 요청: 상점 종류와 파는 물건(입력 순서, 50개까지) */
public record ShopSaveRequest(
        @NotNull ShopKind kind,
        @NotNull @Size(max = 50) List<@NotNull @Valid ShopItemRequest> items
) {
}
