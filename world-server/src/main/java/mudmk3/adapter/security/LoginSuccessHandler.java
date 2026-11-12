package mudmk3.adapter.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import mudmk3.support.stereotype.Adapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

/** Google 로그인에 성공하면 로그인을 시작한 클라이언트 주소로 보낸다 */
@Adapter
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final LoginClientRedirect loginClientRedirect;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        response.sendRedirect(loginClientRedirect.takeTargetUrl(request));
    }
}
