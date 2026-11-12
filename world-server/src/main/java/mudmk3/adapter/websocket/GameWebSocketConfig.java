package mudmk3.adapter.websocket;

import lombok.RequiredArgsConstructor;
import mudmk3.support.config.CorsProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** 게임 접속 {@code /ws/game}. 허용 오리진 목록 밖의 Origin 은 핸드셰이크에서 403으로 거절한다 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class GameWebSocketConfig implements WebSocketConfigurer {

    public static final String PATH = "/ws/game";

    private final GameWebSocketHandler gameWebSocketHandler;
    private final GameHandshakeInterceptor gameHandshakeInterceptor;
    private final CorsProperties corsProperties;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gameWebSocketHandler, PATH)
                .addInterceptors(gameHandshakeInterceptor)
                .setAllowedOrigins(corsProperties.allowedOrigins().toArray(String[]::new));
    }
}
