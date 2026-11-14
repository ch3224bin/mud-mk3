package mudmk3.domain.floor;

import mudmk3.domain.item.ItemType;
import mudmk3.domain.item.ItemTypeFixture;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DroppedItemTest {

    static final Instant T0 = Instant.parse("2026-11-14T00:00:00Z");

    final ItemType hide = ItemTypeFixture.createWolfHide();

    @Test
    void drop() {
        DroppedItem dropped = DroppedItem.drop(3L, hide, 2, T0);

        assertThat(dropped.getRoomId()).isEqualTo(3L);
        assertThat(dropped.getItemType()).isSameAs(hide);
        assertThat(dropped.getCount()).isEqualTo(2);
        assertThat(dropped.getExpiresAt()).isEqualTo(T0.plus(Duration.ofMinutes(5)));
    }

    @Test
    void dropInvalidCountFail() {
        assertThatThrownBy(() -> DroppedItem.drop(3L, hide, 0, T0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void expiresAfterFiveMinutes() {
        DroppedItem dropped = DroppedItem.drop(3L, hide, 1, T0);

        assertThat(dropped.isExpiredAt(T0.plus(Duration.ofMinutes(4).plusSeconds(59)))).isFalse();
        assertThat(dropped.isExpiredAt(T0.plus(Duration.ofMinutes(5)))).isTrue();
    }

    @Test
    void take() {
        DroppedItem dropped = DroppedItem.drop(3L, hide, 3, T0);

        dropped.take(2);

        assertThat(dropped.getCount()).isEqualTo(1);
        assertThatThrownBy(() -> dropped.take(2)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> dropped.take(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
