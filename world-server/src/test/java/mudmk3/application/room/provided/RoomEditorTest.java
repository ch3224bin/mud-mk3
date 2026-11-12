package mudmk3.application.room.provided;

import jakarta.persistence.EntityManager;
import mudmk3.ApplicationServiceTest;
import mudmk3.application.world.provided.WorldSettingsEditor;
import mudmk3.application.zone.provided.ZoneEditor;
import mudmk3.domain.room.Direction;
import mudmk3.domain.room.Exit;
import mudmk3.domain.room.Room;
import mudmk3.domain.zone.Zone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import static mudmk3.domain.room.Direction.NORTH;
import static mudmk3.domain.room.Direction.SOUTH;
import static mudmk3.domain.room.Direction.UP;
import static mudmk3.domain.room.RoomFixture.createRoomSaveRequest;
import static mudmk3.domain.zone.ZoneFixture.createZoneSaveRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ApplicationServiceTest
@RecordApplicationEvents
class RoomEditorTest {

    final RoomEditor roomEditor;
    final RoomFinder roomFinder;
    final ZoneEditor zoneEditor;
    final WorldSettingsEditor worldSettingsEditor;
    final EntityManager entityManager;

    Zone zone;
    Room a;
    Room b;
    Room c;

    RoomEditorTest(RoomEditor roomEditor, RoomFinder roomFinder, ZoneEditor zoneEditor,
                   WorldSettingsEditor worldSettingsEditor, EntityManager entityManager) {
        this.roomEditor = roomEditor;
        this.roomFinder = roomFinder;
        this.zoneEditor = zoneEditor;
        this.worldSettingsEditor = worldSettingsEditor;
        this.entityManager = entityManager;
    }

    @BeforeEach
    void setUp() {
        zone = zoneEditor.create(createZoneSaveRequest("청석숲"));
        a = roomEditor.create(createRoomSaveRequest(zone.getId(), "가"));
        b = roomEditor.create(createRoomSaveRequest(zone.getId(), "나"));
        c = roomEditor.create(createRoomSaveRequest(zone.getId(), "다"));
    }

    @Test
    void create() {
        Room room = roomEditor.create(new RoomSaveRequest(zone.getId(), "  청석숲 오솔길 ", "좁은 길이 굽어 있다."));
        flushAndClear();

        Room found = roomFinder.find(room.getId());
        assertThat(found.getName()).isEqualTo("청석숲 오솔길");
        assertThat(found.getDescription()).isEqualTo("좁은 길이 굽어 있다.");
        assertThat(found.getZone().getName()).isEqualTo("청석숲");
        assertThat(roomFinder.findAllInZone(zone.getId())).extracting(Room::getName)
                .containsExactly("가", "나", "다", "청석숲 오솔길");
    }

