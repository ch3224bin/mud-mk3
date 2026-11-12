package mudmk3.adapter.security;

import mudmk3.application.account.provided.AccountLogin;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * {@code @WebMvcTest} 에 실제 보안 설정(필터 체인·CORS·클라이언트 헤더 검사)을 붙인다.
 * Google 로그인은 부르지 않으므로 계정 로그인 포트는 쓰면 실패하는 가짜로 둔다.
 */
@TestConfiguration
@Import({SecurityConfig.class, GoogleOidcUserService.class, LoginClientRedirect.class,
        LoginSuccessHandler.class, LoginFailureHandler.class})
public class WebMvcSecurityTestConfiguration {

    @Bean
    AccountLogin accountLogin() {
        return request -> {
            throw new UnsupportedOperationException("WebMvc 테스트에서는 로그인하지 않는다");
        };
    }
}
