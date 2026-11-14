package mudmk3.application.npc.provided;

import jakarta.persistence.EntityManager;
import mudmk3.ApplicationServiceTest;
import mudmk3.application.room.provided.RoomEditor;
import mudmk3.application.zone.provided.ZoneEditor;
import mudmk3.domain.npc.Npc;
import mudmk3.domain.room.Room;
import mudmk3.domain.zone.Zone;
import org.junit.jupiter.api.Test;

import static mudmk3.domain.npc.NpcFixture.createNpcSaveRequest;
import static mudmk3.domain.room.RoomFixture.createRoomSaveRequest;
import static mudmk3.domain.zone.ZoneFixture.createZoneSaveRequest;
import static org.assertj.core.api.Assertions.assertThat;

@ApplicationServiceTest
record NpcFinderTest(NpcFinder npcFinder, NpcEditor npcEditor, ZoneEditor zoneEditor, RoomEditor roomEditor,
                     EntityManager entityManager) {

    @Test
    void findAllInRoom() {
        Zone zone = zoneEditor.create(createZoneSaveRequest("청우촌"));
        Room room = roomEditor.create(createRoomSaveRequest(zone.getId(), "마을 어귀"));
        Room other = roomEditor.create(createRoomSaveRequest(zone.getId(), "우물가"));
        Npc first = npcEditor.create(createNpcSaveRequest("이씨", room.getId()));
        npcEditor.create(createNpcSaveRequest("박씨", other.getId()));
        npcEditor.create(createNpcSaveRequest("최씨", null));
        Npc second = npcEditor.create(createNpcSaveRequest("왕씨", room.getId()));
        entityManager.flush();
        entityManager.clear();

        assertThat(npcFinder.findAllInRoom(room.getId())).extracting(Npc::getId)
                .containsExactly(first.getId(), second.getId());
    }

    @Test
    void findAllByName() {
        npcEditor.create(createNpcSaveRequest("최씨", null));
        npcEditor.create(createNpcSaveRequest("왕씨", null));

        assertThat(npcFinder.findAll()).extracting(Npc::getName).containsExactly("왕씨", "최씨");
    }

    @Test
    void existsByName() {
        npcEditor.create(createNpcSaveRequest("Wang", null));

        assertThat(npcFinder.existsByName("Wang")).isTrue();
        assertThat(npcFinder.existsByName("wANG")).isTrue();
        assertThat(npcFinder.existsByName("왕씨")).isFalse();
    }
}
