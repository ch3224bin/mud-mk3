package mudmk3.domain.game;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.concurrent.atomic.AtomicLong;

import static java.util.Objects.requireNonNull;

/**
 * 세계에 접속해 있는 플레이어(메모리 월드 상태). 캐릭터 하나에 하나다.
 * {@code enteredAt} 은 지금 방에 들어온 순서를 나타내는 증가 번호다(방 안 사람을 들어온 순서로 보여 준다).
 */
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OnlinePlayer {

    private static final AtomicLong ENTRY_SEQUENCE = new AtomicLong();

    private Long characterId;
    private Long accountId;
    private String name;
    private Long roomId;
    private String sessionId;
    private volatile long enteredAt;

    /** 그 방에서 세계에 들어온다 */
    public static OnlinePlayer enter(Long characterId, Long accountId, String name, Long roomId, String sessionId) {
        OnlinePlayer player = new OnlinePlayer();
        player.characterId = requireNonNull(characterId);
        player.accountId = requireNonNull(accountId);
        player.name = requireNonNull(name);
        player.sessionId = requireNonNull(sessionId);
        player.moveTo(roomId);
        return player;
    }

    /** 다른 방으로 옮긴다. 그 방에 막 들어온 사람이 된다 */
    public void moveTo(Long roomId) {
        this.roomId = requireNonNull(roomId);
        this.enteredAt = ENTRY_SEQUENCE.incrementAndGet();
    }

    /** 같은 캐릭터를 새 연결이 이어받는다(방과 들어온 순서는 그대로) */
    public void rebind(String sessionId) {
        this.sessionId = requireNonNull(sessionId);
    }

    public boolean isIn(Long roomId) {
        return this.roomId.equals(roomId);
    }
}
