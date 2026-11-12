package mudmk3.adapter.webapi;

import mudmk3.adapter.security.WebMvcSecurityTestConfiguration;
import mudmk3.application.room.provided.ExitConflictException;
import mudmk3.application.room.provided.ExitSaveRequest;
import mudmk3.application.room.provided.RoomEditor;
import mudmk3.application.room.provided.RoomFinder;
import mudmk3.application.room.provided.RoomSaveRequest;
import mudmk3.application.room.provided.StartRoomDeletionException;
import mudmk3.application.world.provided.WorldSettingsFinder;
import mudmk3.domain.room.Direction;
import mudmk3.domain.room.Room;
import mudmk3.domain.zone.Zone;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static mudmk3.adapter.security.SecurityFixture.admin;
import static mudmk3.adapter.security.SecurityFixture.clientHeader;
import static mudmk3.adapter.security.SecurityFixture.player;
import static mudmk3.adapter.webapi.ZoneApiWebMvcTest.room;
import static mudmk3.adapter.webapi.ZoneApiWebMvcTest.zone;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(RoomApi.class)
@Import(WebMvcSecurityTestConfiguration.class)
@MockitoBean(types = {RoomFinder.class, RoomEditor.class, WorldSettingsFinder.class})
record RoomApiWebMvcTest(MockMvcTester mvc, RoomFinder roomFinder, RoomEditor roomEditor,
                         WorldSettingsFinder worldSettingsFinder) {

    @Test
    void get() {
        Zone zone = zone(1L, "청석숲");
        Zone other = zone(2L, "황풍곡");
        Room room = room(zone, 10L, "샘터");
        room.setExit(Direction.UP, room(other, 12L, "바람 언덕"));
        room.setExit(Direction.NORTH, room(zone, 11L, "오솔길"));
        when(roomFinder.find(10L)).thenReturn(room);
        when(worldSettingsFinder.isStartRoom(10L)).thenReturn(true);

        var json = assertThat(mvc.get().with(admin()).uri("/api/admin/rooms/10")).hasStatusOk().bodyJson();
        json.isStrictlyEqualTo("""
                {"id":10,"zoneId":1,"zoneName":"청석숲","name":"샘터","description":"샘터의 묘사다.","start":true,
                 "exits":[
                   {"direction":"NORTH","directionName":"북","targetRoomId":11,"targetRoomName":"오솔길","targetZoneName":"청석숲"},
                   {"direction":"UP","directionName":"위","targetRoomId":12,"targetRoomName":"바람 언덕","targetZoneName":"황풍곡"}
                 ]}""");
        json.extractingPath("$.exits[0].directionName").isEqualTo("북");
    }

    @Test
    void create() {
        when(roomEditor.create(any())).thenReturn(room(zone(1L, "청석숲"), 10L, "샘터"));

        assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"zoneId":1,"name":"샘터","description":"샘터의 묘사다."}"""))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id":10,"zoneId":1,"zoneName":"청석숲","name":"샘터","start":false,"exits":[]}""");
        verify(roomEditor).create(new RoomSaveRequest(1L, "샘터", "샘터의 묘사다."));
    }

    @Test
    void createWithoutZoneFail() {
        assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"샘터","description":"샘터의 묘사다."}"""))
                .hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(roomEditor);
    }

    @Test
    void update() {
        when(roomEditor.update(eq(10L), any())).thenReturn(room(zone(2L, "황풍곡"), 10L, "바람 골짜기"));

        assertThat(mvc.put().with(admin()).with(clientHeader()).uri("/api/admin/rooms/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"zoneId":2,"name":"바람 골짜기","description":"바람이 거세다."}"""))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id":10,"zoneId":2,"zoneName":"황풍곡","name":"바람 골짜기"}""");
    }

    @Test
    void delete() {
        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/rooms/10")).hasStatus(HttpStatus.NO_CONTENT);
        verify(roomEditor).delete(10L);
    }

    @Test
    void deleteStartRoomFail() {
        doThrow(new StartRoomDeletionException("시작 방은 지울 수 없습니다")).when(roomEditor).delete(10L);

        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/rooms/10"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("시작 방은 지울 수 없습니다");
    }

    @Test
    void putExit() {
        Zone zone = zone(1L, "청석숲");
        Room room = room(zone, 10L, "샘터");
        room.setExit(Direction.NORTH, room(zone, 11L, "오솔길"));
        when(roomEditor.putExit(eq(10L), eq(Direction.NORTH), any())).thenReturn(room);

        assertThat(mvc.put().with(admin()).with(clientHeader()).uri("/api/admin/rooms/10/exits/NORTH")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"targetRoomId":11,"twoWay":true}"""))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.exits[0].targetRoomName").isEqualTo("오솔길");
        verify(roomEditor).putExit(10L, Direction.NORTH, new ExitSaveRequest(11L, true));
    }

    @Test
    void putExitBadDirectionFail() {
        assertThat(mvc.put().with(admin()).with(clientHeader()).uri("/api/admin/rooms/10/exits/NORTHH")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"targetRoomId":11,"twoWay":true}"""))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("값이 올바르지 않습니다: direction=NORTHH");
        verifyNoInteractions(roomEditor);
    }

    @Test
    void putExitConflictFail() {
        when(roomEditor.putExit(eq(10L), eq(Direction.NORTH), any()))
                .thenThrow(new ExitConflictException("반대 방향 출구가 이미 다른 방으로 이어져 있습니다: 오솔길 남"));

        assertThat(mvc.put().with(admin()).with(clientHeader()).uri("/api/admin/rooms/10/exits/NORTH")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"targetRoomId":11,"twoWay":true}"""))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("반대 방향 출구가 이미 다른 방으로 이어져 있습니다: 오솔길 남");
    }

    @Test
    void putExitToSelfFail() {
        when(roomEditor.putExit(eq(10L), eq(Direction.NORTH), any()))
                .thenThrow(new IllegalArgumentException("자기 자신으로 가는 출구는 만들 수 없습니다"));

        assertThat(mvc.put().with(admin()).with(clientHeader()).uri("/api/admin/rooms/10/exits/NORTH")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"targetRoomId":10,"twoWay":true}"""))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("자기 자신으로 가는 출구는 만들 수 없습니다");
    }

    @Test
    void deleteExit() {
        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/rooms/10/exits/SOUTH?twoWay=true")).hasStatus(HttpStatus.NO_CONTENT);
        verify(roomEditor).deleteExit(10L, Direction.SOUTH, true);
    }

    @Test
    void deleteExitOneWayByDefault() {
        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/rooms/10/exits/SOUTH")).hasStatus(HttpStatus.NO_CONTENT);
        verify(roomEditor).deleteExit(10L, Direction.SOUTH, false);
    }

    @Test
    void unauthenticatedFail() {
        assertThat(mvc.get().uri("/api/admin/rooms/10")).hasStatus(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(roomFinder);
    }

    @Test
    void putExitByPlayerFail() {
        assertThat(mvc.put().with(player()).with(clientHeader()).uri("/api/admin/rooms/10/exits/NORTH")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"targetRoomId":11,"twoWay":true}"""))
                .hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(roomEditor);
    }
}
