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
import mudmk3.domain.technique.Technique;
import org.jspecify.annotations.Nullable;

/**
 * 아이템 종류를 만들거나 고치는 요청. 이름은 앞뒤 공백을 자르고 여러 공백을 하나로 바꾼 값으로 검증한다.
 * 저물대 등급은 종류가 저물대일 때만, 담긴 공법 id는 종류가 옥간·서책일 때만 보낸다(빠지면 담긴 공법 없음)
 */
public record ItemTypeSaveRequest(
        @NotBlank @Size(max = 20) String name,
        @Nullable @Size(max = 500) String description,
        @NotNull ItemCategory category,
        @Min(0) @Max(100_000_000) long baseValue,
        @Nullable BagGrade bagGrade,
        @Nullable Long techniqueId
) {
    public ItemTypeSaveRequest {
        name = normalize(name);
    }

    /** 담긴 공법은 서비스가 {@code techniqueId} 로 찾아 넘긴다 */
    public ItemTypeSaveInfo toInfo(@Nullable Technique technique) {
        return new ItemTypeSaveInfo(name, description, category, baseValue, bagGrade, technique);
    }

    /** 검증 전 입력은 null일 수 있으므로 null은 그대로 두고 {@code @NotBlank} 가 막게 한다 */
    private static @Nullable String normalize(@Nullable String name) {
        return name == null ? null : ItemNames.normalize(name);
    }
}
