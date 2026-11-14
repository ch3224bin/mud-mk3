package mudmk3.application.faction.provided;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mudmk3.domain.faction.FactionKind;
import mudmk3.domain.faction.FactionSaveInfo;
import mudmk3.domain.shared.Names;
import org.jspecify.annotations.Nullable;

/** 성·종문을 만들거나 고치는 요청. 이름은 앞뒤 공백을 자른 값으로 검증한다 */
public record FactionSaveRequest(
        @NotNull FactionKind kind,
        @NotBlank @Size(max = 30) String name,
        @Size(max = 500) @Nullable String description
) {
    public FactionSaveRequest {
        name = Names.strip(name);
    }

    public FactionSaveInfo toInfo() {
        return new FactionSaveInfo(kind, name, description);
    }
}
