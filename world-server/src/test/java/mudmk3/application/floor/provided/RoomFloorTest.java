package mudmk3.application.floor.provided;

import jakarta.persistence.EntityManager;
import mudmk3.ApplicationServiceTest;
import mudmk3.MutableClock;
import mudmk3.application.floor.required.DroppedItemRepository;
import mudmk3.application.item.provided.ItemTypeEditor;
import mudmk3.application.item.provided.ItemTypeFinder;
import mudmk3.application.room.provided.RoomEditor;
import mudmk3.application.zone.provided.ZoneEditor;
import mudmk3.domain.floor.DroppedItem;
import mudmk3.domain.floor.FloorStack;
import mudmk3.domain.item.ItemCategory;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.item.SpiritStoneGrade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static mudmk3.domain.floor.FloorFixture.createFloorPlacementSaveRequest;
import static mudmk3.domain.item.ItemTypeFixture.createItemTypeSaveRequest;
import static mudmk3.domain.room.RoomFixture.createRoomSaveRequest;
import static mudmk3.domain.zone.ZoneFixture.createZoneSaveRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@ApplicationServiceTest
class RoomFloorTest {

    final RoomFloor roomFloor;
    final FloorPlacementEditor floorPlacementEditor;
    final DroppedItemRepository droppedItemRepository;
    final ItemTypeEditor itemTypeEditor;
    final ItemTypeFinder itemTypeFinder;
    final ZoneEditor zoneEditor;
    final RoomEditor roomEditor;
    final MutableClock clock;
    final EntityManager entityManager;

    Long roomId;
    ItemType hide;
    ItemType stone;

    RoomFloorTest(RoomFloor roomFloor, FloorPlacementEditor floorPlacementEditor, DroppedItemRepository droppedItemRepository,
                  ItemTypeEditor itemTypeEditor, ItemTypeFinder itemTypeFinder, ZoneEditor zoneEditor, RoomEditor roomEditor,
                  MutableClock clock, EntityManager entityManager) {
        this.roomFloor = roomFloor;
        this.floorPlacementEditor = floorPlacementEditor;
        this.droppedItemRepository = droppedItemRepository;
        this.itemTypeEditor = itemTypeEditor;
        this.itemTypeFinder = itemTypeFinder;
        this.zoneEditor = zoneEditor;
        this.roomEditor = roomEditor;
        this.clock = clock;
        this.entityManager = entityManager;
    }

    @BeforeEach
    void setUp() {
        Long zoneId = zoneEditor.create(createZoneSaveRequest("청석숲")).getId();
        roomId = roomEditor.create(createRoomSaveRequest(zoneId, "샘터")).getId();
        hide = itemTypeEditor.create(createItemTypeSaveRequest("늑대 가죽", ItemCategory.MATERIAL, 2));
        stone = itemTypeFinder.spiritStone(SpiritStoneGrade.LOW);
    }

    @Test
    void look() {
        floorPlacementEditor.create(roomId, createFloorPlacementSaveRequest(hide.getId(), 2, 10));
        roomFloor.drop(roomId, stone.getId(), 3);
        roomFloor.drop(roomId, hide.getId(), 1);
        flushAndClear();

        assertThat(roomFloor.look(roomId))
                .extracting(stack -> stack.itemType().getName(), FloorStack::count)
                .containsExactly(tuple("늑대 가죽", 3), tuple("하품 영석", 3));
        assertThat(roomFloor.look(roomId + 1000)).isEmpty();
    }

    @Test
    void lookPurgesExpired() {
        roomFloor.drop(roomId, hide.getId(), 2);
        flushAndClear();
        clock.advance(Duration.ofMinutes(4).plusSeconds(59));
        assertThat(roomFloor.look(roomId)).hasSize(1);

        clock.advance(Duration.ofSeconds(1));
        assertThat(roomFloor.look(roomId)).isEmpty();
        flushAndClear();

        assertThat(droppedItemRepository.findAllByRoomIdOrderByIdAsc(roomId)).isEmpty();
    }

