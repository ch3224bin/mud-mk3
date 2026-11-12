package mudmk3.domain.world;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import mudmk3.domain.AbstractEntity;
import mudmk3.domain.room.Room;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/** 세계 설정(행 하나): 새 캐릭터가 처음 서는 시작 방 */
@Entity
@Getter
@ToString(callSuper = true, exclude = "startRoom")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorldSettings extends AbstractEntity {

    // 시작 방은 findStartRoom() 으로 꺼낸다
    @Getter(AccessLevel.NONE)
    @ManyToOne
    private @Nullable Room startRoom;

    public static WorldSettings create() {
        return new WorldSettings();
    }

    public void designateStartRoom(Room room) {
        this.startRoom = requireNonNull(room);
    }

    public Optional<Room> findStartRoom() {
        return Optional.ofNullable(startRoom);
    }

    public boolean isStartRoom(Room room) {
        return room.equals(startRoom);
    }
}
