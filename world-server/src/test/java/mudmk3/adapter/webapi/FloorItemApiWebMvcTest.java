package mudmk3.adapter.webapi;

import mudmk3.adapter.security.WebMvcSecurityTestConfiguration;
import mudmk3.application.floor.provided.FloorPlacementEditor;
import mudmk3.application.floor.provided.FloorPlacementSaveRequest;
import mudmk3.domain.floor.FloorPlacement;
import mudmk3.domain.item.ItemTypeFixture;
import mudmk3.support.config.ClockConfig;
import org.jspecify.annotations.Nullable;
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
import static mudmk3.domain.floor.FloorFixture.createFloorPlacementSaveInfo;
import static mudmk3.domain.floor.FloorFixture.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(FloorItemApi.class)
@Import({WebMvcSecurityTestConfiguration.class, ClockConfig.class})
@MockitoBean(types = FloorPlacementEditor.class)
record FloorItemApiWebMvcTest(MockMvcTester mvc, FloorPlacementEditor floorPlacementEditor) {

    static FloorPlacement placement(long id, long roomId, int count, @Nullable Integer respawnMinutes) {
        return withId(FloorPlacement.create(roomId,
                withId(ItemTypeFixture.createWolfHide(), 5L), createFloorPlacementSaveInfo(count, respawnMinutes)), id);
    }

    @Test
    void create() {
        when(floorPlacementEditor.create(eq(10L), any())).thenReturn(placement(3L, 10L, 2, 10));

        assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/rooms/10/floor-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"itemTypeId":5,"count":2,"respawnMinutes":10}"""))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .isStrictlyEqualTo("""
                        {"id":3,"roomId":10,"itemTypeId":5,"itemName":"늑대 가죽","count":2,"respawnMinutes":10,"remaining":2}""");
        verify(floorPlacementEditor).create(10L, new FloorPlacementSaveRequest(5L, 2, 10));
    }

    @Test
    void createWithoutRespawn() {
        when(floorPlacementEditor.create(eq(10L), any())).thenReturn(placement(3L, 10L, 1, null));

        assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/rooms/10/floor-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"itemTypeId":5,"count":1,"respawnMinutes":null}"""))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$.respawnMinutes").isNull();
        verify(floorPlacementEditor).create(10L, new FloorPlacementSaveRequest(5L, 1, null));
    }

    @Test
    void createZeroCountFail() {
        assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/rooms/10/floor-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"itemTypeId":5,"count":0,"respawnMinutes":10}"""))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("입력 값이 올바르지 않습니다: count");
        verifyNoInteractions(floorPlacementEditor);
    }

    @Test
    void createUnknownRoomFail() {
        when(floorPlacementEditor.create(eq(999L), any())).thenThrow(new IllegalArgumentException("방을 찾을 수 없습니다. id: 999"));

        assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/rooms/999/floor-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"itemTypeId":5,"count":1}"""))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("방을 찾을 수 없습니다. id: 999");
    }

    @Test
    void update() {
        when(floorPlacementEditor.update(eq(3L), any())).thenReturn(placement(3L, 10L, 4, null));

        assertThat(mvc.put().with(admin()).with(clientHeader()).uri("/api/admin/floor-items/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"itemTypeId":5,"count":4}"""))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id":3,"count":4,"remaining":4,"respawnMinutes":null}""");
        verify(floorPlacementEditor).update(3L, new FloorPlacementSaveRequest(5L, 4, null));
    }

    @Test
    void delete() {
        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/floor-items/3")).hasStatus(HttpStatus.NO_CONTENT);
        verify(floorPlacementEditor).delete(3L);
    }

    @Test
    void deleteUnknownFail() {
        doThrow(new IllegalArgumentException("바닥 물건을 찾을 수 없습니다. id: 9"))
                .when(floorPlacementEditor).delete(9L);

        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/floor-items/9"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("바닥 물건을 찾을 수 없습니다. id: 9");
    }

    @Test
    void createByPlayerFail() {
        assertThat(mvc.post().with(player()).with(clientHeader()).uri("/api/admin/rooms/10/floor-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"itemTypeId":5,"count":1}"""))
                .hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(floorPlacementEditor);
    }
}
