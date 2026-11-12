package mudmk3.adapter.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 보안 필터 단계(컨트롤러 밖)의 거절 응답을 ApiControllerAdvice 와 같은 모양의 ProblemDetail JSON 으로 쓴다
 */
final class ProblemDetailResponses {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    private ProblemDetailResponses() {
    }

    static void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String detail,
                      Class<?> exceptionType) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "about:blank");
        body.put("title", status.getReasonPhrase());
        body.put("status", status.value());
        body.put("detail", detail);
        body.put("instance", request.getRequestURI());
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("exception", exceptionType.getSimpleName());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        JSON_MAPPER.writeValue(response.getWriter(), body);
    }
}
