package mudmk3.application.mob;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import mudmk3.ApplicationServiceTest;
import mudmk3.QueryCounter;
import mudmk3.application.mob.provided.MobPlacementEditor;
import mudmk3.application.mob.provided.MobPlacementSaveRequest;
import mudmk3.application.mob.provided.MobTypeEditor;
import mudmk3.application.mob.provided.MobTypeFinder;
import mudmk3.application.mob.provided.MobTypeView;
import mudmk3.application.mob.provided.MobTypeView.PlacementView;
import mudmk3.application.room.provided.RoomEditor;
import mudmk3.application.zone.provided.ZoneEditor;
import mudmk3.domain.room.Room;
import org.junit.jupiter.api.Test;

import java.util.List;

import static mudmk3.domain.mob.MobFixture.rabbit;
import static mudmk3.domain.mob.MobFixture.request;
import static mudmk3.domain.mob.MobFixture.wolf;
import static mudmk3.domain.room.RoomFixture.createRoomSaveRequest;
import static mudmk3.domain.zone.ZoneFixture.createZoneSaveRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/** 몹 관리 조회가 내는 쿼리 수 */
@ApplicationServiceTest
class MobTypeQueryServiceTest {

    private final MobTypeFinder mobTypeFinder;
    private final MobTypeEditor mobTypeEditor;
    private final MobPlacementEditor mobPlacementEditor;
    private final ZoneEditor zoneEditor;
    private final RoomEditor roomEditor;
    private final EntityManager entityManager;
    private final QueryCounter queryCounter;

    MobTypeQueryServiceTest(MobTypeFinder mobTypeFinder, MobTypeEditor mobTypeEditor,
                            MobPlacementEditor mobPlacementEditor, ZoneEditor zoneEditor, RoomEditor roomEditor,
                            EntityManager entityManager, EntityManagerFactory entityManagerFactory) {
        this.mobTypeFinder = mobTypeFinder;
        this.mobTypeEditor = mobTypeEditor;
        this.mobPlacementEditor = mobPlacementEditor;
        this.zoneEditor = zoneEditor;
        this.roomEditor = roomEditor;
        this.entityManager = entityManager;
        this.queryCounter = new QueryCounter(entityManagerFactory);
    }

    /** 몹 종류가 없으면 목록 쿼리 하나만 내고 빈 {@code IN} 쿼리를 내지 않는다 */
    @Test
    void viewAllEmptyQueriesOnce() {
        flushAndClear();
        queryCounter.reset();

        List<MobTypeView> views = mobTypeFinder.viewAll();

        assertThat(views).isEmpty();
        assertThat(queryCounter.prepared()).isEqualTo(1);
    }

    /** 배치된 방의 이름은 방 수와 상관없이 한 번에 읽는다 */
    @Test
    void viewAllPlacementRoomNamesQueryCountConstant() {
        MobTypeView wolf = mobTypeEditor.create(request(wolf()));
        MobTypeView rabbit = mobTypeEditor.create(request(rabbit()));
        Room spring = prepareRoom("샘터");
        mobPlacementEditor.create(spring.getId(), new MobPlacementSaveRequest(wolf.id(), 1));
        long oneRoom = countViewAll();

        Room path = prepareRoom("오솔길");
        Room hill = prepareRoom("언덕");
        mobPlacementEditor.create(path.getId(), new MobPlacementSaveRequest(wolf.id(), 2));
        mobPlacementEditor.create(hill.getId(), new MobPlacementSaveRequest(rabbit.id(), 3));
        long threeRooms = countViewAll();

        assertThat(threeRooms).isEqualTo(oneRoom);
        flushAndClear();
        List<MobTypeView> views = mobTypeFinder.viewAll();
        MobTypeView wolfView = views.stream().filter(view -> view.id().equals(wolf.id())).findFirst().orElseThrow();
        assertThat(wolfView.placements()).extracting(PlacementView::roomName, PlacementView::count)
                .containsExactly(tuple("샘터", 1), tuple("오솔길", 2));
        MobTypeView rabbitView = views.stream().filter(view -> view.id().equals(rabbit.id())).findFirst().orElseThrow();
        assertThat(rabbitView.placements()).extracting(PlacementView::roomName).containsExactly("언덕");
    }

    private long countViewAll() {
        flushAndClear();
        queryCounter.reset();
        mobTypeFinder.viewAll();
        return queryCounter.prepared();
    }

    private Room prepareRoom(String name) {
        Long zoneId = zoneEditor.create(createZoneSaveRequest(name + " 지역")).getId();
        return roomEditor.create(createRoomSaveRequest(zoneId, name));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
