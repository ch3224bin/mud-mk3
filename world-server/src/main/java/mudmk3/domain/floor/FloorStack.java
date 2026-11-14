package mudmk3.domain.floor;

import mudmk3.domain.item.ItemType;

import static java.util.Objects.requireNonNull;

/** 방 보기에 한 줄로 보이는 바닥 물건 묶음: 같은 종류의 배치·버린 것을 합친 수 */
public record FloorStack(ItemType itemType, int count) {

    public FloorStack {
        requireNonNull(itemType);
    }
}
