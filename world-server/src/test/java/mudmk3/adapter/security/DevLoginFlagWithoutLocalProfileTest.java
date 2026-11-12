package mudmk3.adapter.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static mudmk3.adapter.security.SecurityFixture.clientHeader;
import static org.assertj.core.api.Assertions.assertThat;

/** 플래그가 켜져 있어도 local 프로필이 아니면 개발용 로그인은 없다 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "mud.auth.dev-login.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:dev-login-flag-only;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
record DevLoginFlagWithoutLocalProfileTest(MockMvcTester mvc) {

    @Test
    void devLoginNotFound() {
        assertThat(mvc.post().uri("/api/auth/dev").with(clientHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"admin@example.com"}"""))
                .hasStatus(HttpStatus.NOT_FOUND);
    }
}
