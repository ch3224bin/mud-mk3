package mudmk3.adapter.websocket;

import mudmk3.application.game.required.GameCloseReason;
import mudmk3.application.game.required.GameMessage;
import mudmk3.application.game.required.GameMessage.NoticeMessage;
import mudmk3.application.game.required.GameMessage.PromptMessage;
import mudmk3.application.game.required.GameMessage.RoomMessage;
import mudmk3.application.game.required.GameMessage.RoomMessage.OccupantView;
import mudmk3.application.game.required.GameMessage.RoomMessage.RoomView;
import mudmk3.application.game.required.GameMessage.RoomMessage.ZoneView;
import mudmk3.application.game.required.GameMessage.SayMessage;
import mudmk3.application.game.required.GameMessage.SessionMessage;
import mudmk3.application.game.required.GameMessage.SystemMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketGameMessageSenderTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final WebSocketGameMessageSender sender = new WebSocketGameMessageSender(jsonMapper);

    @Test
    void sessionJson() {
        assertJson(new SessionMessage(SessionMessage.CREATING, null), """
                {"type":"session","state":"creating","characterName":null}""");
        assertJson(new SessionMessage(SessionMessage.PLAYING, "한백"), """
                {"type":"session","state":"playing","characterName":"한백"}""");
    }

    @Test
    void promptJson() {
        assertJson(new PromptMessage("이름을 정하십시오. (한글 2~6자)"), """
                {"type":"prompt","text":"이름을 정하십시오. (한글 2~6자)"}""");
    }

    @Test
    void roomJson() {
        RoomMessage room = new RoomMessage(new ZoneView(1L, "청우촌"), true,
                new RoomView(2L, "마을 어귀", "흙길이 갈라진다."), List.of("북", "남동"),
                List.of(OccupantView.player("한립")));

        assertJson(room, """
                {"type":"room","zone":{"id":1,"name":"청우촌"},"zoneEntered":true,
                 "room":{"id":2,"name":"마을 어귀","description":"흙길이 갈라진다."},
                 "exits":["북","남동"],"occupants":[{"name":"한립","kind":"player"}]}""");
    }

    @Test
    void systemJson() {
        assertJson(SystemMessage.info("접속한 사람 (1)\n  한백    청우촌"), """
                {"type":"system","text":"접속한 사람 (1)\\n  한백    청우촌","tone":"info"}""");
        assertJson(SystemMessage.error("그쪽으로는 길이 없습니다."), """
                {"type":"system","text":"그쪽으로는 길이 없습니다.","tone":"error"}""");
    }

    @Test
    void noticeJson() {
        assertJson(NoticeMessage.movement("한백이 북쪽으로 떠난다.", "한백", "leave"), """
                {"type":"notice","text":"한백이 북쪽으로 떠난다.","tone":"movement","actor":"한백","event":"leave"}""");
    }

    @Test
    void sayJson() {
        assertJson(SayMessage.speech("한백", "길을 묻겠소.", false, "한백이 말한다: \"길을 묻겠소.\""), """
                {"type":"say","speaker":"한백","message":"길을 묻겠소.","self":false,
                 "text":"한백이 말한다: \\"길을 묻겠소.\\"","tone":"speech"}""");
    }

    @Test
    void typeComesFirst() {
        assertThat(sender.toJson(SystemMessage.info("안내"))).startsWith("{\"type\":\"system\",");
        assertThat(sender.toJson(new SessionMessage(SessionMessage.CREATING, null))).startsWith("{\"type\":\"session\",");
    }

    @Test
    void errorJson() {
        assertThat(jsonMapper.readTree(sender.errorJson("BAD_MESSAGE", "알 수 없는 메시지입니다.")))
                .isEqualTo(jsonMapper.readTree("""
                        {"type":"error","code":"BAD_MESSAGE","text":"알 수 없는 메시지입니다."}"""));
        assertThat(sender.errorJson("BAD_MESSAGE", "알 수 없는 메시지입니다.")).startsWith("{\"type\":\"error\",");
    }

    @Test
    void sendToRegisteredSession() throws IOException {
        WebSocketSession session = openSession("s1");
        sender.register(session);

        sender.send("s1", SystemMessage.info("안내"));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        assertThat(jsonMapper.readTree(captor.getValue().getPayload()))
                .isEqualTo(jsonMapper.readTree("""
                        {"type":"system","text":"안내","tone":"info"}"""));
    }

    @Test
    void sendToUnknownSessionIgnored() throws IOException {
        WebSocketSession session = openSession("s1");
        sender.register(session);
        sender.unregister("s1");

        sender.send("s1", SystemMessage.info("안내"));
        sender.send("nobody", SystemMessage.info("안내"));
        sender.close("nobody", GameCloseReason.QUIT);

        verify(session, never()).sendMessage(any());
    }

    @Test
    void sendFailureIsSwallowed() throws IOException {
        WebSocketSession session = openSession("s1");
        doThrow(new IOException("끊김")).when(session).sendMessage(any());
        sender.register(session);

        sender.send("s1", SystemMessage.info("안내"));
    }

    @Test
    void closeCodes() throws IOException {
        assertCloseCode(GameCloseReason.QUIT, 1000);
        assertCloseCode(GameCloseReason.REPLACED, 4409);
        assertCloseCode(GameCloseReason.WORLD_NOT_READY, 1013);
    }

    private void assertCloseCode(GameCloseReason reason, int code) throws IOException {
        WebSocketSession session = openSession("close-" + reason);
        sender.register(session);

        sender.close("close-" + reason, reason);

        ArgumentCaptor<CloseStatus> captor = ArgumentCaptor.forClass(CloseStatus.class);
        verify(session).close(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(code);
    }

    private static WebSocketSession openSession(String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    private void assertJson(GameMessage message, String expected) {
        String json = sender.toJson(message);
        JsonNode actual = jsonMapper.readTree(json);
        assertThat(actual).isEqualTo(jsonMapper.readTree(expected));
        assertThat(json).startsWith("{\"type\":\"" + message.type() + "\",");
    }
}
