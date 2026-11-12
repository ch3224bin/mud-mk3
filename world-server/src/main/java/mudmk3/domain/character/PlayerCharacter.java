package mudmk3.domain.character;

import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import mudmk3.domain.AbstractEntity;

import static java.util.Objects.requireNonNull;

/**
 * 플레이어 캐릭터: 계정 하나에 하나. 이름은 세계에서 하나뿐이다.
 * 지금 서 있는 방은 id로만 갖는다(방을 지우면 캐릭터를 다른 방으로 옮기므로 FK를 두지 않는다).
 */
@Entity
@Getter
@ToString(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerCharacter extends AbstractEntity {

    private Long accountId;

    private CharacterName name;

    private Long roomId;

    public static PlayerCharacter create(CharacterCreateInfo info) {
        requireNonNull(info);
        PlayerCharacter character = new PlayerCharacter();
        character.accountId = requireNonNull(info.accountId());
        character.name = requireNonNull(info.name());
        character.roomId = requireNonNull(info.roomId());
        return character;
    }

    public void moveTo(Long roomId) {
        this.roomId = requireNonNull(roomId);
    }
}
