package mudmk3.application.game.provided;

/**
 * 게임 접속과 플레이어 입력 처리를 제공한다.
 * <p>트랜잭션 밖에서 부른다. 구현은 서버 전역 게임 락을 잡은 뒤 자기 트랜잭션을 연다. 트랜잭션 안에서 부르면
 * DB 락을 쥔 채 게임 락을 기다리게 되어 교착할 수 있다. 메시지는 처리가 커밋된 뒤 락 안에서
 * {@code GameMessageSender} 로 보내므로, sender는 막히지 않아야 하고 {@code close} 가 동기로 {@link #disconnect} 를
 * 다시 부르더라도(같은 스레드 재진입) 안전해야 한다.
 */
public interface GameConnector {

    /** 로그인한 계정의 연결이 열렸다. 캐릭터가 없으면 생성 흐름을, 있으면 세계에 들여보낸다 */
    void connect(String sessionId, Long accountId);

    /** 그 연결에서 입력 한 줄이 왔다 */
    void handle(String sessionId, String text);

    /** 그 연결이 닫혔다. 지금 그 캐릭터의 연결이면 세계에서 뺀다 */
    void disconnect(String sessionId);
}
