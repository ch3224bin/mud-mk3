package mudmk3.adapter.security;

import mudmk3.TestFakesConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static mudmk3.adapter.security.SecurityFixture.clientHeader;
import static org.assertj.core.api.Assertions.assertThat;

/** local 프로필이어도 플래그가 꺼져 있으면 개발용 로그인은 없다 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "local"})
@TestPropertySource(properties = {
        "mud.auth.dev-login.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:dev-login-profile-only;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
@Import(TestFakesConfiguration.class)
record DevLoginLocalProfileWithoutFlagTest(MockMvcTester mvc) {

    @Test
    void devLoginNotFound() {
        assertThat(mvc.post().uri("/api/auth/dev").with(clientHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"admin@example.com"}"""))
                .hasStatus(HttpStatus.NOT_FOUND);
    }
}
