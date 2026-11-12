package mudmk3.domain.room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import mudmk3.domain.shared.Names;

/** 방을 만들거나 고칠 때 받는 값. 이름은 앞뒤 공백을 자른 값으로 검증한다 */
public record RoomSaveInfo(
        @NotBlank @Size(max = 30) String name,
        @NotBlank @Size(max = 1000) String description
) {
    public RoomSaveInfo {
        name = Names.strip(name);
    }
}
