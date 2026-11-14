package mudmk3.application.zone.provided;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import mudmk3.domain.shared.Names;
import mudmk3.domain.zone.ZoneSaveInfo;
import org.jspecify.annotations.Nullable;

/** 지역을 만들거나 고치는 요청. 이름은 앞뒤 공백을 자른 값으로 검증한다. 소유 성·종문 id가 null이면 소유 없음이다 */
public record ZoneSaveRequest(
        @NotBlank @Size(max = 30) String name,
        @Size(max = 500) @Nullable String description,
        @Nullable Long ownerId
) {
    public ZoneSaveRequest {
        name = Names.strip(name);
    }

    public ZoneSaveInfo toInfo() {
        return new ZoneSaveInfo(name, description);
    }
}
