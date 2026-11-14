package mudmk3.adapter.webapi;

import mudmk3.adapter.security.WebMvcSecurityTestConfiguration;
import mudmk3.application.room.provided.RoomFinder;
import mudmk3.application.zone.provided.ZoneEditor;
import mudmk3.application.zone.provided.ZoneFinder;
import mudmk3.application.zone.provided.ZoneNotEmptyException;
import mudmk3.application.zone.provided.ZoneSaveRequest;
import mudmk3.domain.room.Direction;
import mudmk3.domain.room.Room;
import mudmk3.domain.room.RoomFixture;
import mudmk3.domain.zone.DuplicateZoneNameException;
import mudmk3.domain.zone.Zone;
import mudmk3.domain.zone.ZoneFixture;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;

import static mudmk3.adapter.security.SecurityFixture.admin;
import static mudmk3.adapter.security.SecurityFixture.clientHeader;
import static mudmk3.adapter.security.SecurityFixture.player;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(ZoneApi.class)
@Import(WebMvcSecurityTestConfiguration.class)
@MockitoBean(types = {ZoneFinder.class, ZoneEditor.class, RoomFinder.class})
record ZoneApiWebMvcTest(MockMvcTester mvc, ZoneFinder zoneFinder, ZoneEditor zoneEditor, RoomFinder roomFinder) {

    @Test
    void list() {
        Zone a = zone(1L, "가람골");
        Zone b = zone(2L, "청석숲");
        when(zoneFinder.findAll()).thenReturn(List.of(a, b));
        when(zoneFinder.countRooms(1L)).thenReturn(0L);
        when(zoneFinder.countRooms(2L)).thenReturn(3L);

        assertThat(mvc.get().with(admin()).uri("/api/admin/zones"))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        [{"id":1,"name":"가람골","roomCount":0},{"id":2,"name":"청석숲","roomCount":3}]""");
    }

    @Test
    void create() {
        when(zoneEditor.create(any())).thenReturn(zone(1L, "청석숲"));
        when(zoneFinder.countRooms(1L)).thenReturn(0L);

        assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/zones")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"청석숲","description":"푸른 돌이 흩어진 숲이다."}"""))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .isStrictlyEqualTo("""
                        {"id":1,"name":"청석숲","description":"푸른 돌이 흩어진 숲이다.","roomCount":0}""");
        verify(zoneEditor).create(new ZoneSaveRequest("청석숲", "푸른 돌이 흩어진 숲이다.", null));
    }

    @Test
    void createBlankNameFail() {
        var result = assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/zones")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"  ","description":""}"""))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson();
        result.extractingPath("$.detail").isEqualTo("입력 값이 올바르지 않습니다: name");
        result.extractingPath("$.exception").isEqualTo("MethodArgumentNotValidException");
        result.extractingPath("$.timestamp").isNotNull();
        verify(zoneEditor, never()).create(any());
    }

    @Test
    void createDuplicateNameFail() {
        when(zoneEditor.create(any())).thenThrow(new DuplicateZoneNameException("이미 있는 지역 이름입니다: 청석숲"));

        var result = assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/zones")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"청석숲"}"""))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson();
        result.extractingPath("$.detail").isEqualTo("이미 있는 지역 이름입니다: 청석숲");
        result.extractingPath("$.status").isEqualTo(409);
        result.extractingPath("$.exception").isEqualTo("DuplicateZoneNameException");
        result.extractingPath("$.timestamp").isNotNull();
    }

    @Test
    void update() {
        when(zoneEditor.update(any(), any())).thenReturn(zone(1L, "황풍곡"));
        when(zoneFinder.countRooms(1L)).thenReturn(2L);

        assertThat(mvc.put().with(admin()).with(clientHeader()).uri("/api/admin/zones/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"황풍곡","description":"푸른 돌이 흩어진 숲이다."}"""))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id":1,"name":"황풍곡","roomCount":2}""");
    }

    @Test
    void delete() {
        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/zones/1")).hasStatus(HttpStatus.NO_CONTENT);
        verify(zoneEditor).delete(1L);
    }

    @Test
    void deleteNotEmptyFail() {
        doThrow(new ZoneNotEmptyException("방이 남아 있는 지역은 지울 수 없습니다")).when(zoneEditor).delete(1L);

        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/zones/1"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("방이 남아 있는 지역은 지울 수 없습니다");
    }

    @Test
    void missingZoneFail() {
        when(zoneFinder.find(9L)).thenThrow(new IllegalArgumentException("지역을 찾을 수 없습니다. id: 9"));
        when(roomFinder.findAllInZone(9L)).thenThrow(new IllegalArgumentException("지역을 찾을 수 없습니다. id: 9"));

        assertThat(mvc.get().with(admin()).uri("/api/admin/zones/9/rooms"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("지역을 찾을 수 없습니다. id: 9");
    }

    @Test
    void rooms() {
        Zone zone = zone(1L, "청석숲");
        Room a = room(zone, 10L, "샘터");
        Room b = room(zone, 11L, "오솔길");
        a.setExit(Direction.NORTH, b);
        when(roomFinder.findAllInZone(1L)).thenReturn(List.of(a, b));

        assertThat(mvc.get().with(admin()).uri("/api/admin/zones/1/rooms"))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("""
                        [{"id":10,"name":"샘터","exitCount":1},{"id":11,"name":"오솔길","exitCount":0}]""");
    }

    @Test
    void unexpectedErrorFail() {
        when(zoneFinder.findAll()).thenThrow(new IllegalStateException("내부 사정"));

        var result = assertThat(mvc.get().with(admin()).uri("/api/admin/zones"))
                .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .bodyJson();
        result.extractingPath("$.detail").isEqualTo("서버 오류가 발생했습니다");
        result.extractingPath("$.exception").isEqualTo("IllegalStateException");
    }

    @Test
    void unauthenticatedFail() {
        assertThat(mvc.get().uri("/api/admin/zones")).hasStatus(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(zoneFinder);
    }

    @Test
    void playerFail() {
        assertThat(mvc.get().with(player()).uri("/api/admin/zones")).hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(zoneFinder);
    }

    @Test
    void createWithoutClientHeaderFail() {
        assertThat(mvc.post().with(admin()).uri("/api/admin/zones")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"청석숲"}"""))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("허용되지 않은 요청입니다.");
        verifyNoInteractions(zoneEditor);
    }

    /** 컨트롤러 안에서 난 보안 예외는 500으로 바뀌지 않고 Spring Security가 403/401로 처리한다 */
    @Test
    void accessDeniedInsideControllerFail() {
        when(zoneFinder.findAll()).thenThrow(new AccessDeniedException("거절"));

        assertThat(mvc.get().with(admin()).uri("/api/admin/zones")).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void authenticationExceptionInsideControllerFail() {
        when(zoneFinder.findAll()).thenThrow(new InsufficientAuthenticationException("다시 로그인"));

        assertThat(mvc.get().with(admin()).uri("/api/admin/zones")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    static Zone zone(Long id, String name) {
        Zone zone = ZoneFixture.createZone(name);
        ReflectionTestUtils.setField(zone, "id", id);
        return zone;
    }

    static Room room(Zone zone, Long id, String name) {
        Room room = RoomFixture.createRoom(zone, name);
        ReflectionTestUtils.setField(room, "id", id);
        return room;
    }
}
