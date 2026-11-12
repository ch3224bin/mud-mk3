package mudmk3.adapter.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mudmk3.support.stereotype.Adapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/** Google 로그인에 실패하면 로그인을 시작한 클라이언트 주소에 {@code loginError=1} 을 붙여 보낸다 */
@Slf4j
@Adapter
@RequiredArgsConstructor
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final LoginClientRedirect loginClientRedirect;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.info("Google 로그인 실패: {}", exception.getMessage());
        String target = UriComponentsBuilder.fromUriString(loginClientRedirect.takeTargetUrl(request))
                .queryParam("loginError", "1")
                .toUriString();
        response.sendRedirect(target);
    }
}
