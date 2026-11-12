package mudmk3.domain.shared;

import org.jspecify.annotations.Nullable;

/** 이름 입력을 다듬는다 */
public final class Names {

    private Names() {
    }

    /**
     * 앞뒤 공백을 자른다. 검증 전 입력(요청 DTO 등)은 null일 수 있으므로 null은 그대로 돌려주고
     * Bean Validation({@code @NotBlank})이 막게 한다
     */
    public static @Nullable String strip(@Nullable String name) {
        return name == null ? null : name.strip();
    }
}
