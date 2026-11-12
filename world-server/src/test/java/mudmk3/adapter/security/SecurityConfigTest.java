package mudmk3.adapter.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class SecurityConfigTest {

    private static final String WARNING = "Google 로그인이 설정되지 않았습니다 (GOOGLE_CLIENT_ID)";

    @Test
    void warnWhenGoogleClientIdNotConfigured(CapturedOutput output) throws Exception {
        MockEnvironment environment = new MockEnvironment()
                .withProperty(SecurityConfig.GOOGLE_CLIENT_ID_PROPERTY, "not-configured");

        SecurityConfig.googleLoginConfigurationCheck(environment).run(new DefaultApplicationArguments());

        assertThat(output).contains(WARNING);
    }

    @Test
    void noWarningWhenGoogleClientIdConfigured(CapturedOutput output) throws Exception {
        MockEnvironment environment = new MockEnvironment()
                .withProperty(SecurityConfig.GOOGLE_CLIENT_ID_PROPERTY, "test-client-id");

        SecurityConfig.googleLoginConfigurationCheck(environment).run(new DefaultApplicationArguments());

        assertThat(output).doesNotContain(WARNING);
    }
}
