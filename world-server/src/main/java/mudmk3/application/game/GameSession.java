package mudmk3.application.game;

import lombok.Getter;
import lombok.ToString;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static org.springframework.util.Assert.state;

/** 연결 하나의 게임 상태: 캐릭터 생성 중(이름 묻기 → 확인)인지, 세계에서 노는 중인지 */
@Getter
@ToString
final class GameSession {

    enum Phase { NAMING, CONFIRMING, PLAYING }

    private final Long accountId;
    private Phase phase;
    private @Nullable String pendingName;
    private @Nullable Long characterId;
    private @Nullable Long lastZoneId;
    private @Nullable PendingPurchase pending;

    private GameSession(Long accountId, Phase phase) {
        this.accountId = requireNonNull(accountId);
        this.phase = phase;
    }

    static GameSession naming(Long accountId) {
        return new GameSession(accountId, Phase.NAMING);
    }

    static GameSession playing(Long accountId, Long characterId) {
        GameSession session = new GameSession(accountId, Phase.PLAYING);
        session.characterId = requireNonNull(characterId);
        return session;
    }

    boolean isPlaying() {
        return phase == Phase.PLAYING;
    }

    void askName() {
        state(phase != Phase.PLAYING, "이미 캐릭터로 놀고 있습니다");
        this.phase = Phase.NAMING;
        this.pendingName = null;
    }

    void confirmName(String name) {
        state(phase == Phase.NAMING, "이름을 묻는 중이 아닙니다");
        this.phase = Phase.CONFIRMING;
        this.pendingName = requireNonNull(name);
    }

    void startPlaying(Long characterId) {
        state(phase != Phase.PLAYING, "이미 캐릭터로 놀고 있습니다");
        this.phase = Phase.PLAYING;
        this.pendingName = null;
        this.characterId = requireNonNull(characterId);
    }

    String requirePendingName() {
        return requireNonNull(pendingName, "확인할 이름이 없습니다");
    }

    Long requireCharacterId() {
        return requireNonNull(characterId, "캐릭터가 없습니다");
    }

    /** 그 지역의 방을 보게 된다. 앞서 본 지역과 다르면 true(지역에 들어섬) */
    boolean enterZone(Long zoneId) {
        boolean entered = !Objects.equals(lastZoneId, zoneId);
        this.lastZoneId = zoneId;
        return entered;
    }

    /** 확인을 기다리는 사기를 둔다. 확인 대기는 하나뿐이라 앞의 것은 사라진다 */
    void askConfirm(PendingPurchase purchase) {
        this.pending = requireNonNull(purchase);
    }

    /** 확인을 기다리는 사기(시간이 지났는지는 묻는 쪽이 본다) */
    Optional<PendingPurchase> findPending() {
        return Optional.ofNullable(pending);
    }

    void clearPending() {
        this.pending = null;
    }

    /** 상위 등급 영석을 깨야 해 확인을 기다리는 사기: 어느 상점에서 무엇을 몇 개, 언제 물었나 */
    record PendingPurchase(Long npcId, Long itemTypeId, int count, Instant askedAt) {
        PendingPurchase {
            requireNonNull(npcId);
            requireNonNull(itemTypeId);
            requireNonNull(askedAt);
        }
    }
}
