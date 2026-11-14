package mudmk3.application.floor.provided;

import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import mudmk3.ApplicationServiceTest;
import mudmk3.MutableClock;
import mudmk3.application.floor.required.DroppedItemRepository;
import mudmk3.application.item.provided.ItemTypeEditor;
import mudmk3.application.room.provided.RoomEditor;
import mudmk3.application.room.provided.StartRoomDeletionException;
import mudmk3.application.world.provided.WorldSettingsEditor;
import mudmk3.application.zone.provided.ZoneEditor;
import mudmk3.domain.floor.FloorPlacement;
import mudmk3.domain.item.ItemCategory;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.room.Room;
import org.junit.jupiter.api.Test;

import static mudmk3.domain.floor.FloorFixture.createFloorPlacementSaveRequest;
import static mudmk3.domain.item.ItemTypeFixture.createItemTypeSaveRequest;
import static mudmk3.domain.room.RoomFixture.createRoomSaveRequest;
import static mudmk3.domain.zone.ZoneFixture.createZoneSaveRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ApplicationServiceTest
record FloorPlacementEditorTest(FloorPlacementEditor floorPlacementEditor, FloorPlacementFinder floorPlacementFinder,
                                RoomFloor roomFloor, DroppedItemRepository droppedItemRepository,
                                ItemTypeEditor itemTypeEditor, ZoneEditor zoneEditor, RoomEditor roomEditor,
                                WorldSettingsEditor worldSettingsEditor, EntityManager entityManager) {

    @Test
    void create() {
        Room room = prepareRoom();
        ItemType hide = prepareItemType("늑대 가죽");

        FloorPlacement placement = floorPlacementEditor.create(room.getId(), createFloorPlacementSaveRequest(hide.getId(), 3, 10));
        flushAndClear();

        FloorPlacement found = floorPlacementFinder.find(placement.getId());
        assertThat(found.getRoomId()).isEqualTo(room.getId());
        assertThat(found.getItemType().getName()).isEqualTo("늑대 가죽");
        assertThat(found.getCount()).isEqualTo(3);
        assertThat(found.findRespawnMinutes()).contains(10);
        assertThat(floorPlacementFinder.findAllInRoom(room.getId())).extracting(FloorPlacement::getId)
                .containsExactly(placement.getId());
    }

    @Test
    void createInvalidFail() {
        Room room = prepareRoom();
        ItemType hide = prepareItemType("늑대 가죽");

        assertThatThrownBy(() -> floorPlacementEditor.create(room.getId(), createFloorPlacementSaveRequest(hide.getId(), 0, null)))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> floorPlacementEditor.create(room.getId(), createFloorPlacementSaveRequest(hide.getId(), 1, 1441)))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void unknownRoomFail() {
        ItemType hide = prepareItemType("늑대 가죽");

        assertThatThrownBy(() -> floorPlacementEditor.create(999L, createFloorPlacementSaveRequest(hide.getId(), 1, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("방을 찾을 수 없습니다. id: 999");
    }

    @Test
    void unknownItemTypeFail() {
        Room room = prepareRoom();

        assertThatThrownBy(() -> floorPlacementEditor.create(room.getId(), createFloorPlacementSaveRequest(999L, 1, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("물건 종류를 찾을 수 없습니다. id: 999");
    }

    @Test
    void unknownPlacementFail() {
        assertThatThrownBy(() -> floorPlacementFinder.find(9L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("바닥 물건을 찾을 수 없습니다. id: 9");
        assertThatThrownBy(() -> floorPlacementEditor.delete(9L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("바닥 물건을 찾을 수 없습니다. id: 9");
    }

    @Test
    void update() {
        Room room = prepareRoom();
        ItemType hide = prepareItemType("늑대 가죽");
        ItemType pill = prepareItemType("회춘단");
        FloorPlacement placement = floorPlacementEditor.create(room.getId(), createFloorPlacementSaveRequest(hide.getId(), 3, 10));
        roomFloor.take(room.getId(), hide.getId(), 3);
        flushAndClear();

        floorPlacementEditor.update(placement.getId(), createFloorPlacementSaveRequest(pill.getId(), 5, null));
        flushAndClear();

        FloorPlacement found = floorPlacementFinder.find(placement.getId());
        assertThat(found.getItemType().getName()).isEqualTo("회춘단");
        assertThat(found.getCount()).isEqualTo(5);
        assertThat(found.findRespawnMinutes()).isEmpty();
        assertThat(found.availableAt(MutableClock.START)).isEqualTo(5);
    }

    @Test
    void delete() {
        Room room = prepareRoom();
        ItemType hide = prepareItemType("늑대 가죽");
        FloorPlacement placement = floorPlacementEditor.create(room.getId(), createFloorPlacementSaveRequest(hide.getId(), 3, 10));
        flushAndClear();

        floorPlacementEditor.delete(placement.getId());
        flushAndClear();

        assertThat(floorPlacementFinder.findAllInRoom(room.getId())).isEmpty();
    }

    @Test
    void deleteRoomRemovesFloorItems() {
        Room room = prepareRoom();
        ItemType hide = prepareItemType("늑대 가죽");
        floorPlacementEditor.create(room.getId(), createFloorPlacementSaveRequest(hide.getId(), 3, 10));
        roomFloor.drop(room.getId(), hide.getId(), 2);
        flushAndClear();

        roomEditor.delete(room.getId());
        flushAndClear();

        assertThat(floorPlacementFinder.findAllInRoom(room.getId())).isEmpty();
        assertThat(droppedItemRepository.findAllByRoomIdOrderByIdAsc(room.getId())).isEmpty();
    }

    @Test
    void deleteStartRoomKeepsFloorItems() {
        Room room = prepareRoom();
        worldSettingsEditor.designateStartRoom(room.getId());
        ItemType hide = prepareItemType("늑대 가죽");
        floorPlacementEditor.create(room.getId(), createFloorPlacementSaveRequest(hide.getId(), 3, 10));
        roomFloor.drop(room.getId(), hide.getId(), 2);
        flushAndClear();

        assertThatThrownBy(() -> roomEditor.delete(room.getId()))
                .isInstanceOf(StartRoomDeletionException.class);
        flushAndClear();

        assertThat(floorPlacementFinder.findAllInRoom(room.getId())).hasSize(1);
        assertThat(droppedItemRepository.findAllByRoomIdOrderByIdAsc(room.getId())).hasSize(1);
    }

    private Room prepareRoom() {
        Long zoneId = zoneEditor.create(createZoneSaveRequest("청석숲")).getId();
        return roomEditor.create(createRoomSaveRequest(zoneId, "샘터"));
    }

    private ItemType prepareItemType(String name) {
        return itemTypeEditor.create(createItemTypeSaveRequest(name, ItemCategory.MATERIAL, 2));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
