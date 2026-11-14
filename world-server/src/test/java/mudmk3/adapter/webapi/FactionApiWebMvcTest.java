package mudmk3.adapter.webapi;

import mudmk3.adapter.security.WebMvcSecurityTestConfiguration;
import mudmk3.application.faction.provided.FactionEditor;
import mudmk3.application.faction.provided.FactionFinder;
import mudmk3.application.faction.provided.FactionInUseException;
import mudmk3.application.faction.provided.FactionSaveRequest;
import mudmk3.domain.faction.DuplicateFactionNameException;
import mudmk3.domain.faction.Faction;
import mudmk3.domain.faction.FactionFixture;
import mudmk3.domain.faction.FactionKind;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

@WebMvcTest(FactionApi.class)
@Import(WebMvcSecurityTestConfiguration.class)
@MockitoBean(types = {FactionFinder.class, FactionEditor.class})
record FactionApiWebMvcTest(MockMvcTester mvc, FactionFinder factionFinder, FactionEditor factionEditor) {

    @Test
    void list() {
        Faction a = faction(2L, FactionKind.CASTLE, "청석성");
        Faction b = faction(1L, FactionKind.SECT, "황엽곡");
        when(factionFinder.findAll()).thenReturn(List.of(a, b));
        when(factionFinder.countZones(2L)).thenReturn(2L);
        when(factionFinder.countZones(1L)).thenReturn(0L);

        assertThat(mvc.get().with(admin()).uri("/api/admin/factions"))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        [{"id":2,"kind":"CASTLE","kindName":"성","name":"청석성","zoneCount":2},
                         {"id":1,"kind":"SECT","kindName":"종문","name":"황엽곡","zoneCount":0}]""");
    }

    @Test
    void create() {
        when(factionEditor.create(any())).thenReturn(faction(1L, FactionKind.CASTLE, "청석성"));
        when(factionFinder.countZones(1L)).thenReturn(0L);

        assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/factions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"kind":"CASTLE","name":"청석성","description":"청석성의 묘사다."}"""))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .isStrictlyEqualTo("""
                        {"id":1,"kind":"CASTLE","kindName":"성","name":"청석성","description":"청석성의 묘사다.","zoneCount":0}""");
        verify(factionEditor).create(new FactionSaveRequest(FactionKind.CASTLE, "청석성", "청석성의 묘사다."));
    }

    @Test
    void createBlankNameFail() {
        assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/factions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"kind":"CASTLE","name":"  "}"""))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("입력 값이 올바르지 않습니다: name");
        verify(factionEditor, never()).create(any());
    }

    @Test
    void createUnknownKindFail() {
        assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/factions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"kind":"CITY","name":"청석성"}"""))
                .hasStatus(HttpStatus.BAD_REQUEST);
        verify(factionEditor, never()).create(any());
    }

    @Test
    void createDuplicateNameFail() {
        when(factionEditor.create(any())).thenThrow(new DuplicateFactionNameException("이미 있는 성·종문 이름입니다: 청석성"));

        var result = assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/factions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"kind":"SECT","name":"청석성"}"""))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson();
        result.extractingPath("$.detail").isEqualTo("이미 있는 성·종문 이름입니다: 청석성");
        result.extractingPath("$.exception").isEqualTo("DuplicateFactionNameException");
    }

    @Test
    void update() {
        when(factionEditor.update(any(), any())).thenReturn(faction(1L, FactionKind.SECT, "황엽곡"));
        when(factionFinder.countZones(1L)).thenReturn(3L);

        assertThat(mvc.put().with(admin()).with(clientHeader()).uri("/api/admin/factions/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"kind":"SECT","name":"황엽곡","description":"황엽곡의 묘사다."}"""))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id":1,"kind":"SECT","kindName":"종문","name":"황엽곡","zoneCount":3}""");
        verify(factionEditor).update(1L, new FactionSaveRequest(FactionKind.SECT, "황엽곡", "황엽곡의 묘사다."));
    }

    @Test
    void delete() {
        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/factions/1"))
                .hasStatus(HttpStatus.NO_CONTENT);
        verify(factionEditor).delete(1L);
    }

    @Test
    void deleteOwningFactionFail() {
        doThrow(new FactionInUseException("소유한 지역이 있는 성·종문은 지울 수 없습니다")).when(factionEditor).delete(1L);

        var result = assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/factions/1"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson();
        result.extractingPath("$.detail").isEqualTo("소유한 지역이 있는 성·종문은 지울 수 없습니다");
        result.extractingPath("$.exception").isEqualTo("FactionInUseException");
    }

    @Test
    void playerFail() {
        assertThat(mvc.get().with(player()).uri("/api/admin/factions")).hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(factionFinder);
    }

    @Test
    void createWithoutClientHeaderFail() {
        assertThat(mvc.post().with(admin()).uri("/api/admin/factions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"kind":"CASTLE","name":"청석성"}"""))
                .hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(factionEditor);
    }

    static Faction faction(Long id, FactionKind kind, String name) {
        Faction faction = FactionFixture.createFaction(kind, name);
        ReflectionTestUtils.setField(faction, "id", id);
        return faction;
    }
}
