package mudmk3.adapter.websocket;

import lombok.extern.slf4j.Slf4j;
import mudmk3.application.game.required.GameCloseReason;
import mudmk3.application.game.required.GameMessage;
import mudmk3.application.game.required.GameMessageSender;
import mudmk3.support.stereotype.Adapter;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.NativeWebSocketSession;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * 게임 메시지를 WebSocket 연결에 JSON({@code type} 이 맨 앞, 나머지는 record 필드)으로 보낸다.
 * <p>게임 처리는 전역 락을 쥔 채 보내므로 보내기는 막히지 않는다: {@code send}·{@code close} 는 연결마다 둔 보내기 줄에
 * 넣고 곧장 돌아오며, 연결마다 한 번에 하나의 보내기 작업(가상 스레드)이 줄을 차례로 비운다(순서 유지, 쓰는 스레드 하나).
 * {@code close} 도 줄에 들어가므로 앞서 넣은 메시지를 다 보낸 뒤 닫힌다.
 * <p>읽지 않는 클라이언트: 아직 보내지 못한 메시지가 {@value #MAX_PENDING_BYTES} 바이트를 넘으면 줄을 비우고
 * {@link #TOO_SLOW}(4500)로 닫는다. 보내기 하나가 막히는 시간은 톰캣 블로킹 보내기 한도({@value #SEND_TIME_LIMIT_MS}ms)로
 * 묶어 막힌 보내기 작업도 결국 끝나고 연결을 닫는다. 닫힘은 핸들러의 닫힘 처리({@code disconnect})를 탄다.
 */
@Slf4j
@Adapter
public class WebSocketGameMessageSender implements GameMessageSender, DisposableBean {

    static final int SEND_TIME_LIMIT_MS = 5000;
    static final int MAX_PENDING_BYTES = 512 * 1024;
    /** 톰캣이 보내기 하나를 막고 기다리는 최대 시간(기본 20초) */
    static final String TOMCAT_BLOCKING_SEND_TIMEOUT = "org.apache.tomcat.websocket.BLOCKING_SEND_TIMEOUT";

    static final CloseStatus UNAUTHORIZED = new CloseStatus(4401);
    static final CloseStatus REPLACED = new CloseStatus(4409);
    /** 클라이언트가 메시지를 받아 가지 않아 보낼 것이 너무 밀림(또는 보내기 실패) */
    static final CloseStatus TOO_SLOW = CloseStatus.SESSION_NOT_RELIABLE;

    private final JsonMapper jsonMapper;
    private final Map<String, Outbound> outbounds = new ConcurrentHashMap<>();
    private final ExecutorService sendExecutor =
            Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("ws-send-", 0).factory());

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
        outbounds.put(session.getId(), new Outbound(session));
    }

    /** 닫힌 연결을 빼고 보내지 못한 것을 버린다 */
    public void unregister(String sessionId) {
        Outbound outbound = outbounds.remove(sessionId);
        if (outbound != null) {
            outbound.discard();
        }
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

    /** 앞서 넣은 메시지를 보낸 뒤 그 연결을 닫기 코드와 함께 닫는다. 없는 연결이면 무시한다 */
    public void close(String sessionId, CloseStatus status) {
        Outbound outbound = outbounds.get(sessionId);
        if (outbound != null) {
            outbound.enqueueClose(status);
        }
    }

    @Override
    public void destroy() {
        sendExecutor.shutdownNow();
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
        Outbound outbound = outbounds.get(sessionId);
        if (outbound != null) {
            outbound.enqueueText(json);
        }
    }

    /** 연결 하나의 보내기 줄. 줄 상태는 이 객체의 모니터로 지키고, 실제 쓰기는 보내기 작업 하나만 한다 */
    private final class Outbound {

        private final WebSocketSession session;
        private final Queue<Pending> queue = new ArrayDeque<>();
        private long pendingBytes;
        /** 보내기 작업이 돌고 있다 */
        private boolean draining;
        /** 닫기가 줄에 들어갔거나 버려졌다. 더는 받지 않는다 */
        private boolean finished;

        Outbound(WebSocketSession session) {
            this.session = session;
        }

        void enqueueText(String json) {
            int bytes = json.getBytes(StandardCharsets.UTF_8).length;
            synchronized (this) {
                if (finished) return;
                if (pendingBytes + bytes > MAX_PENDING_BYTES) {
                    log.info("WebSocket 클라이언트가 메시지를 받아 가지 않아 연결을 닫습니다. session: {}", session.getId());
                    queue.clear();
                    pendingBytes = 0;
                    finishWith(TOO_SLOW);
                    return;
                }
                queue.add(new Pending(new TextMessage(json), null, bytes));
                pendingBytes += bytes;
                startDrainIfIdle();
            }
        }

        synchronized void enqueueClose(CloseStatus status) {
            if (finished) return;
            finishWith(status);
        }

        synchronized void discard() {
            finished = true;
            queue.clear();
            pendingBytes = 0;
        }

        private void finishWith(CloseStatus status) {
            finished = true;
            queue.add(new Pending(null, status, 0));
            startDrainIfIdle();
        }

        private void startDrainIfIdle() {
            if (draining) return;
            draining = true;
            try {
                sendExecutor.execute(this::drain);
            } catch (RejectedExecutionException e) {
                // 서버가 내려가는 중
                draining = false;
            }
        }

        private void drain() {
            while (true) {
                Pending next;
                synchronized (this) {
                    next = queue.poll();
                    if (next == null) {
                        draining = false;
                        return;
                    }
                    pendingBytes -= next.bytes();
                }
                CloseStatus closeStatus = next.closeStatus();
                TextMessage message = next.message();
                if (closeStatus != null) {
                    closeSession(closeStatus);
                } else if (message != null && !sendMessage(message)) {
                    discard();
                    closeSession(TOO_SLOW);
                }
            }
        }

        private boolean sendMessage(TextMessage message) {
            try {
                if (!session.isOpen()) return false;
                session.sendMessage(message);
                return true;
            } catch (Exception e) {
                log.debug("WebSocket 메시지를 보내지 못했습니다. session: {}", session.getId(), e);
                return false;
            }
        }

        private void closeSession(CloseStatus status) {
            try {
                if (session.isOpen()) {
                    session.close(status);
                }
            } catch (Exception e) {
                log.debug("WebSocket 연결을 닫지 못했습니다. session: {}", session.getId(), e);
            }
        }
    }

    /** 줄에 든 것 하나: 보낼 메시지 또는 닫기 */
    private record Pending(@Nullable TextMessage message, @Nullable CloseStatus closeStatus, int bytes) {
    }
}
