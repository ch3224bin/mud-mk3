package mudmk3.adapter.webapi;

import mudmk3.application.world.provided.WorldSettingsEditor;
import mudmk3.application.world.provided.WorldSettingsFinder;
import mudmk3.domain.room.Room;
import mudmk3.domain.world.WorldSettings;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.Optional;

import static mudmk3.adapter.webapi.ZoneApiWebMvcTest.room;
import static mudmk3.adapter.webapi.ZoneApiWebMvcTest.zone;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(WorldApi.class)
@MockitoBean(types = {WorldSettingsFinder.class, WorldSettingsEditor.class})
record WorldApiWebMvcTest(MockMvcTester mvc, WorldSettingsFinder worldSettingsFinder,
                          WorldSettingsEditor worldSettingsEditor) {

    @Test
    void getWithoutStartRoom() {
        when(worldSettingsFinder.findStartRoom()).thenReturn(Optional.empty());

        assertThat(mvc.get().uri("/api/admin/world"))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("""
                        {"startRoomId":null,"startRoomName":null,"startZoneName":null}""");
    }

    @Test
    void get() {
        when(worldSettingsFinder.findStartRoom()).thenReturn(Optional.of(room(zone(1L, "청석숲"), 10L, "샘터")));

        assertThat(mvc.get().uri("/api/admin/world"))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("""
                        {"startRoomId":10,"startRoomName":"샘터","startZoneName":"청석숲"}""");
    }

    @Test
    void designateStartRoom() {
        Room room = room(zone(1L, "청석숲"), 10L, "샘터");
        WorldSettings settings = WorldSettings.create();
        settings.designateStartRoom(room);
        when(worldSettingsEditor.designateStartRoom(10L)).thenReturn(settings);

        assertThat(mvc.put().uri("/api/admin/world/start-room")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"roomId":10}"""))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("""
                        {"startRoomId":10,"startRoomName":"샘터","startZoneName":"청석숲"}""");
    }

    @Test
    void designateStartRoomWithoutIdFail() {
        assertThat(mvc.put().uri("/api/admin/world/start-room")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(worldSettingsEditor);
    }
}
