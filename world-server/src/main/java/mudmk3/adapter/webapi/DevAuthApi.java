package mudmk3.adapter.webapi;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mudmk3.adapter.security.SessionAuthenticator;
import mudmk3.adapter.webapi.dto.AuthMeResponse;
import mudmk3.adapter.webapi.dto.DevLoginRequest;
import mudmk3.application.account.provided.AccountLogin;
import mudmk3.application.account.provided.AccountLoginRequest;
import mudmk3.application.account.provided.AccountPrincipal;
import mudmk3.domain.shared.Email;
import mudmk3.support.stereotype.WebApiAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 개발용 로그인: Google 없이 이메일만으로 세션을 만든다. 계정의 subject는 {@code dev:<email>}.
 * local 프로필이면서 {@code mud.auth.dev-login.enabled=true} 일 때만 빈이 생긴다(그 밖에는 404).
 */
@WebApiAdapter
@Profile("local")
@ConditionalOnProperty(name = "mud.auth.dev-login.enabled", havingValue = "true")
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class DevAuthApi {

    static final String DEV_SUBJECT_PREFIX = "dev:";

    private final AccountLogin accountLogin;
    private final SessionAuthenticator sessionAuthenticator;

    @PostMapping("/dev")
    public AuthMeResponse devLogin(@RequestBody @Valid DevLoginRequest request,
                                   HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String email = new Email(request.email()).address();
        AccountPrincipal principal = accountLogin.login(new AccountLoginRequest(DEV_SUBJECT_PREFIX + email, email));
        sessionAuthenticator.signIn(principal, httpRequest, httpResponse);
        return AuthMeResponse.of(principal);
    }
}
