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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/** 세계 설정(행 하나): 새 캐릭터가 처음 서는 시작 방과 받는 시작 소지품(입력 순서, 30줄까지) */
@Entity
@Getter
@ToString(callSuper = true, exclude = {"startRoom", "startingItems"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorldSettings extends AbstractEntity {

    public static final int STARTING_ITEMS_MAX = 30;

    // 시작 방은 findStartRoom() 으로 꺼낸다
    @Getter(AccessLevel.NONE)
    @ManyToOne
    private @Nullable Room startRoom;

    @Getter(AccessLevel.NONE)
    private List<StartingItem> startingItems = new ArrayList<>();

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

    /** 시작 소지품(입력 순서, 바꿀 수 없음) */
    public List<StartingItem> getStartingItems() {
        return Collections.unmodifiableList(startingItems);
    }

    /**
     * 시작 소지품을 통째로 바꾼다. 같은 물건이 두 줄이면 안 된다. 물건 종류에 기대는 검사(칸·저물대 수·없는 물건)는
     * 물건 종류를 아는 애플리케이션 서비스가 먼저 한다
     */
    public void replaceStartingItems(List<StartingItem> items) {
        List<StartingItem> newItems = List.copyOf(requireNonNull(items));
        if (newItems.size() > STARTING_ITEMS_MAX) {
            throw new IllegalArgumentException("시작 소지품은 30줄까지 정할 수 있습니다.");
        }
        Set<Long> itemTypeIds = new HashSet<>();
        for (StartingItem item : newItems) {
            if (!itemTypeIds.add(item.itemTypeId())) {
                throw new IllegalArgumentException("같은 물건이 두 번 있습니다. id: " + item.itemTypeId());
            }
        }
        this.startingItems.clear();
        this.startingItems.addAll(newItems);
    }
}
