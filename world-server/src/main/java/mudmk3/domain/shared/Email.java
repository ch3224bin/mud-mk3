package mudmk3.domain.shared;

import java.util.Locale;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/** 이메일 주소. 앞뒤 공백을 자르고 소문자로 맞춘다 */
public record Email(String address) {

    public static final int MAX_LENGTH = 320;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$");

    public Email {
        address = requireNonNull(address).strip().toLowerCase(Locale.ROOT);
        if (address.length() > MAX_LENGTH || !EMAIL_PATTERN.matcher(address).matches()) {
            throw new IllegalArgumentException("이메일 형식이 바르지 않습니다: " + address);
        }
    }
}
