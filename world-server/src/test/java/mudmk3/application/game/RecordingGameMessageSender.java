package mudmk3.application.game;

import mudmk3.application.game.required.GameCloseReason;
import mudmk3.application.game.required.GameMessage;
import mudmk3.application.game.required.GameMessage.RoomMessage;
import mudmk3.application.game.required.GameMessageSender;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 보낸 메시지와 닫은 세션을 기록하는 가짜 sender */
public class RecordingGameMessageSender implements GameMessageSender {

    private final List<Sent> sent = new ArrayList<>();
    private final Map<String, GameCloseReason> closed = new LinkedHashMap<>();

    @Override
    public synchronized void send(String sessionId, GameMessage message) {
        sent.add(new Sent(sessionId, message));
    }

    @Override
    public synchronized void close(String sessionId, GameCloseReason reason) {
        closed.put(sessionId, reason);
    }

    public synchronized void clear() {
        sent.clear();
        closed.clear();
    }

    /** 그 세션이 받은 메시지(순서대로). 조용한 방 상태({@code quiet} 인 room)는 빼고 {@link #quietRoomsTo} 로 따로 본다 */
    public synchronized List<GameMessage> messagesTo(String sessionId) {
        return allMessagesTo(sessionId).stream()
                .filter(message -> !isQuietRoom(message))
                .toList();
    }

    /** 그 세션이 받은 조용한 방 상태(순서대로) */
    public synchronized List<RoomMessage> quietRoomsTo(String sessionId) {
        return allMessagesTo(sessionId).stream()
                .filter(RecordingGameMessageSender::isQuietRoom)
                .map(RoomMessage.class::cast)
                .toList();
    }

    /** 그 세션이 받은 모든 메시지(조용한 방 상태 포함, 순서대로) */
    public synchronized List<GameMessage> allMessagesTo(String sessionId) {
        return sent.stream()
                .filter(s -> s.sessionId().equals(sessionId))
                .map(Sent::message)
                .toList();
    }

    private static boolean isQuietRoom(GameMessage message) {
        return message instanceof RoomMessage room && room.quiet();
    }

    /** 보낸 차례대로 모든 메시지(세션 사이의 순서를 볼 때) */
    public synchronized List<Sent> sent() {
        return List.copyOf(sent);
    }

    public <T extends GameMessage> List<T> messagesTo(String sessionId, Class<T> type) {
        return messagesTo(sessionId).stream()
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
    }

    public <T extends GameMessage> T lastTo(String sessionId, Class<T> type) {
        List<T> messages = messagesTo(sessionId, type);
        if (messages.isEmpty()) {
            throw new AssertionError(sessionId + " 에 " + type.getSimpleName() + " 메시지가 없다: " + messagesTo(sessionId));
        }
        return messages.getLast();
    }

    public synchronized Optional<GameCloseReason> closeReasonOf(String sessionId) {
        return Optional.ofNullable(closed.get(sessionId));
    }

    public record Sent(String sessionId, GameMessage message) {
    }
}
