package mudmk3.application.item.provided;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mudmk3.domain.item.BagGrade;
import mudmk3.domain.item.ItemCategory;
import mudmk3.domain.item.ItemNames;
import mudmk3.domain.item.ItemTypeSaveInfo;
import org.jspecify.annotations.Nullable;

/**
 * 아이템 종류를 만들거나 고치는 요청. 이름은 앞뒤 공백을 자르고 여러 공백을 하나로 바꾼 값으로 검증한다.
 * 저물대 등급은 종류가 저물대일 때만 보낸다
 */
public record ItemTypeSaveRequest(
        @NotBlank @Size(max = 20) String name,
        @Nullable @Size(max = 500) String description,
        @NotNull ItemCategory category,
        @Min(0) @Max(100_000_000) long baseValue,
        @Nullable BagGrade bagGrade
) {
    public ItemTypeSaveRequest {
        name = normalize(name);
    }

    public ItemTypeSaveInfo toInfo() {
        return new ItemTypeSaveInfo(name, description, category, baseValue, bagGrade);
    }

    /** 검증 전 입력은 null일 수 있으므로 null은 그대로 두고 {@code @NotBlank} 가 막게 한다 */
    private static @Nullable String normalize(@Nullable String name) {
        return name == null ? null : ItemNames.normalize(name);
    }
}
