package mudmk3.adapter.security;

import mudmk3.TestFakesConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static mudmk3.adapter.security.SecurityFixture.clientHeader;
import static org.assertj.core.api.Assertions.assertThat;

/** local 프로필 + 플래그일 때만 생기는 개발용 로그인. 테스트 설정의 관리자 이메일은 admin@example.com 이다 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "local"})
@TestPropertySource(properties = {
        "mud.auth.dev-login.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:dev-login-it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
@Import(TestFakesConfiguration.class)
record DevLoginIntegrationTest(MockMvcTester mvc) {

    @Test
    void devLoginAdmin() {
        MvcTestResult login = devLogin(new MockHttpSession(), "Admin@Example.com");

        assertThat(login).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                {"email":"admin@example.com","admin":true,"characterName":null}""");
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(mvc.get().uri("/api/admin/zones").session(session)).hasStatusOk();
        assertThat(mvc.get().uri("/api/auth/me").session(session)).hasStatusOk()
                .bodyJson().extractingPath("$.admin").isEqualTo(true);
    }

    @Test
    void devLoginPlayer() {
        MvcTestResult login = devLogin(new MockHttpSession(), "hanli@example.com");

        assertThat(login).hasStatusOk().bodyJson().extractingPath("$.admin").isEqualTo(false);
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(mvc.get().uri("/api/admin/zones").session(session)).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void devLoginSameEmailSameAccount() {
        MvcTestResult first = devLogin(new MockHttpSession(), "same@example.com");
        MvcTestResult second = devLogin(new MockHttpSession(), "same@example.com");

        Object firstId = assertThat(first).bodyJson().extractingPath("$.id").actual();
        assertThat(second).bodyJson().extractingPath("$.id").isEqualTo(firstId);
    }

    @Test
    void devLoginChangesSessionId() {
        MockHttpSession session = new MockHttpSession(null, "before-login");

        MvcTestResult login = devLogin(session, "hanli@example.com");

        assertThat(login).hasStatusOk();
        assertThat(login.getRequest().getSession().getId()).isNotEqualTo("before-login");
    }

    @Test
    void devLoginWithoutClientHeaderFail() {
        assertThat(mvc.post().uri("/api/auth/dev")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"admin@example.com"}"""))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void devLoginInvalidEmailFail() {
        assertThat(devLogin(new MockHttpSession(), "not-an-email")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    private MvcTestResult devLogin(MockHttpSession session, String email) {
        return mvc.post().uri("/api/auth/dev").session(session).with(clientHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s"}""".formatted(email))
                .exchange();
    }
}
