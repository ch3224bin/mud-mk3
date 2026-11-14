package mudmk3.application.floor.provided;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import mudmk3.domain.floor.FloorPlacementSaveInfo;
import org.jspecify.annotations.Nullable;

/** 관리자가 방 바닥에 물건을 두거나 고치는 요청. 재등장이 없으면(null) 한 번뿐이다 */
public record FloorPlacementSaveRequest(
        @NotNull Long itemTypeId,
        @Min(1) @Max(9999) int count,
        @Nullable @Min(1) @Max(1440) Integer respawnMinutes
) {

    public FloorPlacementSaveInfo toInfo() {
        return new FloorPlacementSaveInfo(count, respawnMinutes);
    }
}
