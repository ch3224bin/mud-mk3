package mudmk3.adapter.webapi;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import mudmk3.adapter.security.LoginClientRedirect;
import mudmk3.adapter.security.SessionAuthenticator;
import mudmk3.adapter.webapi.dto.AuthMeResponse;
import mudmk3.support.stereotype.WebApiAdapter;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.net.URI;

/** 로그인 시작·로그인 확인·로그아웃 */
@WebApiAdapter
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApi {

    static final String GOOGLE_AUTHORIZATION_PATH = "/oauth2/authorization/google";

    private final LoginClientRedirect loginClientRedirect;

    /** 로그인 뒤 돌아갈 클라이언트(play/admin)를 세션에 적고 Google 로그인으로 보낸다 */
    @GetMapping("/login")
    public ResponseEntity<Void> login(@RequestParam(required = false) @Nullable String client,
                                      HttpServletRequest request) {
        loginClientRedirect.remember(request, client);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(request.getContextPath() + GOOGLE_AUTHORIZATION_PATH))
                .build();
    }

    @GetMapping("/me")
    public AuthMeResponse me(Authentication authentication) {
        return AuthMeResponse.of(SessionAuthenticator.principalOf(authentication)
                .orElseThrow(() -> new InsufficientAuthenticationException("로그인이 필요합니다.")));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }
}
