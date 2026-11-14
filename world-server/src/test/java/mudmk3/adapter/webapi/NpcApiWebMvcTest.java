package mudmk3.adapter.webapi;

import mudmk3.adapter.security.WebMvcSecurityTestConfiguration;
import mudmk3.application.npc.provided.NpcEditor;
import mudmk3.application.npc.provided.NpcFinder;
import mudmk3.application.npc.provided.NpcSaveRequest;
import mudmk3.application.npc.provided.NpcTopicRequest;
import mudmk3.application.room.provided.RoomFinder;
import mudmk3.domain.npc.DuplicateNpcNameException;
import mudmk3.domain.npc.Npc;
import mudmk3.domain.npc.NpcSaveInfo;
import mudmk3.domain.npc.NpcTopic;
import mudmk3.domain.room.Room;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static mudmk3.adapter.security.SecurityFixture.admin;
import static mudmk3.adapter.security.SecurityFixture.clientHeader;
import static mudmk3.adapter.security.SecurityFixture.player;
import static mudmk3.adapter.webapi.ZoneApiWebMvcTest.room;
import static mudmk3.adapter.webapi.ZoneApiWebMvcTest.zone;
import static mudmk3.domain.npc.NpcFixture.createNpc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(NpcApi.class)
@Import(WebMvcSecurityTestConfiguration.class)
@MockitoBean(types = {NpcFinder.class, NpcEditor.class, RoomFinder.class})
record NpcApiWebMvcTest(MockMvcTester mvc, NpcFinder npcFinder, NpcEditor npcEditor, RoomFinder roomFinder) {

