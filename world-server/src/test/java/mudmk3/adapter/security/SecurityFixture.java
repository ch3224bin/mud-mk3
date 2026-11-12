package mudmk3.adapter.security;

import mudmk3.application.account.provided.AccountPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/** MockMvc 요청에 세션 로그인 상태와 클라이언트 헤더를 붙인다 */
public class SecurityFixture {

    public static final String CLIENT_HEADER = "X-MUD-Client";

    public static AccountPrincipal adminPrincipal() {
        return new AccountPrincipal(1L, "admin@example.com", true);
    }

    public static AccountPrincipal playerPrincipal() {
        return new AccountPrincipal(2L, "hanli@example.com", false);
    }

    public static Authentication authenticationOf(AccountPrincipal principal) {
        return UsernamePasswordAuthenticationToken.authenticated(
                principal, null, SessionAuthenticator.authoritiesOf(principal));
    }

    /** 관리자(ROLE_PLAYER + ROLE_ADMIN)로 로그인한 요청 */
    public static RequestPostProcessor admin() {
        return authentication(authenticationOf(adminPrincipal()));
    }

    /** 일반 계정(ROLE_PLAYER)으로 로그인한 요청 */
    public static RequestPostProcessor player() {
        return authentication(authenticationOf(playerPrincipal()));
    }

    /** 변경 요청에 필요한 {@code X-MUD-Client: 1} 헤더 */
    public static RequestPostProcessor clientHeader() {
        return request -> {
            request.addHeader(CLIENT_HEADER, "1");
            return request;
        };
    }
}
