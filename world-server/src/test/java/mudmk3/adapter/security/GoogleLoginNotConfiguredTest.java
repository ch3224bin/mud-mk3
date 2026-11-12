package mudmk3.adapter.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/** GOOGLE_CLIENT_ID·GOOGLE_CLIENT_SECRET 이 없을 때(기본값 not-configured)도 앱은 뜨고 경고만 남긴다 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=not-configured",
        "spring.security.oauth2.client.registration.google.client-secret=not-configured"
})
@ExtendWith(OutputCaptureExtension.class)
class GoogleLoginNotConfiguredTest {

    @Test
    void startsWithWarning(CapturedOutput output) {
        assertThat(output).contains("Google 로그인이 설정되지 않았습니다 (GOOGLE_CLIENT_ID)");
    }
}
