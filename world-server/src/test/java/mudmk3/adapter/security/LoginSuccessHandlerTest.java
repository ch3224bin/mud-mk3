package mudmk3.adapter.security;

import mudmk3.support.config.AuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LoginSuccessHandlerTest {

    private final LoginClientRedirect loginClientRedirect = new LoginClientRedirect(new AuthProperties(
            List.of("admin@example.com"), "http://localhost:3000/", "http://localhost:3001/",
            new AuthProperties.DevLogin(false)));

    private final LoginSuccessHandler successHandler = new LoginSuccessHandler(loginClientRedirect);

    private final LoginFailureHandler failureHandler = new LoginFailureHandler(loginClientRedirect);

    @Test
    void adminClientGoesToAdminConsole() throws Exception {
        MockHttpServletRequest request = requestWithClient("admin");
        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(request, response, new TestingAuthenticationToken("u", null));

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3001/");
        assertThat(request.getSession().getAttribute(LoginClientRedirect.SESSION_ATTRIBUTE)).isNull();
    }

    @Test
    void noClientGoesToPlayClient() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(request, response, new TestingAuthenticationToken("u", null));

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3000/");
    }

    @Test
    void unknownClientGoesToPlayClient() throws Exception {
        MockHttpServletRequest request = requestWithClient("https://evil.example");
        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(request, response, new TestingAuthenticationToken("u", null));

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3000/");
    }

    @Test
    void rememberOnlyKnownClient() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        loginClientRedirect.remember(request, "https://evil.example");
        assertThat(request.getSession().getAttribute(LoginClientRedirect.SESSION_ATTRIBUTE)).isEqualTo("play");

        loginClientRedirect.remember(request, "admin");
        assertThat(request.getSession().getAttribute(LoginClientRedirect.SESSION_ATTRIBUTE)).isEqualTo("admin");

        loginClientRedirect.remember(request, null);
        assertThat(request.getSession().getAttribute(LoginClientRedirect.SESSION_ATTRIBUTE)).isEqualTo("play");
    }

    @Test
    void failureGoesToClientWithLoginError() throws Exception {
        MockHttpServletRequest request = requestWithClient("admin");
        MockHttpServletResponse response = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(request, response, new BadCredentialsException("거절"));

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3001/?loginError=1");
        assertThat(request.getSession().getAttribute(LoginClientRedirect.SESSION_ATTRIBUTE)).isNull();
    }

    @Test
    void failureWithoutClientGoesToPlayClient() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(request, response, new BadCredentialsException("거절"));

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3000/?loginError=1");
    }

    private static MockHttpServletRequest requestWithClient(String client) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute(LoginClientRedirect.SESSION_ATTRIBUTE, client);
        return request;
    }
}
