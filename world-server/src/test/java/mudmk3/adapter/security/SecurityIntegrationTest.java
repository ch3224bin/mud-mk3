package mudmk3.adapter.security;

import mudmk3.application.account.provided.AccountPrincipal;
import mudmk3.application.character.required.CharacterRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static mudmk3.adapter.security.SecurityFixture.admin;
import static mudmk3.adapter.security.SecurityFixture.authenticationOf;
import static mudmk3.adapter.security.SecurityFixture.clientHeader;
import static mudmk3.adapter.security.SecurityFixture.player;
import static mudmk3.adapter.security.SecurityFixture.playerPrincipal;
import static mudmk3.domain.character.CharacterFixture.createCharacter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;

/** 실제 보안 필터 체인으로 인증·권한·CSRF 헤더·CORS·로그인 리다이렉트를 확인한다. Google은 부르지 않는다 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:security-it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE")
record SecurityIntegrationTest(MockMvcTester mvc, CharacterRepository characterRepository) {

    @Test
    void adminApiUnauthenticatedFail() {
        MvcTestResult result = mvc.get().uri("/api/admin/zones").exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION)).isNull();
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("로그인이 필요합니다.");
    }

    @Test
    void adminApiPlayerFail() {
        assertThat(mvc.get().with(player()).uri("/api/admin/zones"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().extractingPath("$.detail").isEqualTo("권한이 없습니다.");
    }

    @Test
    void adminApiAdmin() {
        assertThat(mvc.get().with(admin()).uri("/api/admin/zones")).hasStatusOk();
    }

    @Test
    void adminChangeWithoutClientHeaderFail() {
        MvcTestResult result = mvc.post().with(admin()).uri("/api/admin/zones")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"청석숲"}""")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("허용되지 않은 요청입니다.");
    }

    @Test
    void adminChangeWithClientHeader() {
        assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/zones")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"청석숲"}"""))
                .hasStatus(HttpStatus.CREATED);
    }

    @Test
    void loginRedirectsToGoogle() {
        MvcTestResult login = mvc.get().uri("/api/auth/login?client=admin").exchange();

        assertThat(login).hasStatus(HttpStatus.FOUND);
        assertThat(login.getResponse().getRedirectedUrl()).endsWith("/oauth2/authorization/google");
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.getAttribute(LoginClientRedirect.SESSION_ATTRIBUTE)).isEqualTo("admin");

        MvcTestResult authorization = mvc.get().uri("/oauth2/authorization/google").session(session).exchange();

        assertThat(authorization).hasStatus(HttpStatus.FOUND);
        UriComponents google = UriComponentsBuilder.fromUriString(authorization.getResponse().getRedirectedUrl()).build();
        assertThat(google.getHost()).isEqualTo("accounts.google.com");
        MultiValueMap<String, String> query = google.getQueryParams();
        assertThat(decoded(query.getFirst("client_id"))).isEqualTo("test-client-id");
        assertThat(decoded(query.getFirst("redirect_uri"))).isEqualTo("http://localhost/login/oauth2/code/google");
        assertThat(decoded(query.getFirst("scope"))).isEqualTo("openid profile email");
        assertThat(decoded(query.getFirst("response_type"))).isEqualTo("code");
    }

    @Test
    void loginWithoutClientRemembersPlay() {
        MvcTestResult login = mvc.get().uri("/api/auth/login").exchange();

        assertThat(login).hasStatus(HttpStatus.FOUND);
        assertThat(login.getRequest().getSession().getAttribute(LoginClientRedirect.SESSION_ATTRIBUTE))
                .isEqualTo("play");
    }

    @Test
    void me() {
        assertThat(mvc.get().with(player()).uri("/api/auth/me"))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("""
                        {"id":2,"email":"hanli@example.com","admin":false,"characterName":null}""");
    }

    @Test
    void meWithGoogleUser() {
        OidcIdToken idToken = idToken("google-sub-1", "admin@example.com");
        AccountPrincipal principal = new AccountPrincipal(5L, "admin@example.com", true);
        MudOidcUser user = new MudOidcUser(principal, SessionAuthenticator.authoritiesOf(principal), idToken);

        assertThat(mvc.get().with(oidcLogin().oidcUser(user)).uri("/api/auth/me"))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("""
                        {"id":5,"email":"admin@example.com","admin":true,"characterName":null}""");
    }

    @Test
    void meIncludesCharacterName() {
        characterRepository.save(createCharacter(9001L, "한백", 1L));
        characterRepository.save(createCharacter(9002L, "한립", 1L));
        AccountPrincipal player = new AccountPrincipal(9001L, "hanbaek@example.com", false);

        assertThat(mvc.get().with(authentication(authenticationOf(player))).uri("/api/auth/me"))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("""
                        {"id":9001,"email":"hanbaek@example.com","admin":false,"characterName":"한백"}""");

        // Google 로그인의 getName()은 sub 이므로 계정은 accountId 로 찾아야 한다
        AccountPrincipal googlePlayer = new AccountPrincipal(9002L, "hanli@example.com", false);
        MudOidcUser user = new MudOidcUser(googlePlayer, SessionAuthenticator.authoritiesOf(googlePlayer),
                idToken("9001", "hanli@example.com"));
        assertThat(mvc.get().with(oidcLogin().oidcUser(user)).uri("/api/auth/me"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.characterName").isEqualTo("한립");
    }

    @Test
    void meUnauthenticatedFail() {
        assertThat(mvc.get().uri("/api/auth/me")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logoutInvalidatesSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(authenticationOf(playerPrincipal())));
        assertThat(mvc.get().uri("/api/auth/me").session(session)).hasStatusOk();

        assertThat(mvc.post().uri("/api/auth/logout").session(session).with(clientHeader()))
                .hasStatus(HttpStatus.NO_CONTENT);

        assertThat(session.isInvalid()).isTrue();
        assertThat(mvc.get().uri("/api/auth/me").session(session)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logoutWithoutSessionSucceeds() {
        assertThat(mvc.post().uri("/api/auth/logout").with(clientHeader())).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void logoutWithoutSessionAndClientHeaderFail() {
        assertThat(mvc.post().uri("/api/auth/logout")).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void logoutWithoutClientHeaderFail() {
        assertThat(mvc.post().with(player()).uri("/api/auth/logout")).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void devLoginNotFoundByDefault() {
        assertThat(mvc.post().uri("/api/auth/dev").with(clientHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"admin@example.com"}"""))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void corsPreflight() {
        MvcTestResult result = mvc.options().uri("/api/admin/zones")
                .header(HttpHeaders.ORIGIN, "http://localhost:3001")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "x-mud-client,content-type")
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .isEqualTo("http://localhost:3001");
        assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
        assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS))
                .containsIgnoringCase("x-mud-client");
    }

    @Test
    void corsPreflightOtherOriginFail() {
        MvcTestResult result = mvc.options().uri("/api/admin/zones")
                .header(HttpHeaders.ORIGIN, "http://evil.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "x-mud-client")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
    }

    private static OidcIdToken idToken(String subject, String email) {
        return OidcIdToken.withTokenValue("id-token")
                .subject(subject)
                .claim("email", email)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }

    private static String decoded(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
