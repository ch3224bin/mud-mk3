package mudmk3.domain.character;

import org.jspecify.annotations.Nullable;

import java.util.regex.Pattern;

/** 캐릭터 이름: 띄어쓰기 없는 한글 완성형 2~6자. 만든 뒤에는 바꿀 수 없다 */
public record CharacterName(String value) {

    public static final int MAX_LENGTH = 6;

    private static final Pattern NAME_PATTERN = Pattern.compile("^[가-힣]{2,6}$");

    public CharacterName {
        if (!isValid(value)) {
            throw new IllegalArgumentException("이름은 띄어쓰기 없이 한글 2~6자로 정하십시오.");
        }
    }

    public static boolean isValid(@Nullable String value) {
        return value != null && NAME_PATTERN.matcher(value).matches();
    }
}
