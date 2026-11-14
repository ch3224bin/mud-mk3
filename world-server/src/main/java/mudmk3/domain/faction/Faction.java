package mudmk3.domain.faction;

import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import mudmk3.domain.AbstractEntity;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

/** 성·종문: 지역을 소유할 수 있는 세력. 이름은 성과 종문을 통틀어 하나뿐이다 */
@Entity
@Getter
@ToString(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Faction extends AbstractEntity {

    private FactionKind kind;

    private String name;

    private String description;

    public static Faction create(FactionSaveInfo info) {
        Faction faction = new Faction();
        faction.apply(info);
        return faction;
    }

    public void update(FactionSaveInfo info) {
        apply(info);
    }

    private void apply(FactionSaveInfo info) {
        requireNonNull(info);
        this.kind = requireNonNull(info.kind());
        this.name = requireNonNull(info.name()).strip();
        this.description = requireNonNullElse(info.description(), "");
    }
}
