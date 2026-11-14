package mudmk3.domain.floor;

import mudmk3.domain.item.ItemType;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 방 바닥을 방 보기 묶음으로 만든다(D40) */
public final class FloorStacks {

    private FloorStacks() {
    }

    /**
     * 같은 종류는 배치·버린 것을 합쳐 한 줄로 묶는다. 순서는 그 종류가 처음 나온 자리(배치 id 순 → 버린 것 id 순, 넘겨받은 목록 순서).
     * 그 시각에 0인 배치와 사라진 버린 물건은 빼므로, 그런 것은 자리도 차지하지 않는다
     */
    public static List<FloorStack> of(List<FloorPlacement> placements, List<DroppedItem> droppedItems, Instant now) {
        Map<ItemType, Long> counts = new LinkedHashMap<>();
        for (FloorPlacement placement : placements) {
            int available = placement.availableAt(now);
            if (available > 0) counts.merge(placement.getItemType(), (long) available, Long::sum);
        }
        for (DroppedItem dropped : droppedItems) {
            if (!dropped.isExpiredAt(now) && dropped.getCount() > 0) {
                counts.merge(dropped.getItemType(), (long) dropped.getCount(), Long::sum);
            }
        }
        return counts.entrySet().stream()
                .map(entry -> new FloorStack(entry.getKey(), Math.toIntExact(entry.getValue())))
                .toList();
    }
}
