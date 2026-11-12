package mudmk3.adapter.security;

import mudmk3.application.account.provided.AccountLogin;
import mudmk3.application.account.provided.AccountLoginRequest;
import mudmk3.application.account.provided.AccountPrincipal;
import mudmk3.domain.shared.Email;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 실제 Google을 부르지 않고 ID 토큰 클레임으로 계정 처리 부분만 확인한다 */
@ExtendWith(OutputCaptureExtension.class)
class GoogleOidcUserServiceTest {

    private final List<AccountLoginRequest> logins = new ArrayList<>();

    /** admin@example.com 만 관리자로 보는 가짜 로그인 포트 */
    private final AccountLogin fakeAccountLogin = request -> {
        logins.add(request);
        return new AccountPrincipal(7L, request.email(), request.email().equals("admin@example.com"));
    };

    private final GoogleOidcUserService service = new GoogleOidcUserService(fakeAccountLogin);

    @Test
    void toMudUser() {
        MudOidcUser user = service.toMudUser(idToken("google-sub-1", "hanli@example.com", true));

        assertThat(logins).containsExactly(new AccountLoginRequest("google-sub-1", "hanli@example.com"));
        assertThat(user.getAccountPrincipal()).isEqualTo(new AccountPrincipal(7L, "hanli@example.com", false));
        assertThat(user.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_PLAYER");
        assertThat(user.getSubject()).isEqualTo("google-sub-1");
    }

    @Test
    void adminEmailGetsAdminRole() {
        MudOidcUser user = service.toMudUser(idToken("google-sub-1", "admin@example.com", true));

        assertThat(user.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_PLAYER", "ROLE_ADMIN");
        assertThat(user.getAccountPrincipal().admin()).isTrue();
    }

    @Test
    void emailNotVerifiedFail() {
        assertThatThrownBy(() -> service.toMudUser(idToken("google-sub-1", "hanli@example.com", false)))
                .isInstanceOf(OAuth2AuthenticationException.class);
        assertThatThrownBy(() -> service.toMudUser(idToken("google-sub-1", "hanli@example.com", null)))
                .isInstanceOf(OAuth2AuthenticationException.class);
        assertThat(logins).isEmpty();
    }

    @Test
    void emailMissingFail() {
        assertThatThrownBy(() -> service.toMudUser(idToken("google-sub-1", null, true)))
                .isInstanceOf(OAuth2AuthenticationException.class);
        assertThat(logins).isEmpty();
    }

    @Test
    void loginRejectedFail() {
        GoogleOidcUserService rejecting = new GoogleOidcUserService(request -> {
            throw new IllegalArgumentException("이메일 형식이 바르지 않습니다");
        });

        assertThatThrownBy(() -> rejecting.toMudUser(idToken("google-sub-1", "hanli@example.com", true)))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void loginRejectedDoesNotExposeEmailFail(CapturedOutput output) {
        GoogleOidcUserService rejecting = new GoogleOidcUserService(request -> {
            new Email(request.email());
            throw new IllegalStateException("닿지 않는다");
        });

        assertThatThrownBy(() -> rejecting.toMudUser(idToken("google-sub-1", "secret mail@example.com", true)))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(exception -> assertThat(exception.getMessage()).doesNotContain("mail@example.com"));
        assertThat(output).contains("Google 계정으로 로그인하지 못했습니다")
                .doesNotContain("mail@example.com");
    }

    private static OidcIdToken idToken(String subject, String email, Boolean emailVerified) {
        OidcIdToken.Builder builder = OidcIdToken.withTokenValue("id-token")
                .subject(subject)
                .issuer("https://accounts.google.com")
                .audience(List.of("test-client-id"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60));
        if (email != null) {
            builder.claim("email", email);
        }
        if (emailVerified != null) {
            builder.claim("email_verified", emailVerified);
        }
        return builder.build();
    }
}
