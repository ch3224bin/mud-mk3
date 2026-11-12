package mudmk3.application.game.provided;

/** 게임 접속과 플레이어 입력 처리를 제공한다 */
public interface GameConnector {

    /** 로그인한 계정의 연결이 열렸다. 캐릭터가 없으면 생성 흐름을, 있으면 세계에 들여보낸다 */
    void connect(String sessionId, Long accountId);

    /** 그 연결에서 입력 한 줄이 왔다 */
    void handle(String sessionId, String text);

    /** 그 연결이 닫혔다. 지금 그 캐릭터의 연결이면 세계에서 뺀다 */
    void disconnect(String sessionId);
}
