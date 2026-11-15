package mudmk3.application.mob.provided;

import jakarta.persistence.EntityManager;
import mudmk3.ApplicationServiceTest;
import mudmk3.application.room.provided.RoomEditor;
import mudmk3.application.zone.provided.ZoneEditor;
import mudmk3.domain.mob.MobPlacement;
import mudmk3.domain.room.Room;
import org.junit.jupiter.api.Test;

import static mudmk3.domain.mob.MobFixture.rabbit;
import static mudmk3.domain.mob.MobFixture.request;
import static mudmk3.domain.mob.MobFixture.wolf;
import static mudmk3.domain.room.RoomFixture.createRoomSaveRequest;
import static mudmk3.domain.zone.ZoneFixture.createZoneSaveRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ApplicationServiceTest
record MobPlacementEditorTest(MobPlacementEditor mobPlacementEditor, MobPlacementFinder mobPlacementFinder,
                              MobTypeEditor mobTypeEditor, ZoneEditor zoneEditor, RoomEditor roomEditor,
                              EntityManager entityManager) {

    @Test
    void createAndFindInRoom() {
        Room room = prepareRoom("샘터");
        Room other = prepareRoom("오솔길");
        MobTypeView wolf = mobTypeEditor.create(request(wolf()));
        MobTypeView rabbit = mobTypeEditor.create(request(rabbit()));

        MobPlacementView created = mobPlacementEditor.create(room.getId(), new MobPlacementSaveRequest(wolf.id(), 3));
        MobPlacementView second = mobPlacementEditor.create(room.getId(), new MobPlacementSaveRequest(rabbit.id(), 1));
        mobPlacementEditor.create(other.getId(), new MobPlacementSaveRequest(rabbit.id(), 2));
        flushAndClear();

        assertThat(created.roomId()).isEqualTo(room.getId());
        assertThat(created.mobId()).isEqualTo(wolf.id());
        assertThat(created.mobName()).isEqualTo("청모랑");
        assertThat(created.count()).isEqualTo(3);
        // 한 방에 배치 여럿, id 순
        assertThat(mobPlacementFinder.findAllInRoom(room.getId())).containsExactly(created, second);
    }

    @Test
    void findAllReadsMobTypeWithDrops() {
        Room room = prepareRoom("샘터");
        MobTypeView wolf = mobTypeEditor.create(request(wolf()));
        MobTypeView rabbit = mobTypeEditor.create(request(rabbit()));
        MobPlacementView first = mobPlacementEditor.create(room.getId(), new MobPlacementSaveRequest(rabbit.id(), 2));
        MobPlacementView second = mobPlacementEditor.create(room.getId(), new MobPlacementSaveRequest(wolf.id(), 1));
        flushAndClear();

        var all = mobPlacementFinder.findAll();
        entityManager.clear();

        assertThat(all).extracting(MobPlacement::getId).containsExactly(first.id(), second.id());
        assertThat(all).extracting(placement -> placement.getMobType().getName()).containsExactly("들토끼", "청모랑");
        assertThat(all.getFirst().getMobType().getFixedDrops()).isEmpty();
        assertThat(all.getFirst().getMobType().getTierDrops()).isEmpty();
    }

    @Test
    void unknownRoomFail() {
        MobTypeView wolf = mobTypeEditor.create(request(wolf()));

        assertThatThrownBy(() -> mobPlacementEditor.create(9L, new MobPlacementSaveRequest(wolf.id(), 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("방을 찾을 수 없습니다. id: 9");
    }

    @Test
    void unknownMobFail() {
        Room room = prepareRoom("샘터");
        MobTypeView wolf = mobTypeEditor.create(request(wolf()));
        MobPlacementView placement = mobPlacementEditor.create(room.getId(), new MobPlacementSaveRequest(wolf.id(), 1));

        assertThatThrownBy(() -> mobPlacementEditor.create(room.getId(), new MobPlacementSaveRequest(999L, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("몹을 찾을 수 없습니다. id: 999");
        assertThatThrownBy(() -> mobPlacementEditor.update(placement.id(), new MobPlacementSaveRequest(999L, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("몹을 찾을 수 없습니다. id: 999");
    }

    @Test
    void missingMobIdFail() {
        assertThatThrownBy(() -> new MobPlacementSaveRequest(null, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("mobId 는 꼭 보내야 합니다");
    }

    @Test
    void countRangeFail() {
        Room room = prepareRoom("샘터");
        MobTypeView wolf = mobTypeEditor.create(request(wolf()));

        assertThatThrownBy(() -> mobPlacementEditor.create(room.getId(), new MobPlacementSaveRequest(wolf.id(), 11)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("몹 수는 1~10으로 정하십시오.");
    }

    @Test
    void updateChangesMobAndCount() {
        Room room = prepareRoom("샘터");
        MobTypeView wolf = mobTypeEditor.create(request(wolf()));
        MobTypeView rabbit = mobTypeEditor.create(request(rabbit()));
        MobPlacementView placement = mobPlacementEditor.create(room.getId(), new MobPlacementSaveRequest(wolf.id(), 3));
        flushAndClear();

        MobPlacementView updated = mobPlacementEditor.update(placement.id(), new MobPlacementSaveRequest(rabbit.id(), 7));
        flushAndClear();

        assertThat(updated).isEqualTo(new MobPlacementView(placement.id(), room.getId(), rabbit.id(), "들토끼", 7));
        assertThat(mobPlacementFinder.findAllInRoom(room.getId())).containsExactly(updated);
    }

    @Test
    void delete() {
        Room room = prepareRoom("샘터");
        MobTypeView wolf = mobTypeEditor.create(request(wolf()));
        MobPlacementView placement = mobPlacementEditor.create(room.getId(), new MobPlacementSaveRequest(wolf.id(), 3));
        flushAndClear();

        mobPlacementEditor.delete(placement.id());
        flushAndClear();

        assertThat(mobPlacementFinder.findAllInRoom(room.getId())).isEmpty();
    }

    @Test
    void unknownPlacementFail() {
        MobTypeView wolf = mobTypeEditor.create(request(wolf()));

        assertThatThrownBy(() -> mobPlacementEditor.update(9L, new MobPlacementSaveRequest(wolf.id(), 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("몹 배치를 찾을 수 없습니다. id: 9");
        assertThatThrownBy(() -> mobPlacementEditor.delete(9L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("몹 배치를 찾을 수 없습니다. id: 9");
    }

    @Test
    void roomDeletedRemovesPlacements() {
        Room room = prepareRoom("샘터");
        Room other = prepareRoom("오솔길");
        MobTypeView wolf = mobTypeEditor.create(request(wolf()));
        mobPlacementEditor.create(room.getId(), new MobPlacementSaveRequest(wolf.id(), 3));
        MobPlacementView kept = mobPlacementEditor.create(other.getId(), new MobPlacementSaveRequest(wolf.id(), 1));
        flushAndClear();

        roomEditor.delete(room.getId());
        flushAndClear();

        assertThat(mobPlacementFinder.findAllInRoom(room.getId())).isEmpty();
        assertThat(mobPlacementFinder.findAllInRoom(other.getId())).containsExactly(kept);
        // 배치가 지워졌으니 몹도 지울 수 있다(다른 방 배치를 치운 뒤)
        mobPlacementEditor.delete(kept.id());
        mobTypeEditor.delete(wolf.id());
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
