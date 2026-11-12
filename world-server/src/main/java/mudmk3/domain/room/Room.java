package mudmk3.domain.room;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import mudmk3.domain.AbstractEntity;
import mudmk3.domain.zone.Zone;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/** 방: 캐릭터가 서 있는 장소. 지역 하나에 속하고 방위마다 출구를 하나까지 가진다 */
@Entity
@Getter
@ToString(callSuper = true, exclude = {"zone", "exits"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room extends AbstractEntity {

    @ManyToOne
    private Zone zone;

    private String name;

    private String description;

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "room")
    private List<Exit> exits = new ArrayList<>();

    public static Room create(Zone zone, RoomSaveInfo info) {
        Room room = new Room();
        room.apply(zone, info);
        return room;
    }

    public void update(Zone zone, RoomSaveInfo info) {
        apply(zone, info);
    }

    /** 그 방위에 출구를 둔다. 이미 있으면 대상을 바꾼다 */
    public void setExit(Direction direction, Room target) {
        requireNonNull(direction);
        requireNonNull(target);
        if (this.equals(target)) {
            throw new IllegalArgumentException("자기 자신으로 가는 출구는 만들 수 없습니다");
        }
        findExit(direction).ifPresentOrElse(
                exit -> exit.changeTarget(target),
                () -> exits.add(Exit.create(this, direction, target)));
    }

    public void removeExit(Direction direction) {
        requireNonNull(direction);
        exits.removeIf(exit -> exit.getDirection() == direction);
    }

    public Optional<Exit> findExit(Direction direction) {
        return exits.stream()
                .filter(exit -> exit.getDirection() == direction)
                .findFirst();
    }

    /** 방위 선언 순서로 정렬한 출구 목록(바꿀 수 없음) */
    public List<Exit> getExits() {
        return exits.stream()
                .sorted(Comparator.comparing(Exit::getDirection))
                .toList();
    }

    private void apply(Zone zone, RoomSaveInfo info) {
        requireNonNull(info);
        this.zone = requireNonNull(zone);
        this.name = requireNonNull(info.name());
        this.description = requireNonNull(info.description());
    }
}
