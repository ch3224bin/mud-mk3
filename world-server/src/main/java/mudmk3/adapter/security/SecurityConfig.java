package mudmk3.adapter.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mudmk3.support.config.AuthProperties;
import mudmk3.support.config.CorsProperties;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * 보안 설정: Google OAuth2 로그인 + HTTP 세션, 관리 API는 ADMIN 만,
 * CSRF는 Spring 기본 토큰 대신 {@link ClientHeaderCsrfFilter}, CORS는 허용 오리진 목록만.
 * {@code /api/**} 의 인증 실패는 Google로 보내지 않고 401을 준다. 로그아웃은 AuthApi 가 직접 한다.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({AuthProperties.class, CorsProperties.class})
@RequiredArgsConstructor
public class SecurityConfig {

    static final String GOOGLE_CLIENT_ID_PROPERTY = "spring.security.oauth2.client.registration.google.client-id";
    static final String NOT_CONFIGURED = "not-configured";

    private final GoogleOidcUserService googleOidcUserService;
    private final LoginSuccessHandler loginSuccessHandler;
    private final LoginFailureHandler loginFailureHandler;
    private final CorsProperties corsProperties;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterAfter(new ClientHeaderCsrfFilter(), CorsFilter.class)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/dev").permitAll()
                        // WebSocket 인증은 핸드셰이크에서 세션을 보고 따로 한다
                        .requestMatchers("/oauth2/**", "/login/oauth2/**", "/ws/**", "/error").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2Login(login -> login
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(googleOidcUserService))
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler))
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                (request, response, exception) -> ProblemDetailResponses.write(request, response,
                                        HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.", exception.getClass()),
                                PathPatternRequestMatcher.withDefaults().matcher("/api/**"))
                        .accessDeniedHandler((request, response, exception) -> ProblemDetailResponses.write(
                                request, response, HttpStatus.FORBIDDEN, "권한이 없습니다.", exception.getClass())));
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowCredentials(true);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", ClientHeaderCsrfFilter.HEADER));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /** Google 자격 증명 없이도 앱은 뜬다(개발용 로그인은 된다). 대신 시작할 때 알린다 */
    @Bean
    static ApplicationRunner googleLoginConfigurationCheck(Environment environment) {
        return arguments -> {
            String clientId = environment.getProperty(GOOGLE_CLIENT_ID_PROPERTY, NOT_CONFIGURED);
            if (clientId.isBlank() || NOT_CONFIGURED.equals(clientId)) {
                log.warn("Google 로그인이 설정되지 않았습니다 (GOOGLE_CLIENT_ID)");
            }
        };
    }
}