    @Test
    void createInMissingZoneFail() {
        assertThatThrownBy(() -> roomEditor.create(createRoomSaveRequest(-1L, "샘터")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void update() {
        Zone other = zoneEditor.create(createZoneSaveRequest("황풍곡"));

        roomEditor.update(a.getId(), new RoomSaveRequest(other.getId(), "바람 골짜기", "바람이 거세다."));
        flushAndClear();

        Room found = roomFinder.find(a.getId());
        assertThat(found.getName()).isEqualTo("바람 골짜기");
        assertThat(found.getDescription()).isEqualTo("바람이 거세다.");
        assertThat(found.getZone().getName()).isEqualTo("황풍곡");
        assertThat(roomFinder.findAllInZone(zone.getId())).extracting(Room::getName).containsExactly("나", "다");
        assertThat(roomFinder.findAllInZone(other.getId())).extracting(Room::getName).containsExactly("바람 골짜기");
    }

    @Test
    void putExitTwoWay() {
        Room room = roomEditor.putExit(a.getId(), NORTH, new ExitSaveRequest(b.getId(), true));
        flushAndClear();

        assertThat(room.findExit(NORTH)).isPresent();
        assertThat(targetName(a, NORTH)).isEqualTo("나");
        assertThat(targetName(b, SOUTH)).isEqualTo("가");
    }

    @Test
    void putExitTwoWayKeepsExistingBackExit() {
        roomEditor.putExit(b.getId(), SOUTH, new ExitSaveRequest(a.getId(), false));
        flushAndClear();

        roomEditor.putExit(a.getId(), NORTH, new ExitSaveRequest(b.getId(), true));
        flushAndClear();

        assertThat(targetName(a, NORTH)).isEqualTo("나");
        assertThat(roomFinder.find(b.getId()).getExits()).hasSize(1);
        assertThat(targetName(b, SOUTH)).isEqualTo("가");
    }

    @Test
    void putExitTwoWayConflictFail() {
        roomEditor.putExit(b.getId(), SOUTH, new ExitSaveRequest(c.getId(), false));
        flushAndClear();

        assertThatThrownBy(() -> roomEditor.putExit(a.getId(), NORTH, new ExitSaveRequest(b.getId(), true)))
                .isInstanceOf(ExitConflictException.class)
                .hasMessage("반대 방향 출구가 이미 다른 방으로 이어져 있습니다: 나 남");
        flushAndClear();

        assertThat(roomFinder.find(a.getId()).getExits()).isEmpty();
        assertThat(targetName(b, SOUTH)).isEqualTo("다");
    }

    @Test
    void putExitToSelfFail() {
        assertThatThrownBy(() -> roomEditor.putExit(a.getId(), NORTH, new ExitSaveRequest(a.getId(), true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("자기 자신으로 가는 출구는 만들 수 없습니다");
        flushAndClear();

        assertThat(roomFinder.find(a.getId()).getExits()).isEmpty();
    }

    @Test
    void putExitToMissingRoomFail() {
        assertThatThrownBy(() -> roomEditor.putExit(a.getId(), NORTH, new ExitSaveRequest(-1L, true)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void putExitOneWay() {
        roomEditor.putExit(a.getId(), NORTH, new ExitSaveRequest(b.getId(), false));
        flushAndClear();

        assertThat(targetName(a, NORTH)).isEqualTo("나");
        assertThat(roomFinder.find(b.getId()).getExits()).isEmpty();
    }

    @Test
    void putExitReplacesTarget() {
        roomEditor.putExit(a.getId(), NORTH, new ExitSaveRequest(b.getId(), false));
        flushAndClear();

        roomEditor.putExit(a.getId(), NORTH, new ExitSaveRequest(c.getId(), false));
        flushAndClear();

        assertThat(targetName(a, NORTH)).isEqualTo("다");
        assertThat(roomFinder.find(a.getId()).getExits()).hasSize(1);
    }

    @Test
    void deleteExitTwoWay() {
        roomEditor.putExit(a.getId(), NORTH, new ExitSaveRequest(b.getId(), true));
        flushAndClear();

        roomEditor.deleteExit(a.getId(), NORTH, true);
        flushAndClear();

        assertThat(roomFinder.find(a.getId()).getExits()).isEmpty();
        assertThat(roomFinder.find(b.getId()).getExits()).isEmpty();
    }

    @Test
    void deleteExitOneWay() {
        roomEditor.putExit(a.getId(), NORTH, new ExitSaveRequest(b.getId(), true));
        flushAndClear();

        roomEditor.deleteExit(a.getId(), NORTH, false);
        flushAndClear();

        assertThat(roomFinder.find(a.getId()).getExits()).isEmpty();
        assertThat(targetName(b, SOUTH)).isEqualTo("가");
    }

    @Test
    void deleteExitTwoWayKeepsUnrelatedBackExit() {
        roomEditor.putExit(a.getId(), NORTH, new ExitSaveRequest(b.getId(), false));
        roomEditor.putExit(b.getId(), SOUTH, new ExitSaveRequest(c.getId(), false));
        flushAndClear();

        roomEditor.deleteExit(a.getId(), NORTH, true);
        flushAndClear();

        assertThat(roomFinder.find(a.getId()).getExits()).isEmpty();
        assertThat(targetName(b, SOUTH)).isEqualTo("다");
    }

    @Test
    void deleteMissingExitDoesNothing() {
        roomEditor.deleteExit(a.getId(), NORTH, true);
        flushAndClear();

        assertThat(roomFinder.find(a.getId()).getExits()).isEmpty();
    }

    @Test
    void deleteRemovesIncomingExits() {
        roomEditor.putExit(a.getId(), NORTH, new ExitSaveRequest(b.getId(), true));
        roomEditor.putExit(c.getId(), UP, new ExitSaveRequest(b.getId(), false));
        roomEditor.putExit(c.getId(), NORTH, new ExitSaveRequest(a.getId(), false));
        flushAndClear();

        roomEditor.delete(b.getId());
        flushAndClear();

        assertThat(roomFinder.findOptional(b.getId())).isEmpty();
        assertThat(roomFinder.find(a.getId()).getExits()).isEmpty();
        assertThat(roomFinder.find(c.getId()).getExits()).extracting(Exit::getDirection).containsExactly(NORTH);
    }

    @Test
    void deleteStartRoomFail() {
        worldSettingsEditor.designateStartRoom(a.getId());
        flushAndClear();

        assertThatThrownBy(() -> roomEditor.delete(a.getId()))
                .isInstanceOf(StartRoomDeletionException.class)
                .hasMessage("시작 방은 지울 수 없습니다");
        flushAndClear();

        assertThat(roomFinder.findOptional(a.getId())).isPresent();
    }

    @Test
    void deletePublishesRoomDeletedEvent(ApplicationEvents events) {
        roomEditor.delete(b.getId());

        assertThat(events.stream(RoomDeletedEvent.class)).containsExactly(new RoomDeletedEvent(b.getId()));
    }

    private String targetName(Room room, Direction direction) {
        return roomFinder.find(room.getId()).findExit(direction).orElseThrow().getTarget().getName();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