    @Test
    void create() {
        Room room = room(zone(1L, "청우촌"), 10L, "마을 어귀");
        Npc npc = npc(4L, new NpcSaveInfo("왕씨", "어서 오시오.", "글쎄, 잘 모르겠구려.",
                List.of(new NpcTopic("일거리", "숲의 늑대를 잡아 주시오."), new NpcTopic("단약", "단약은 귀한 물건이오.")), true));
        npc.placeIn(10L);
        when(npcEditor.create(any())).thenReturn(npc);
        when(roomFinder.findOptional(10L)).thenReturn(Optional.of(room));

        assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/npcs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"왕씨","greeting":"어서 오시오.","defaultAnswer":"글쎄, 잘 모르겠구려.",
                         "topics":[{"word":"[일거리]","answer":"숲의 늑대를 잡아 주시오."},{"word":"단약","answer":"단약은 귀한 물건이오."}],
                         "llmEnabled":true,"roomId":10}"""))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .isStrictlyEqualTo("""
                        {"id":4,"name":"왕씨","greeting":"어서 오시오.","defaultAnswer":"글쎄, 잘 모르겠구려.",
                         "topics":[{"word":"일거리","answer":"숲의 늑대를 잡아 주시오."},{"word":"단약","answer":"단약은 귀한 물건이오."}],
                         "llmEnabled":true,"roomId":10,"roomName":"마을 어귀","zoneId":1,"zoneName":"청우촌"}""");
        verify(npcEditor).create(new NpcSaveRequest("왕씨", "어서 오시오.", "글쎄, 잘 모르겠구려.",
                List.of(new NpcTopicRequest("[일거리]", "숲의 늑대를 잡아 주시오."), new NpcTopicRequest("단약", "단약은 귀한 물건이오.")),
                true, 10L));
    }

    @Test
    void createWithoutTopics() {
        when(npcEditor.create(any())).thenReturn(npc(4L, new NpcSaveInfo("왕씨", "어서 오시오.", "글쎄.", List.of(), false)));

        assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/npcs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"왕씨","greeting":"어서 오시오.","defaultAnswer":"글쎄.","llmEnabled":false}"""))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id":4,"topics":[],"llmEnabled":false,"roomId":null,"roomName":null,"zoneId":null,"zoneName":null}""");
        verify(npcEditor).create(new NpcSaveRequest("왕씨", "어서 오시오.", "글쎄.", null, false, null));
    }

    @Test
    void createNameWithSpaceFail() {
        assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/npcs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"왕 씨","greeting":"어서 오시오.","defaultAnswer":"글쎄.","llmEnabled":false}"""))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("입력 값이 올바르지 않습니다: name");
        verify(npcEditor, never()).create(any());
    }

    @Test
    void createTooManyTopicsFail() {
        String topics = IntStream.rangeClosed(1, 31)
                .mapToObj(i -> """
                        {"word":"낱말%d","answer":"대답"}""".formatted(i))
                .collect(Collectors.joining(","));

        assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/npcs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"왕씨","greeting":"어서 오시오.","defaultAnswer":"글쎄.","topics":[%s],"llmEnabled":false}""".formatted(topics)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("입력 값이 올바르지 않습니다: topics");
        verify(npcEditor, never()).create(any());
    }

    @Test
    void createBlankGreetingAndTopicFail() {
        assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/npcs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"왕씨","greeting":"  ","defaultAnswer":"%s","topics":[{"word":"단약","answer":" "}],"llmEnabled":false}"""
                        .formatted("가".repeat(501))))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("입력 값이 올바르지 않습니다: defaultAnswer, greeting, topics[0].answer");
        verify(npcEditor, never()).create(any());
    }

    @Test
    void createDuplicateNameFail() {
        when(npcEditor.create(any())).thenThrow(new DuplicateNpcNameException("이미 있는 NPC 이름입니다: 왕씨"));

        var result = assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/npcs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"왕씨","greeting":"어서 오시오.","defaultAnswer":"글쎄.","llmEnabled":false}"""))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson();
        result.extractingPath("$.detail").isEqualTo("이미 있는 NPC 이름입니다: 왕씨");
        result.extractingPath("$.exception").isEqualTo("DuplicateNpcNameException");
    }

    @Test
    void createDuplicateTopicFail() {
        when(npcEditor.create(any())).thenThrow(new IllegalArgumentException("같은 낱말이 두 번 있습니다: 단약"));

        assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/npcs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"왕씨","greeting":"어서 오시오.","defaultAnswer":"글쎄.","llmEnabled":false}"""))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("같은 낱말이 두 번 있습니다: 단약");
    }

    @Test
    void list() {
        Npc placed = npc(4L, "왕씨");
        placed.placeIn(10L);
        Npc unplaced = npc(5L, "이씨");
        when(npcFinder.findAll()).thenReturn(List.of(placed, unplaced));
        when(roomFinder.findOptional(10L)).thenReturn(Optional.of(room(zone(1L, "청우촌"), 10L, "마을 어귀")));

        assertThat(mvc.get().with(admin()).uri("/api/admin/npcs"))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("""
                        [{"id":4,"name":"왕씨","llmEnabled":false,"topicCount":2,"roomId":10,"roomName":"마을 어귀","zoneName":"청우촌"},
                         {"id":5,"name":"이씨","llmEnabled":false,"topicCount":2,"roomId":null,"roomName":null,"zoneName":null}]""");
    }

    @Test
    void get() {
        when(npcFinder.find(4L)).thenReturn(npc(4L, "왕씨"));

        assertThat(mvc.get().with(admin()).uri("/api/admin/npcs/4"))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id":4,"name":"왕씨","topics":[{"word":"단약"},{"word":"일거리"}],"roomId":null}""");
    }

    @Test
    void getUnknownFail() {
        when(npcFinder.find(9L)).thenThrow(new IllegalArgumentException("NPC를 찾을 수 없습니다. id: 9"));

        assertThat(mvc.get().with(admin()).uri("/api/admin/npcs/9"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("NPC를 찾을 수 없습니다. id: 9");
    }

    @Test
    void update() {
        when(npcEditor.update(any(), any())).thenReturn(npc(4L, "이씨"));

        assertThat(mvc.put().with(admin()).with(clientHeader()).uri("/api/admin/npcs/4")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"이씨","greeting":"어서 오시오.","defaultAnswer":"글쎄.","topics":[],"llmEnabled":false}"""))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id":4,"name":"이씨"}""");
        verify(npcEditor).update(4L, new NpcSaveRequest("이씨", "어서 오시오.", "글쎄.", List.of(), false, null));
    }

    @Test
    void delete() {
        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/npcs/4"))
                .hasStatus(HttpStatus.NO_CONTENT);
        verify(npcEditor).delete(4L);
    }

    @Test
    void playerFail() {
        assertThat(mvc.get().with(player()).uri("/api/admin/npcs")).hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(npcFinder);
    }

    @Test
    void createWithoutClientHeaderFail() {
        assertThat(mvc.post().with(admin()).uri("/api/admin/npcs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"왕씨","greeting":"어서 오시오.","defaultAnswer":"글쎄.","llmEnabled":false}"""))
                .hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(npcEditor);
    }

    static Npc npc(Long id, String name) {
        Npc npc = createNpc(name);
        ReflectionTestUtils.setField(npc, "id", id);
        return npc;
    }

    static Npc npc(Long id, NpcSaveInfo info) {
        Npc npc = Npc.create(info);
        ReflectionTestUtils.setField(npc, "id", id);
        return npc;
    }
}
