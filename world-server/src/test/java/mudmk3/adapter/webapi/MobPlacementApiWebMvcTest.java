package mudmk3.adapter.webapi;

import mudmk3.adapter.security.WebMvcSecurityTestConfiguration;
import mudmk3.application.mob.provided.MobPlacementEditor;
import mudmk3.application.mob.provided.MobPlacementSaveRequest;
import mudmk3.application.mob.provided.MobPlacementView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static mudmk3.adapter.security.SecurityFixture.admin;
import static mudmk3.adapter.security.SecurityFixture.clientHeader;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(MobPlacementApi.class)
@Import(WebMvcSecurityTestConfiguration.class)
@MockitoBean(types = MobPlacementEditor.class)
record MobPlacementApiWebMvcTest(MockMvcTester mvc, MobPlacementEditor mobPlacementEditor) {

    @Test
    void create() {
        when(mobPlacementEditor.create(eq(10L), any())).thenReturn(new MobPlacementView(3L, 10L, 1L, "청모랑", 2));

        assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/rooms/10/mobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"mobId":1,"count":2}"""))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .isStrictlyEqualTo("""
                        {"id":3,"roomId":10,"mobId":1,"mobName":"청모랑","count":2}""");
        verify(mobPlacementEditor).create(10L, new MobPlacementSaveRequest(1L, 2));
    }

    @Test
    void createWithoutMobIdFail() {
        assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/rooms/10/mobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"count":2}"""))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("요청 본문을 읽을 수 없습니다");
        verifyNoInteractions(mobPlacementEditor);
    }

    @Test
    void createCountRangeFail() {
        when(mobPlacementEditor.create(eq(10L), any())).thenThrow(new IllegalArgumentException("몹 수는 1~10으로 정하십시오."));

        assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/rooms/10/mobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"mobId":1,"count":11}"""))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("몹 수는 1~10으로 정하십시오.");
        verify(mobPlacementEditor).create(10L, new MobPlacementSaveRequest(1L, 11));
    }

    @Test
    void update() {
        when(mobPlacementEditor.update(eq(3L), any())).thenReturn(new MobPlacementView(3L, 10L, 2L, "들토끼", 5));

        assertThat(mvc.put().with(admin()).with(clientHeader()).uri("/api/admin/mob-placements/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"mobId":2,"count":5}"""))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("""
                        {"id":3,"roomId":10,"mobId":2,"mobName":"들토끼","count":5}""");
        verify(mobPlacementEditor).update(3L, new MobPlacementSaveRequest(2L, 5));
    }

    @Test
    void delete() {
        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/mob-placements/3"))
                .hasStatus(HttpStatus.NO_CONTENT);
        verify(mobPlacementEditor).delete(3L);
    }

    @Test
    void deleteUnknownFail() {
        doThrow(new IllegalArgumentException("몹 배치를 찾을 수 없습니다. id: 9")).when(mobPlacementEditor).delete(9L);

        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/mob-placements/9"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("몹 배치를 찾을 수 없습니다. id: 9");
    }

    @Test
    void createWithoutClientHeaderFail() {
        assertThat(mvc.post().with(admin()).uri("/api/admin/rooms/10/mobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"mobId":1,"count":2}"""))
                .hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(mobPlacementEditor);
    }
}
