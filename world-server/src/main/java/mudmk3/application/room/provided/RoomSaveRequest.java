package mudmk3.application.room.provided;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mudmk3.domain.room.RoomSaveInfo;
import mudmk3.domain.shared.Names;

/** 방을 만들거나 고치는 요청. 이름은 앞뒤 공백을 자른 값으로 검증한다 */
public record RoomSaveRequest(
        @NotNull Long zoneId,
        @NotBlank @Size(max = 30) String name,
        @NotBlank @Size(max = 1000) String description
) {
    public RoomSaveRequest {
        name = Names.strip(name);
    }

    public RoomSaveInfo toInfo() {
        return new RoomSaveInfo(name, description);
    }
}
