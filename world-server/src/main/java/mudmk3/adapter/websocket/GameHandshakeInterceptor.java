package mudmk3.adapter.websocket;

import jakarta.servlet.http.HttpSession;
import mudmk3.adapter.security.SessionAuthenticator;
import mudmk3.support.stereotype.Adapter;
import org.jspecify.annotations.Nullable;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * 핸드셰이크에 실려 온 HTTP 세션(쿠키)의 로그인 정보에서 계정 id를 꺼내 WebSocket 세션 속성 {@value #ACCOUNT_ID} 로 넘긴다.
 * 로그인하지 않았어도 핸드셰이크는 통과시킨다 — 연결을 받은 뒤 까닭을 알리고 닫는다({@link GameWebSocketHandler}).
 * HTTP 세션은 새로 만들지 않는다.
 */
@Adapter
public class GameHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ACCOUNT_ID = "accountId";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpSession httpSession = servletRequest.getServletRequest().getSession(false);
            if (httpSession != null
                    && httpSession.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)
                    instanceof SecurityContext context) {
                SessionAuthenticator.principalOf(context.getAuthentication())
                        .ifPresent(principal -> attributes.put(ACCOUNT_ID, principal.accountId()));
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, @Nullable Exception exception) {
    }
}
