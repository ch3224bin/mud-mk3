package mudmk3.domain.world;

import mudmk3.domain.room.Room;
import mudmk3.domain.room.RoomFixture;
import mudmk3.domain.zone.ZoneFixture;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void replaceStartingItems() {
        WorldSettings settings = WorldSettings.create();
        assertThat(settings.getStartingItems()).isEmpty();

        settings.replaceStartingItems(List.of(new StartingItem(3L, 1), new StartingItem(1L, 30)));
        settings.replaceStartingItems(List.of(new StartingItem(5L, 3), new StartingItem(1L, 30), new StartingItem(4L, 1)));

        assertThat(settings.getStartingItems())
                .containsExactly(new StartingItem(5L, 3), new StartingItem(1L, 30), new StartingItem(4L, 1));
        assertThatThrownBy(() -> settings.getStartingItems().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void replaceStartingItemsCopiesInput() {
        WorldSettings settings = WorldSettings.create();
        List<StartingItem> input = new ArrayList<>(List.of(new StartingItem(1L, 30)));

        settings.replaceStartingItems(input);
        input.add(new StartingItem(2L, 1));

        assertThat(settings.getStartingItems()).containsExactly(new StartingItem(1L, 30));
    }

    @Test
    void duplicateStartingItemFail() {
        WorldSettings settings = WorldSettings.create();

        assertThatThrownBy(() -> settings.replaceStartingItems(List.of(new StartingItem(1L, 3), new StartingItem(1L, 5))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(settings.getStartingItems()).isEmpty();
    }

    @Test
    void tooManyStartingItemsFail() {
        WorldSettings settings = WorldSettings.create();
        List<StartingItem> items = IntStream.rangeClosed(1, 31).mapToObj(id -> new StartingItem((long) id, 1)).toList();

        assertThatThrownBy(() -> settings.replaceStartingItems(items))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void startingItemCountFail() {
        assertThatThrownBy(() -> new StartingItem(1L, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StartingItem(1L, 1_000_000)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new StartingItem(1L, 999_999).count()).isEqualTo(999_999);
    }
}
