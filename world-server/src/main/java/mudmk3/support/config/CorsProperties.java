package mudmk3.support.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 브라우저 오리진 허용 목록({@code mud.cors}). REST CORS와 WebSocket 핸드셰이크 Origin 검사에 쓴다.
 *
 * @param allowedOrigins 허용하는 오리진(예: {@code http://localhost:3000})
 */
@ConfigurationProperties("mud.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}