    @Test
    void lookRefillsPlacement() {
        floorPlacementEditor.create(roomId, createFloorPlacementSaveRequest(hide.getId(), 3, 10));
        assertThat(roomFloor.take(roomId, hide.getId(), 3)).isEqualTo(3);
        flushAndClear();
        assertThat(roomFloor.look(roomId)).isEmpty();

        clock.advance(Duration.ofMinutes(10));
        assertThat(roomFloor.look(roomId)).extracting(FloorStack::count).containsExactly(3);
        flushAndClear();

        // 채운 것이 저장되어 다시 줍고 비우면 그때부터 재등장을 잰다
        assertThat(roomFloor.take(roomId, hide.getId(), 3)).isEqualTo(3);
        clock.advance(Duration.ofMinutes(9));
        assertThat(roomFloor.look(roomId)).isEmpty();
    }

    @Test
    void takeDroppedFirst() {
        floorPlacementEditor.create(roomId, createFloorPlacementSaveRequest(hide.getId(), 3, null));
        roomFloor.drop(roomId, hide.getId(), 2);
        flushAndClear();

        assertThat(roomFloor.take(roomId, hide.getId(), 2)).isEqualTo(2);
        flushAndClear();

        assertThat(droppedItemRepository.findAllByRoomIdOrderByIdAsc(roomId)).isEmpty();
        assertThat(roomFloor.look(roomId)).extracting(FloorStack::count).containsExactly(3);
    }

    @Test
    void takeAcrossStacks() {
        floorPlacementEditor.create(roomId, createFloorPlacementSaveRequest(hide.getId(), 2, null));
        floorPlacementEditor.create(roomId, createFloorPlacementSaveRequest(hide.getId(), 2, null));
        roomFloor.drop(roomId, hide.getId(), 1);
        clock.advance(Duration.ofMinutes(1));
        roomFloor.drop(roomId, hide.getId(), 1);
        roomFloor.drop(roomId, stone.getId(), 5);
        flushAndClear();

        assertThat(roomFloor.take(roomId, hide.getId(), 5)).isEqualTo(5);
        flushAndClear();

        assertThat(droppedItemRepository.findAllByRoomIdOrderByIdAsc(roomId))
                .extracting(dropped -> dropped.getItemType().getName(), DroppedItem::getCount)
                .containsExactly(tuple("하품 영석", 5));
        assertThat(roomFloor.look(roomId))
                .extracting(stack -> stack.itemType().getName(), FloorStack::count)
                .containsExactly(tuple("늑대 가죽", 1), tuple("하품 영석", 5));
        // 바닥에 있는 것보다 많이 달라면 있는 만큼만 덜어 낸다
        assertThat(roomFloor.take(roomId, hide.getId(), 9)).isEqualTo(1);
        assertThat(roomFloor.take(roomId, hide.getId(), 1)).isZero();
    }

    @Test
    void takeSkipsExpired() {
        roomFloor.drop(roomId, hide.getId(), 2);
        clock.advance(Duration.ofMinutes(5));

        assertThat(roomFloor.take(roomId, hide.getId(), 2)).isZero();
    }

    @Test
    void dropKeepsOwnTimer() {
        roomFloor.drop(roomId, hide.getId(), 1);
        clock.advance(Duration.ofMinutes(3));
        roomFloor.drop(roomId, hide.getId(), 2);
        flushAndClear();

        assertThat(roomFloor.look(roomId)).extracting(FloorStack::count).containsExactly(3);
        clock.advance(Duration.ofMinutes(2));
        assertThat(roomFloor.look(roomId)).extracting(FloorStack::count).containsExactly(2);
        clock.advance(Duration.ofMinutes(3));
        assertThat(roomFloor.look(roomId)).isEmpty();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
