package mudmk3.adapter.security;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mudmk3.application.account.provided.AccountLogin;
import mudmk3.application.account.provided.AccountLoginRequest;
import mudmk3.application.account.provided.AccountPrincipal;
import mudmk3.support.stereotype.Adapter;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Google 로그인 결과(검증된 ID 토큰)로 이 서버의 계정을 찾거나 만든다.
 * 이메일이 확인되지 않은 Google 계정은 받지 않는다.
 * 필요한 값(sub, email, email_verified)은 모두 ID 토큰에 있으므로 UserInfo 엔드포인트는 부르지 않는다.
 */
@Slf4j
@Adapter
@RequiredArgsConstructor
public class GoogleOidcUserService extends OidcUserService {

    private final AccountLogin accountLogin;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        return toMudUser(userRequest.getIdToken());
    }

    MudOidcUser toMudUser(OidcIdToken idToken) {
        if (!Boolean.TRUE.equals(idToken.getEmailVerified())) {
            throw failure("email_not_verified", "이메일이 확인되지 않은 Google 계정입니다");
        }
        String email = idToken.getEmail();
        if (email == null || email.isBlank()) {
            throw failure("email_missing", "Google 계정의 이메일을 받지 못했습니다");
        }
        AccountPrincipal principal;
        try {
            principal = accountLogin.login(new AccountLoginRequest(idToken.getSubject(), email));
        } catch (IllegalArgumentException | ConstraintViolationException exception) {
            // 예외 메시지에는 이메일 같은 개인정보가 들어 있을 수 있어 예외 종류만 남긴다
            log.warn("Google 계정으로 로그인하지 못했습니다: {}", exception.getClass().getSimpleName());
            throw failure("invalid_account", "Google 계정 정보가 올바르지 않습니다");
        }
        return new MudOidcUser(principal, SessionAuthenticator.authoritiesOf(principal), idToken);
    }

    private static OAuth2AuthenticationException failure(String code, String description) {
        return new OAuth2AuthenticationException(new OAuth2Error(code, description, null), description);
    }
}
