package mudmk3.domain.room;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import mudmk3.domain.AbstractEntity;

import static java.util.Objects.requireNonNull;

/** 출구: 한 방에서 어느 방위로 어느 방에 가는지. Room 애그리거트 안의 엔티티다 */
@Entity
@Getter
@ToString(callSuper = true, exclude = {"room", "target"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Exit extends AbstractEntity {

    @Getter(AccessLevel.NONE)
    @ManyToOne
    private Room room;

    private Direction direction;

    @ManyToOne
    private Room target;

    static Exit create(Room room, Direction direction, Room target) {
        Exit exit = new Exit();
        exit.room = requireNonNull(room);
        exit.direction = requireNonNull(direction);
        exit.target = requireNonNull(target);
        return exit;
    }

    void changeTarget(Room target) {
        this.target = requireNonNull(target);
    }
}
