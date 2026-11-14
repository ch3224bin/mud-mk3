package mudmk3.domain.floor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.jspecify.annotations.Nullable;

/** 관리자가 바닥에 둘 때 받는 값: 처음 수(1~9,999)와 재등장 분(1~1,440, 없으면 한 번뿐) */
public record FloorPlacementSaveInfo(
        @Min(1) @Max(9999) int count,
        @Nullable @Min(1) @Max(1440) Integer respawnMinutes
) {
}
