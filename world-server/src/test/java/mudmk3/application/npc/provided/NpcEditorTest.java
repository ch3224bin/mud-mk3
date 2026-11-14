package mudmk3.application.npc.provided;

import jakarta.persistence.EntityManager;
import mudmk3.ApplicationServiceTest;
import mudmk3.application.room.provided.RoomEditor;
import mudmk3.application.room.provided.StartRoomDeletionException;
import mudmk3.application.world.provided.WorldSettingsEditor;
import mudmk3.application.zone.provided.ZoneEditor;
import mudmk3.domain.npc.DuplicateNpcNameException;
import mudmk3.domain.npc.Npc;
import mudmk3.domain.npc.NpcTopic;
import mudmk3.domain.room.Room;
import mudmk3.domain.zone.Zone;
import org.junit.jupiter.api.Test;

import java.util.List;

import static mudmk3.domain.npc.NpcFixture.createNpcSaveRequest;
import static mudmk3.domain.room.RoomFixture.createRoomSaveRequest;
import static mudmk3.domain.zone.ZoneFixture.createZoneSaveRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ApplicationServiceTest
record NpcEditorTest(NpcEditor npcEditor, NpcFinder npcFinder, ZoneEditor zoneEditor, RoomEditor roomEditor,
                     WorldSettingsEditor worldSettingsEditor, EntityManager entityManager) {

    @Test
    void createPlaced() {
        Room room = prepareRoom("마을 어귀");

        Npc npc = npcEditor.create(createNpcSaveRequest("왕씨", room.getId()));
        flushAndClear();

        Npc found = npcFinder.find(npc.getId());
        assertThat(found.getName()).isEqualTo("왕씨");
        assertThat(found.getGreeting()).isEqualTo("어서 오시오.");
        assertThat(found.getDefaultAnswer()).isEqualTo("글쎄, 잘 모르겠구려.");
        assertThat(found.getTopics()).extracting(NpcTopic::word).containsExactly("단약", "일거리");
        assertThat(found.findRoomId()).contains(room.getId());
    }

    @Test
    void createUnplaced() {
        Npc npc = npcEditor.create(new NpcSaveRequest(" 왕씨 ", "어서 오시오.", "글쎄.", null, false, null));
        flushAndClear();

        Npc found = npcFinder.find(npc.getId());
        assertThat(found.getName()).isEqualTo("왕씨");
        assertThat(found.getTopics()).isEmpty();
        assertThat(found.findRoomId()).isEmpty();
    }

    @Test
    void duplicateNameFail() {
        npcEditor.create(createNpcSaveRequest("Wang", null));

        assertThatThrownBy(() -> npcEditor.create(createNpcSaveRequest("wang", null)))
                .isInstanceOf(DuplicateNpcNameException.class)
                .hasMessage("이미 있는 NPC 이름입니다: wang");
    }

    @Test
    void updateToOtherNpcNameFail() {
        npcEditor.create(createNpcSaveRequest("왕씨", null));
        Npc other = npcEditor.create(createNpcSaveRequest("이씨", null));

        assertThatThrownBy(() -> npcEditor.update(other.getId(), createNpcSaveRequest("왕씨", null)))
                .isInstanceOf(DuplicateNpcNameException.class)
                .hasMessage("이미 있는 NPC 이름입니다: 왕씨");
    }

    @Test
    void updateKeepsOwnNameInOtherCase() {
        Npc npc = npcEditor.create(createNpcSaveRequest("wang", null));

        Npc updated = npcEditor.update(npc.getId(), createNpcSaveRequest("Wang", null));

        assertThat(updated.getName()).isEqualTo("Wang");
    }

    @Test
    void updateReplacesTopicsAndPlacement() {
        Room first = prepareRoom("마을 어귀");
        Room second = roomEditor.create(createRoomSaveRequest(first.getZone().getId(), "우물가"));
        Npc npc = npcEditor.create(createNpcSaveRequest("왕씨", first.getId()));
        flushAndClear();

        npcEditor.update(npc.getId(), new NpcSaveRequest("왕씨", "또 왔구려.", "모르오.",
                List.of(new NpcTopicRequest("[소문]", "청석성에 도적이 들었다 하오.")), false, second.getId()));
        flushAndClear();

        Npc found = npcFinder.find(npc.getId());
        assertThat(found.getGreeting()).isEqualTo("또 왔구려.");
        assertThat(found.getDefaultAnswer()).isEqualTo("모르오.");
        assertThat(found.getTopics()).containsExactly(new NpcTopic("소문", "청석성에 도적이 들었다 하오."));
        assertThat(found.findRoomId()).contains(second.getId());

        npcEditor.update(npc.getId(), createNpcSaveRequest("왕씨", null));
        flushAndClear();

        assertThat(npcFinder.find(npc.getId()).findRoomId()).isEmpty();
    }

    @Test
    void unknownRoomFail() {
        assertThatThrownBy(() -> npcEditor.create(createNpcSaveRequest("왕씨", 999L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("방을 찾을 수 없습니다. id: 999");
    }

    @Test
    void unknownNpcFail() {
        assertThatThrownBy(() -> npcFinder.find(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("NPC를 찾을 수 없습니다. id: 999");
    }

    @Test
    void duplicateTopicFail() {
        assertThatThrownBy(() -> npcEditor.create(new NpcSaveRequest("왕씨", "어서 오시오.", "글쎄.",
                List.of(new NpcTopicRequest("단약", "가"), new NpcTopicRequest("[단약]", "나")), false, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("같은 낱말이 두 번 있습니다: 단약");
    }

    @Test
    void delete() {
        Npc npc = npcEditor.create(createNpcSaveRequest("왕씨", null));
        flushAndClear();

        npcEditor.delete(npc.getId());
        flushAndClear();

        assertThat(npcFinder.findAll()).isEmpty();
    }

    @Test
    void deleteRoomUnplacesNpcs() {
        Room room = prepareRoom("마을 어귀");
        Npc wang = npcEditor.create(createNpcSaveRequest("왕씨", room.getId()));
        Npc lee = npcEditor.create(createNpcSaveRequest("이씨", room.getId()));
        flushAndClear();

        roomEditor.delete(room.getId());
        flushAndClear();

        assertThat(npcFinder.find(wang.getId()).findRoomId()).isEmpty();
        assertThat(npcFinder.find(lee.getId()).findRoomId()).isEmpty();
        assertThat(npcFinder.findAllInRoom(room.getId())).isEmpty();
    }

    @Test
    void deleteStartRoomKeepsNpcPlacement() {
        Room room = prepareRoom("마을 어귀");
        worldSettingsEditor.designateStartRoom(room.getId());
        Npc npc = npcEditor.create(createNpcSaveRequest("왕씨", room.getId()));
        flushAndClear();

        assertThatThrownBy(() -> roomEditor.delete(room.getId()))
                .isInstanceOf(StartRoomDeletionException.class);
        flushAndClear();

        assertThat(npcFinder.find(npc.getId()).findRoomId()).contains(room.getId());
    }

    @Test
    void llmFlagStored() {
        Npc npc = npcEditor.create(new NpcSaveRequest("왕씨", "어서 오시오.", "글쎄.", List.of(), true, null));
        flushAndClear();

        assertThat(npcFinder.find(npc.getId()).isLlmEnabled()).isTrue();
    }

    private Room prepareRoom(String name) {
        Zone zone = zoneEditor.create(createZoneSaveRequest("청우촌"));
        return roomEditor.create(createRoomSaveRequest(zone.getId(), name));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
