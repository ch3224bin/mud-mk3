package mudmk3.domain.world;

import mudmk3.domain.room.Room;
import mudmk3.domain.room.RoomFixture;
import mudmk3.domain.zone.ZoneFixture;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorldSettingsTest {

    @Test
    void create() {
        WorldSettings settings = WorldSettings.create();

        assertThat(settings.findStartRoom()).isEmpty();
    }

    @Test
    void designateStartRoom() {
        WorldSettings settings = WorldSettings.create();
        Room room = RoomFixture.createRoom(ZoneFixture.createZone(), "청석숲 오솔길");

        settings.designateStartRoom(room);

        assertThat(settings.findStartRoom()).containsSame(room);
        assertThat(settings.isStartRoom(room)).isTrue();
    }

    @Test
    void isStartRoom() {
        WorldSettings settings = WorldSettings.create();
        Room start = RoomFixture.createRoom(ZoneFixture.createZone(), "청석숲 오솔길");
        Room other = RoomFixture.createRoom(ZoneFixture.createZone(), "샘터");

        assertThat(settings.isStartRoom(start)).isFalse();

        settings.designateStartRoom(start);

        assertThat(settings.isStartRoom(other)).isFalse();
    }
}
