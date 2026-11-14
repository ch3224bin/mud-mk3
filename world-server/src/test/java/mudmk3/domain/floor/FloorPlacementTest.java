package mudmk3.domain.floor;

import mudmk3.domain.item.ItemType;
import mudmk3.domain.item.ItemTypeFixture;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static mudmk3.domain.floor.FloorFixture.createFloorPlacementSaveInfo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FloorPlacementTest {

    static final Instant T0 = Instant.parse("2026-11-14T00:00:00Z");

    final ItemType hide = ItemTypeFixture.createWolfHide();

    @Test
    void create() {
        FloorPlacement placement = FloorPlacement.create(3L, hide, createFloorPlacementSaveInfo(3, 10));

        assertThat(placement.getRoomId()).isEqualTo(3L);
        assertThat(placement.getItemType()).isSameAs(hide);
        assertThat(placement.getCount()).isEqualTo(3);
        assertThat(placement.findRespawnMinutes()).contains(10);
        assertThat(placement.availableAt(T0)).isEqualTo(3);
    }

    @Test
    void createInvalidFail() {
        assertThatThrownBy(() -> FloorPlacement.create(3L, hide, createFloorPlacementSaveInfo(0, 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("수는 1~9,999로 정하십시오.");
        assertThatThrownBy(() -> FloorPlacement.create(3L, hide, createFloorPlacementSaveInfo(10_000, 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("수는 1~9,999로 정하십시오.");
        assertThatThrownBy(() -> FloorPlacement.create(3L, hide, createFloorPlacementSaveInfo(1, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("재등장은 1~1,440분으로 정하십시오.");
        assertThatThrownBy(() -> FloorPlacement.create(3L, hide, createFloorPlacementSaveInfo(1, 1441)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("재등장은 1~1,440분으로 정하십시오.");
    }

    @Test
    void takeReducesRemaining() {
        FloorPlacement placement = FloorPlacement.create(3L, hide, createFloorPlacementSaveInfo(3, 10));

        placement.take(2, T0);

        assertThat(placement.availableAt(T0)).isEqualTo(1);
        assertThat(placement.getCount()).isEqualTo(3);
    }

    @Test
    void takeMoreThanAvailableFail() {
        FloorPlacement placement = FloorPlacement.create(3L, hide, createFloorPlacementSaveInfo(3, 10));

        assertThatThrownBy(() -> placement.take(4, T0)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> placement.take(0, T0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void respawnsAfterMinutes() {
        FloorPlacement placement = FloorPlacement.create(3L, hide, createFloorPlacementSaveInfo(3, 10));

        placement.take(3, T0);

        assertThat(placement.availableAt(T0.plus(Duration.ofMinutes(9).plusSeconds(59)))).isZero();
        assertThat(placement.availableAt(T0.plus(Duration.ofMinutes(10)))).isEqualTo(3);
    }

    @Test
    void refreshRefills() {
        FloorPlacement placement = FloorPlacement.create(3L, hide, createFloorPlacementSaveInfo(3, 10));
        placement.take(3, T0);
        Instant later = T0.plus(Duration.ofMinutes(10));

        placement.refresh(later);
        placement.take(1, later);

        // 다시 채운 뒤 한 개를 덜면 다음 재등장은 다시 0이 될 때부터 잰다
        assertThat(placement.availableAt(later.plus(Duration.ofHours(1)))).isEqualTo(2);
    }

    @Test
    void takeRefillsFirst() {
        FloorPlacement placement = FloorPlacement.create(3L, hide, createFloorPlacementSaveInfo(3, 10));
        placement.take(3, T0);

        placement.take(3, T0.plus(Duration.ofMinutes(10)));

        assertThat(placement.availableAt(T0.plus(Duration.ofMinutes(19)))).isZero();
        assertThat(placement.availableAt(T0.plus(Duration.ofMinutes(20)))).isEqualTo(3);
    }

    @Test
    void noRespawnStaysEmpty() {
        FloorPlacement placement = FloorPlacement.create(3L, hide, createFloorPlacementSaveInfo(3, null));

        placement.take(3, T0);
        placement.refresh(T0.plus(Duration.ofDays(30)));

        assertThat(placement.findRespawnMinutes()).isEmpty();
        assertThat(placement.availableAt(T0.plus(Duration.ofDays(30)))).isZero();
    }

    @Test
    void partialTakeDoesNotStartRespawn() {
        FloorPlacement placement = FloorPlacement.create(3L, hide, createFloorPlacementSaveInfo(3, 10));

        placement.take(1, T0);

        assertThat(placement.availableAt(T0.plus(Duration.ofMinutes(10)))).isEqualTo(2);
        assertThat(placement.availableAt(T0.plus(Duration.ofDays(1)))).isEqualTo(2);
    }

    @Test
    void updateResets() {
        FloorPlacement placement = FloorPlacement.create(3L, hide, createFloorPlacementSaveInfo(3, null));
        placement.take(3, T0);
        ItemType pill = ItemTypeFixture.createPill();

        placement.update(pill, createFloorPlacementSaveInfo(5, 20));

        assertThat(placement.getItemType()).isSameAs(pill);
        assertThat(placement.getCount()).isEqualTo(5);
        assertThat(placement.findRespawnMinutes()).contains(20);
        assertThat(placement.availableAt(T0)).isEqualTo(5);
        assertThat(placement.getRoomId()).isEqualTo(3L);
    }
}
