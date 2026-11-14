package mudmk3.domain.shared;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/** 이름·낱말 입력을 다듬는다 */
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

    /**
     * 낱말 입력을 다듬는다: 앞뒤 공백을 자르고 앞의 {@code [} 와 뒤의 {@code ]} 를 뗀 뒤 다시 앞뒤 공백을 자른다
     * ({@code [단약]} → {@code 단약}). NPC 낱말 저장과 {@code 대화} 명령 해석이 함께 쓴다
     */
    public static String stripBrackets(String word) {
        String result = requireNonNull(word).strip();
        if (result.startsWith("[")) result = result.substring(1);
        if (result.endsWith("]")) result = result.substring(0, result.length() - 1);
        return result.strip();
    }
}
