package mudmk3.adapter.websocket;

import lombok.RequiredArgsConstructor;
import mudmk3.application.game.provided.GameConnector;
import mudmk3.support.stereotype.Adapter;
import org.jspecify.annotations.Nullable;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * 게임 WebSocket 연결. 로그인한 연결이면 게임에 잇고, 아니면 까닭을 보내고 4401로 닫는다.
 * 받는 메시지는 {@code command} 하나뿐이다. 게임 포트는 트랜잭션 밖(WebSocket 스레드)에서 부른다.
 * 1차엔 HTTP 세션이 로그아웃·만료돼도 이미 열린 연결은 끊지 않는다(다음 접속부터 막힌다).
 */
@Adapter
@RequiredArgsConstructor
public class GameWebSocketHandler extends TextWebSocketHandler {

    static final String AUTH_REQUIRED = "AUTH_REQUIRED";
    static final String AUTH_REQUIRED_TEXT = "로그인이 필요합니다. 다시 로그인하십시오.";
    static final String BAD_MESSAGE = "BAD_MESSAGE";
    static final String BAD_MESSAGE_TEXT = "알 수 없는 메시지입니다.";

    private final GameConnector gameConnector;
    private final WebSocketGameMessageSender sender;
    private final JsonMapper jsonMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sender.register(session);
        if (!(session.getAttributes().get(GameHandshakeInterceptor.ACCOUNT_ID) instanceof Long accountId)) {
            sender.sendError(session.getId(), AUTH_REQUIRED, AUTH_REQUIRED_TEXT);
            sender.close(session.getId(), WebSocketGameMessageSender.UNAUTHORIZED);
            return;
        }
        gameConnector.connect(session.getId(), accountId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        if (!isAuthenticated(session)) return;
        InboundMessage inbound = parse(message.getPayload());
        if (inbound == null || !inbound.isCommand() || inbound.text() == null) {
            sender.sendError(session.getId(), BAD_MESSAGE, BAD_MESSAGE_TEXT);
            return;
        }
        gameConnector.handle(session.getId(), inbound.text());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sender.unregister(session.getId());
        if (isAuthenticated(session)) {
            gameConnector.disconnect(session.getId());
        }
    }

    private static boolean isAuthenticated(WebSocketSession session) {
        return session.getAttributes().get(GameHandshakeInterceptor.ACCOUNT_ID) instanceof Long;
    }

    private @Nullable InboundMessage parse(String payload) {
        try {
            return jsonMapper.readValue(payload, InboundMessage.class);
        } catch (JacksonException e) {
            return null;
        }
    }
}
