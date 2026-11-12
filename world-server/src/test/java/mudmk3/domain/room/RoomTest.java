package mudmk3.domain.room;

import mudmk3.domain.zone.Zone;
import mudmk3.domain.zone.ZoneFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static mudmk3.domain.room.Direction.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomTest {

    Zone zone;
    Room room;

    @BeforeEach
    void setUp() {
        zone = ZoneFixture.createZone();
        room = RoomFixture.createRoom(zone, "청석숲 오솔길");
    }

    @Test
    void create() {
        assertThat(room.getZone()).isSameAs(zone);
        assertThat(room.getName()).isEqualTo("청석숲 오솔길");
        assertThat(room.getDescription()).isEqualTo("청석숲 오솔길의 묘사다.");
        assertThat(room.getExits()).isEmpty();
    }

    @Test
    void createStripsName() {
        Room stripped = Room.create(zone, new RoomSaveInfo("  샘터 ", "맑은 샘이 솟는다."));

        assertThat(stripped.getName()).isEqualTo("샘터");
    }

    @Test
    void update() {
        Zone other = ZoneFixture.createZone("황풍곡");

        room.update(other, new RoomSaveInfo("바람 골짜기", "바람이 거세다."));

        assertThat(room.getZone()).isSameAs(other);
        assertThat(room.getName()).isEqualTo("바람 골짜기");
        assertThat(room.getDescription()).isEqualTo("바람이 거세다.");
    }

    @Test
    void setExit() {
        Room target = RoomFixture.createRoom(zone, "샘터");

        room.setExit(NORTH, target);

        assertThat(room.getExits()).hasSize(1);
        Exit exit = room.findExit(NORTH).orElseThrow();
        assertThat(exit.getDirection()).isEqualTo(NORTH);
        assertThat(exit.getTarget()).isSameAs(target);
        assertThat(room.findExit(SOUTH)).isEmpty();
    }

    @Test
    void setExitTwiceReplacesTarget() {
        Room first = RoomFixture.createRoom(zone, "샘터");
        Room second = RoomFixture.createRoom(zone, "바위 언덕");

        room.setExit(NORTH, first);
        room.setExit(NORTH, second);

        assertThat(room.getExits()).hasSize(1);
        assertThat(room.findExit(NORTH).orElseThrow().getTarget()).isSameAs(second);
    }

    @Test
    void setExitToSelfFail() {
        assertThatThrownBy(() -> room.setExit(NORTH, room))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("자기 자신으로 가는 출구는 만들 수 없습니다");
        assertThat(room.getExits()).isEmpty();
    }

    @Test
    void removeExit() {
        room.setExit(NORTH, RoomFixture.createRoom(zone, "샘터"));
        room.setExit(EAST, RoomFixture.createRoom(zone, "바위 언덕"));

        room.removeExit(NORTH);
        room.removeExit(WEST);

        assertThat(room.findExit(NORTH)).isEmpty();
        assertThat(room.getExits()).extracting(Exit::getDirection).containsExactly(EAST);
    }

    @Test
    void getExitsInDirectionOrder() {
        for (Direction direction : new Direction[]{DOWN, NORTH, SOUTHWEST, EAST, UP, NORTHWEST, SOUTH, SOUTHEAST, WEST, NORTHEAST}) {
            room.setExit(direction, RoomFixture.createRoom(zone, direction.koreanName()));
        }

        assertThat(room.getExits())
                .extracting(exit -> exit.getDirection().koreanName())
                .containsExactly("동", "서", "남", "북", "북동", "북서", "남동", "남서", "위", "아래");
    }

    @Test
    void getExitsIsUnmodifiable() {
        room.setExit(NORTH, RoomFixture.createRoom(zone, "샘터"));

        assertThatThrownBy(() -> room.getExits().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
