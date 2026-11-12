package mudmk3.application.world.provided;

import jakarta.persistence.EntityManager;
import mudmk3.ApplicationServiceTest;
import mudmk3.application.room.provided.RoomEditor;
import mudmk3.application.zone.provided.ZoneEditor;
import mudmk3.domain.room.Room;
import mudmk3.domain.zone.Zone;
import org.junit.jupiter.api.Test;

import static mudmk3.domain.room.RoomFixture.createRoomSaveRequest;
import static mudmk3.domain.zone.ZoneFixture.createZoneSaveRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ApplicationServiceTest
record WorldSettingsEditorTest(
        WorldSettingsEditor worldSettingsEditor,
        WorldSettingsFinder worldSettingsFinder,
        ZoneEditor zoneEditor,
        RoomEditor roomEditor,
        EntityManager entityManager
) {

    @Test
    void noStartRoom() {
        assertThat(worldSettingsFinder.findStartRoom()).isEmpty();
        assertThat(worldSettingsFinder.isStartRoom(1L)).isFalse();
    }

    @Test
    void designateStartRoom() {
        Zone zone = zoneEditor.create(createZoneSaveRequest("청석숲"));
        Room room = roomEditor.create(createRoomSaveRequest(zone.getId(), "청석숲 오솔길"));

        worldSettingsEditor.designateStartRoom(room.getId());
        flushAndClear();

        assertThat(worldSettingsFinder.findStartRoom()).hasValueSatisfying(start -> {
            assertThat(start.getName()).isEqualTo("청석숲 오솔길");
            assertThat(start.getZone().getName()).isEqualTo("청석숲");
        });
        assertThat(worldSettingsFinder.isStartRoom(room.getId())).isTrue();
    }

    @Test
    void designateAgainReplaces() {
        Zone zone = zoneEditor.create(createZoneSaveRequest("청석숲"));
        Room first = roomEditor.create(createRoomSaveRequest(zone.getId(), "청석숲 오솔길"));
        Room second = roomEditor.create(createRoomSaveRequest(zone.getId(), "샘터"));
        worldSettingsEditor.designateStartRoom(first.getId());
        flushAndClear();

        worldSettingsEditor.designateStartRoom(second.getId());
        flushAndClear();

        assertThat(worldSettingsFinder.findStartRoom()).hasValueSatisfying(start ->
                assertThat(start.getName()).isEqualTo("샘터"));
        assertThat(worldSettingsFinder.isStartRoom(first.getId())).isFalse();
        assertThat(worldSettingsFinder.isStartRoom(second.getId())).isTrue();
        assertThat(entityManager.createQuery("select count(w) from WorldSettings w", Long.class).getSingleResult())
                .isEqualTo(1);
    }

    @Test
    void designateMissingRoomFail() {
        assertThatThrownBy(() -> worldSettingsEditor.designateStartRoom(-1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
