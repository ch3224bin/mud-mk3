package mudmk3.domain.zone;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import mudmk3.domain.AbstractEntity;
import mudmk3.domain.faction.Faction;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

/** 존(지역): 방을 묶는 단위. 이름은 세계에서 하나뿐이다. 성·종문 하나가 소유할 수 있다 */
@Entity
@Getter
@ToString(callSuper = true, exclude = "owner")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Zone extends AbstractEntity {

    private String name;

    private String description;

    // 소유는 findOwner() 로 꺼낸다
    @Getter(AccessLevel.NONE)
    @ManyToOne
    private @Nullable Faction owner;

    public static Zone create(ZoneSaveInfo info, @Nullable Faction owner) {
        Zone zone = new Zone();
        zone.apply(info, owner);
        return zone;
    }

    public void update(ZoneSaveInfo info, @Nullable Faction owner) {
        apply(info, owner);
    }

    public Optional<Faction> findOwner() {
        return Optional.ofNullable(owner);
    }

    private void apply(ZoneSaveInfo info, @Nullable Faction owner) {
        requireNonNull(info);
        this.name = requireNonNull(info.name()).strip();
        this.description = requireNonNullElse(info.description(), "");
        this.owner = owner;
    }
}
