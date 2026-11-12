package mudmk3.adapter.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mudmk3.application.account.provided.AccountPrincipal;
import mudmk3.support.stereotype.Adapter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import java.util.List;
import java.util.Optional;

/** HTTP 세션 로그인 상태를 만들고, 로그인 정보에서 계정을 꺼낸다 */
@Adapter
public class SessionAuthenticator {

    public static final String ROLE_PLAYER = "ROLE_PLAYER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();

    /** 그 계정으로 세션에 로그인한다. 세션 고정 공격을 막으려고 이미 있던 세션은 id를 바꾼다 */
    public void signIn(AccountPrincipal principal, HttpServletRequest request, HttpServletResponse response) {
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, authoritiesOf(principal)));
        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    /** 모든 계정은 ROLE_PLAYER, 관리자는 ROLE_ADMIN 도 갖는다 */
    public static List<GrantedAuthority> authoritiesOf(AccountPrincipal principal) {
        return principal.admin()
                ? List.of(new SimpleGrantedAuthority(ROLE_PLAYER), new SimpleGrantedAuthority(ROLE_ADMIN))
                : List.of(new SimpleGrantedAuthority(ROLE_PLAYER));
    }

    /** 개발용 로그인({@link AccountPrincipal})과 Google 로그인({@link MudOidcUser}) 모두에서 계정을 꺼낸다 */
    public static Optional<AccountPrincipal> principalOf(@Nullable Authentication authentication) {
        if (authentication == null) {
            return Optional.empty();
        }
        return switch (authentication.getPrincipal()) {
            case AccountPrincipal principal -> Optional.of(principal);
            case MudOidcUser user -> Optional.of(user.getAccountPrincipal());
            case null, default -> Optional.empty();
        };
    }
}
