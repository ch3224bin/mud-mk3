package mudmk3.adapter.security;

import mudmk3.application.account.provided.AccountPrincipal;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.util.Collection;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/** Google로 로그인한 사용자. ID 토큰과 함께 이 서버의 계정({@link AccountPrincipal})을 갖는다 */
public final class MudOidcUser extends DefaultOidcUser {

    private final AccountPrincipal accountPrincipal;

    public MudOidcUser(AccountPrincipal accountPrincipal, Collection<? extends GrantedAuthority> authorities,
                       OidcIdToken idToken) {
        super(authorities, idToken, IdTokenClaimNames.SUB);
        this.accountPrincipal = requireNonNull(accountPrincipal);
    }

    public AccountPrincipal getAccountPrincipal() {
        return accountPrincipal;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return o instanceof MudOidcUser other && super.equals(other)
                && accountPrincipal.equals(other.accountPrincipal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), accountPrincipal);
    }
}
