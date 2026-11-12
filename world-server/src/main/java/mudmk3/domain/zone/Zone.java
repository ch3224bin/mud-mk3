package mudmk3.domain.zone;

import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import mudmk3.domain.AbstractEntity;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

/** 존(지역): 방을 묶는 단위. 이름은 세계에서 하나뿐이다 */
@Entity
@Getter
@ToString(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Zone extends AbstractEntity {

    private String name;

    private String description;

    public static Zone create(ZoneSaveInfo info) {
        Zone zone = new Zone();
        zone.apply(info);
        return zone;
    }

    public void update(ZoneSaveInfo info) {
        apply(info);
    }

    private void apply(ZoneSaveInfo info) {
        requireNonNull(info);
        this.name = requireNonNull(info.name()).strip();
        this.description = requireNonNullElse(info.description(), "");
    }
}
