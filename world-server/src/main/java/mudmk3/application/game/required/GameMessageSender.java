package mudmk3.application.game.required;

/** 플레이어 연결에 게임 메시지를 보내고 연결을 닫는다 */
public interface GameMessageSender {

    /** 그 연결에 메시지를 보낸다. 없는 연결이면 무시한다 */
    void send(String sessionId, GameMessage message);

    /** 그 연결을 닫는다 */
    void close(String sessionId, GameCloseReason reason);
}
