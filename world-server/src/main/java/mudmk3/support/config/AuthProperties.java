package mudmk3.support.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 로그인 설정({@code mud.auth}). 값이 없는 목록·개발용 로그인은 빈 목록·꺼짐으로 둔다.
 *
 * @param adminEmails     관리자 이메일 목록(대소문자 무시)
 * @param playClientUrl   play-client 로그인 뒤 돌아갈 주소
 * @param adminConsoleUrl admin-console 로그인 뒤 돌아갈 주소
 * @param devLogin        개발용 로그인(local 프로필에서만 의미)
 */
@ConfigurationProperties("mud.auth")
public record AuthProperties(
        List<String> adminEmails,
        String playClientUrl,
        String adminConsoleUrl,
        DevLogin devLogin
) {

    public AuthProperties {
        adminEmails = adminEmails == null ? List.of() : adminEmails.stream()
                .filter(Objects::nonNull)
                .map(email -> email.strip().toLowerCase(Locale.ROOT))
                .filter(email -> !email.isEmpty())
                .toList();
        devLogin = devLogin == null ? new DevLogin(false) : devLogin;
    }

    /** 관리자 이메일 목록에 있는지(대소문자 무시) */
    public boolean isAdmin(String email) {
        return adminEmails.contains(email.strip().toLowerCase(Locale.ROOT));
    }

    public record DevLogin(boolean enabled) {
    }
}
