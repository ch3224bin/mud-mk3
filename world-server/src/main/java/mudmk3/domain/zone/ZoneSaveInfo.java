package mudmk3.domain.zone;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import mudmk3.domain.shared.Names;
import org.jspecify.annotations.Nullable;

/** 지역을 만들거나 고칠 때 받는 값. 이름은 앞뒤 공백을 자른 값으로 검증한다. 묘사가 null이면 빈 문자열로 저장한다 */
public record ZoneSaveInfo(
        @NotBlank @Size(max = 30) String name,
        @Size(max = 500) @Nullable String description
) {
    public ZoneSaveInfo {
        name = Names.strip(name);
    }
}
