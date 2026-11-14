package mudmk3.application.game;

import lombok.Getter;
import lombok.ToString;
import mudmk3.domain.character.SpiritRoot;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static org.springframework.util.Assert.state;

/**
 * 연결 하나의 게임 상태: 캐릭터 생성 중(이름 → 영근 → 확인)인지, 영근이 없는 기존 캐릭터가 영근을 고르는 중(영근 → 확인)인지,
 * 세계에서 노는 중인지
 */
@Getter
@ToString
final class GameSession {

    enum Phase { NAMING, ROOTING, CONFIRMING, ROOTING_EXISTING, CONFIRMING_EXISTING, PLAYING }

    private final Long accountId;
    private Phase phase;
    private @Nullable String pendingName;
    private @Nullable SpiritRoot pendingRoot;
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

    /** 영근이 없는 기존 캐릭터: 세계에 들어가기 전에 영근을 고른다(이름 단계가 없다) */
    static GameSession rootingExisting(Long accountId, Long characterId, String name) {
        GameSession session = new GameSession(accountId, Phase.ROOTING_EXISTING);
        session.characterId = requireNonNull(characterId);
        session.pendingName = requireNonNull(name);
        return session;
    }

    static GameSession playing(Long accountId, Long characterId) {
        GameSession session = new GameSession(accountId, Phase.PLAYING);
        session.characterId = requireNonNull(characterId);
        return session;
    }

    boolean isPlaying() {
        return phase == Phase.PLAYING;
    }

    /** 기존 캐릭터의 영근을 고르는 중(영근 묻기 또는 확인)인가 */
    boolean isExisting() {
        return phase == Phase.ROOTING_EXISTING || phase == Phase.CONFIRMING_EXISTING;
    }

    /** 새 캐릭터의 이름을 다시 묻는다(이름·영근을 버린다) */
    void askName() {
        state(phase == Phase.NAMING || phase == Phase.ROOTING || phase == Phase.CONFIRMING, "새 캐릭터를 만드는 중이 아닙니다");
        this.phase = Phase.NAMING;
        this.pendingName = null;
        this.pendingRoot = null;
    }

    /** 맞는 이름을 받아 영근을 묻는다 */
    void rooting(String name) {
        state(phase == Phase.NAMING, "이름을 묻는 중이 아닙니다");
        this.phase = Phase.ROOTING;
        this.pendingName = requireNonNull(name);
    }

    /** 확인 단계에서 영근 묻기로 돌아간다(이름은 그대로) */
    void askRoot() {
        state(phase == Phase.CONFIRMING || phase == Phase.CONFIRMING_EXISTING, "영근을 확인하는 중이 아닙니다");
        this.phase = phase == Phase.CONFIRMING ? Phase.ROOTING : Phase.ROOTING_EXISTING;
        this.pendingRoot = null;
    }

    /** 맞는 영근을 받아 확인을 묻는다 */
    void confirmRoot(SpiritRoot root) {
        state(phase == Phase.ROOTING || phase == Phase.ROOTING_EXISTING, "영근을 묻는 중이 아닙니다");
        this.phase = phase == Phase.ROOTING ? Phase.CONFIRMING : Phase.CONFIRMING_EXISTING;
        this.pendingRoot = requireNonNull(root);
    }

    void startPlaying(Long characterId) {
        state(phase != Phase.PLAYING, "이미 캐릭터로 놀고 있습니다");
        this.phase = Phase.PLAYING;
        this.pendingName = null;
        this.pendingRoot = null;
        this.characterId = requireNonNull(characterId);
    }

    String requirePendingName() {
        return requireNonNull(pendingName, "확인할 이름이 없습니다");
    }

    SpiritRoot requirePendingRoot() {
        return requireNonNull(pendingRoot, "확인할 영근이 없습니다");
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
