package mudmk3.adapter.websocket;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.jspecify.annotations.Nullable;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.standard.StandardWebSocketUpgradeStrategy;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * 서블릿 컨테이너에 로그인 사용자를 숨기고 연결을 올린다. 톰캣은 로그인 사용자가 있는 연결을 HTTP 세션에 묶어 두고
 * 그 세션이 끝나면 안내 없이 1008로 닫는데, 그러면 {@link HttpSessionEndListener} 의 안내·4401과 겨룬다.
 * 세션 끝의 연결 닫기는 이 앱이 혼자 맡는다.
 */
class GameUpgradeStrategy extends StandardWebSocketUpgradeStrategy {

    @Override
    public void upgrade(ServerHttpRequest request, ServerHttpResponse response,
                        @Nullable String selectedProtocol, List<WebSocketExtension> selectedExtensions,
                        @Nullable Principal user, WebSocketHandler wsHandler, Map<String, Object> attrs)
            throws HandshakeFailureException {
        HttpServletRequest servletRequest = getHttpServletRequest(request);
        ServerHttpRequest withoutPrincipal = new ServletServerHttpRequest(new HttpServletRequestWrapper(servletRequest) {
            @Override
            public @Nullable Principal getUserPrincipal() {
                return null;
            }
        });
        super.upgrade(withoutPrincipal, response, selectedProtocol, selectedExtensions, user, wsHandler, attrs);
    }
}
