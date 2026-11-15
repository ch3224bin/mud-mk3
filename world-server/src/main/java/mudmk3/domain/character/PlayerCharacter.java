package mudmk3.domain.character;

import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import mudmk3.domain.AbstractEntity;
import mudmk3.domain.shared.Realm;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static org.springframework.util.Assert.state;

/**
 * 플레이어 캐릭터: 계정 하나에 하나. 이름은 세계에서 하나뿐이다.
 * 지금 서 있는 방은 id로만 갖는다(방을 지우면 캐릭터를 다른 방으로 옮기므로 FK를 두지 않는다).
 * 경지는 새 캐릭터면 범인이다. 경지 열이 비어 있는 기존 행은 서버가 뜰 때 채운다.
 * 영근은 비어 있으면 아직 정하지 않은 것이고, 한 번 정하면 바꾸지 않는다.
 * 깎인 기혈은 전용 쿼리로만 읽고 쓴다(엔티티 저장은 그 열을 쓰지 않아 경지 지정·이동과 서로 되돌리지 않는다)
 */
@Entity
@Getter
@ToString(callSuper = true, exclude = "spiritRoots")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerCharacter extends AbstractEntity {

    private Long accountId;

    private CharacterName name;

    private Long roomId;

    @Getter(AccessLevel.NONE)
    private @Nullable Realm realm;

    @Getter(AccessLevel.NONE)
    private @Nullable Double qiBloodLost;

    /** 깎인 법력(6차). 엔티티는 읽기만 하고 게임·관리 코드는 전용 쿼리로 읽고 쓴다 */
    @Getter(AccessLevel.NONE)
    private @Nullable Double manaLost;

    @Getter(AccessLevel.NONE)
    private List<SpiritRootEntry> spiritRoots = new ArrayList<>();

    public static PlayerCharacter create(CharacterCreateInfo info) {
        requireNonNull(info);
        PlayerCharacter character = new PlayerCharacter();
        character.accountId = requireNonNull(info.accountId());
        character.name = requireNonNull(info.name());
        character.roomId = requireNonNull(info.roomId());
        character.realm = Realm.MORTAL;
        SpiritRoot spiritRoot = info.spiritRoot();
        if (spiritRoot != null) {
            character.spiritRoots.addAll(spiritRoot.entries());
        }
        return character;
    }

    /** 지금 경지. 열이 비어 있으면(서버 시작 채우기 전) IllegalStateException */
    public Realm getRealm() {
        state(realm != null, "캐릭터의 경지가 비어 있습니다");
        return requireNonNull(realm);
    }

    /** 엔티티를 읽은 때의 깎인 기혈(비어 있으면 null). 테스트만 쓴다 — 게임·관리 코드는 전용 쿼리로 읽는다 */
    public @Nullable Double getQiBloodLostAtLoad() {
        return qiBloodLost;
    }

    /** 영근. 아직 정하지 않았으면 비어 있다 */
    public Optional<SpiritRoot> findSpiritRoot() {
        return spiritRoots.isEmpty() ? Optional.empty() : Optional.of(SpiritRoot.of(spiritRoots));
    }

    /** 영근을 정한다. 한 번만 정할 수 있다 */
    public void chooseSpiritRoot(SpiritRoot spiritRoot) {
        requireNonNull(spiritRoot);
        state(spiritRoots.isEmpty(), "영근은 이미 정했습니다");
        spiritRoots.addAll(spiritRoot.entries());
    }

    public void changeRealm(Realm realm) {
        this.realm = requireNonNull(realm);
    }

    public void moveTo(Long roomId) {
        this.roomId = requireNonNull(roomId);
    }
}
