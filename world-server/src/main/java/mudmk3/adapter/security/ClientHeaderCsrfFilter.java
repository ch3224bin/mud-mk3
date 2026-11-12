package mudmk3.adapter.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * CSRF 방어: {@code /api/**} 의 변경 요청(POST·PUT·DELETE·PATCH)은 {@code X-MUD-Client: 1} 헤더가 있어야 한다.
 * 사용자 정의 헤더는 CORS 사전 요청을 일으키므로 허용 목록 밖의 오리진은 보낼 수 없다.
 * 서블릿 필터로 따로 등록되지 않게 빈으로 만들지 않고 SecurityConfig 가 보안 필터 체인에만 넣는다.
 */
public class ClientHeaderCsrfFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-MUD-Client";

    private static final Set<String> CHANGING_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");

    private static final RequestMatcher API = PathPatternRequestMatcher.withDefaults().matcher("/api/**");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (CHANGING_METHODS.contains(request.getMethod()) && API.matches(request)
                && !"1".equals(request.getHeader(HEADER))) {
            ProblemDetailResponses.write(request, response, HttpStatus.FORBIDDEN, "허용되지 않은 요청입니다.",
                    AccessDeniedException.class);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
