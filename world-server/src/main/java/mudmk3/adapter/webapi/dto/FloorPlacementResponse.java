package mudmk3.adapter.webapi.dto;

import mudmk3.domain.floor.FloorPlacement;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/** 관리자가 둔 바닥 물건. respawnMinutes 가 null 이면 한 번뿐, remaining 은 지금 시각으로 계산한 남은 수(저장하지 않음) */
public record FloorPlacementResponse(
        Long id,
        Long roomId,
        Long itemTypeId,
        String itemName,
        int count,
        @Nullable Integer respawnMinutes,
        int remaining
) {

    public static FloorPlacementResponse of(FloorPlacement placement, Instant now) {
        return new FloorPlacementResponse(placement.getId(), placement.getRoomId(), placement.getItemType().getId(),
                placement.getItemType().getName(), placement.getCount(), placement.findRespawnMinutes().orElse(null),
                placement.availableAt(now));
    }
}
