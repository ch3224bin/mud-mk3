package mudmk3.domain.floor;

import mudmk3.domain.item.ItemType;
import mudmk3.domain.item.ItemTypeFixture;
import mudmk3.domain.item.SpiritStoneGrade;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static mudmk3.domain.floor.FloorFixture.createFloorPlacementSaveInfo;
import static mudmk3.domain.floor.FloorFixture.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class FloorStacksTest {

    static final Instant T0 = Instant.parse("2026-11-14T00:00:00Z");

    final ItemType hide = withId(ItemTypeFixture.createWolfHide(), 5L);
    final ItemType stone = withId(ItemType.spiritStone(SpiritStoneGrade.LOW), 1L);
    final ItemType pill = withId(ItemTypeFixture.createPill(), 7L);

    @Test
    void groupsByItemTypeInFirstOrder() {
        List<FloorPlacement> placements = List.of(FloorPlacement.create(3L, hide, createFloorPlacementSaveInfo(2, 10)));
        List<DroppedItem> dropped = List.of(
                DroppedItem.drop(3L, stone, 3, T0),
                DroppedItem.drop(3L, hide, 1, T0),
                DroppedItem.drop(3L, pill, 4, T0.minus(Duration.ofMinutes(5))));

        assertThat(FloorStacks.of(placements, dropped, T0))
                .extracting(stack -> stack.itemType().getName(), FloorStack::count)
                .containsExactly(tuple("늑대 가죽", 3), tuple("하품 영석", 3));
    }

    @Test
    void emptyPlacementLeftOut() {
        FloorPlacement empty = FloorPlacement.create(3L, hide, createFloorPlacementSaveInfo(2, null));
        empty.take(2, T0);
        FloorPlacement pills = FloorPlacement.create(3L, pill, createFloorPlacementSaveInfo(1, null));

        assertThat(FloorStacks.of(List.of(empty, pills), List.of(DroppedItem.drop(3L, hide, 1, T0)), T0))
                .extracting(stack -> stack.itemType().getName(), FloorStack::count)
                .containsExactly(tuple("회춘단(연기 초)", 1), tuple("늑대 가죽", 1));
    }

    @Test
    void empty() {
        assertThat(FloorStacks.of(List.of(), List.of(), T0)).isEmpty();
    }
}
