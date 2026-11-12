package mudmk3.adapter.websocket;

import lombok.extern.slf4j.Slf4j;
import mudmk3.application.game.required.GameCloseReason;
import mudmk3.application.game.required.GameMessage;
import mudmk3.application.game.required.GameMessageSender;
import mudmk3.support.stereotype.Adapter;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.NativeWebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 게임 메시지를 WebSocket 연결에 JSON({@code type} 이 맨 앞, 나머지는 record 필드)으로 보낸다.
 * 게임 처리는 전역 락을 쥔 채 보내므로 느린 클라이언트가 게임 전체를 막지 않게 한다:
 * 다른 스레드가 보내는 중이면 버퍼에 쌓고 곧장 돌아오며({@link ConcurrentWebSocketSessionDecorator}),
 * 한 번 보내기에 {@value #SEND_TIME_LIMIT_MS}ms 를 넘기거나 버퍼가 {@value #BUFFER_SIZE_LIMIT} 바이트를 넘으면 그 연결을 끊는다.
 */
@Slf4j
@Adapter
public class WebSocketGameMessageSender implements GameMessageSender {

    static final int SEND_TIME_LIMIT_MS = 5000;
    static final int BUFFER_SIZE_LIMIT = 512 * 1024;
    /** 톰캣이 보내기 하나를 막고 기다리는 최대 시간(기본 20초). 보내는 스레드 자신이 막히는 시간도 위 한도로 묶는다 */
    static final String TOMCAT_BLOCKING_SEND_TIMEOUT = "org.apache.tomcat.websocket.BLOCKING_SEND_TIMEOUT";

    static final CloseStatus UNAUTHORIZED = new CloseStatus(4401);
    static final CloseStatus REPLACED = new CloseStatus(4409);

    private final JsonMapper jsonMapper;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public WebSocketGameMessageSender(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /** 열린 연결을 보낼 곳으로 등록한다 */
    public void register(WebSocketSession session) {
        if (session instanceof NativeWebSocketSession nativeSession) {
            jakarta.websocket.Session standard = nativeSession.getNativeSession(jakarta.websocket.Session.class);
            if (standard != null) {
                standard.getUserProperties().put(TOMCAT_BLOCKING_SEND_TIMEOUT, (long) SEND_TIME_LIMIT_MS);
            }
        }
        sessions.put(session.getId(),
                new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT));
    }

    /** 닫힌 연결을 뺀다 */
    public void unregister(String sessionId) {
        sessions.remove(sessionId);
    }

    @Override
    public void send(String sessionId, GameMessage message) {
        sendText(sessionId, toJson(message));
    }

    /** 프로토콜 오류({@code {"type":"error","code":..,"text":..}})를 보낸다 */
    public void sendError(String sessionId, String code, String text) {
        sendText(sessionId, errorJson(code, text));
    }

    @Override
    public void close(String sessionId, GameCloseReason reason) {
        close(sessionId, switch (reason) {
            case QUIT -> CloseStatus.NORMAL;
            case REPLACED -> REPLACED;
            case WORLD_NOT_READY -> CloseStatus.SERVICE_OVERLOAD;
        });
    }

    /** 그 연결을 닫기 코드와 함께 닫는다. 없는 연결이면 무시한다 */
    public void close(String sessionId, CloseStatus status) {
        WebSocketSession session = sessions.get(sessionId);
        if (session == null) return;
        try {
            session.close(status);
        } catch (IOException | RuntimeException e) {
            log.debug("WebSocket 연결을 닫지 못했습니다. session: {}", sessionId, e);
        }
    }

    String toJson(GameMessage message) {
        ObjectNode node = jsonMapper.createObjectNode();
        node.put("type", message.type());
        node.setAll((ObjectNode) jsonMapper.valueToTree(message));
        return jsonMapper.writeValueAsString(node);
    }

    String errorJson(String code, String text) {
        ObjectNode node = jsonMapper.createObjectNode();
        node.put("type", "error");
        node.put("code", code);
        node.put("text", text);
        return jsonMapper.writeValueAsString(node);
    }

    private void sendText(String sessionId, String json) {
        WebSocketSession session = sessions.get(sessionId);
        if (session == null || !session.isOpen()) return;
        try {
            session.sendMessage(new TextMessage(json));
        } catch (IOException | RuntimeException e) {
            // 한도를 넘었거나 이미 끊긴 연결이다. 끊김은 핸들러의 닫힘 처리가 정리한다
            log.debug("WebSocket 메시지를 보내지 못했습니다. session: {}", sessionId, e);
        }
    }
}
