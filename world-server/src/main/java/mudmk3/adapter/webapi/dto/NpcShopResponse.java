package mudmk3.adapter.webapi.dto;

import mudmk3.domain.item.ItemType;
import mudmk3.domain.npc.Npc;
import mudmk3.domain.npc.ShopKind;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/** NPC의 상점 역할: 종류와 파는 물건(입력 순서) */
public record NpcShopResponse(ShopKind kind, String kindName, List<NpcShopItemResponse> items) {

    /** 상점이 아니면 null. 물건 종류는 id로만 가지므로 이름은 미리 찾은 종류에서 꺼낸다(쓰이는 종류는 지울 수 없어 늘 있다) */
    public static @Nullable NpcShopResponse of(Npc npc, Map<Long, ItemType> itemTypes) {
        return npc.findShopKind()
                .map(kind -> new NpcShopResponse(kind, kind.koreanName(), npc.getShopItems().stream()
                        .map(item -> NpcShopItemResponse.of(item, requireNonNull(itemTypes.get(item.itemTypeId()),
                                () -> "물건 종류를 찾을 수 없습니다. id: " + item.itemTypeId())))
                        .toList()))
                .orElse(null);
    }
}
