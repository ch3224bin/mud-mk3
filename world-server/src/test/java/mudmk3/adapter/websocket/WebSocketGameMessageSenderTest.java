package mudmk3.adapter.websocket;

import mudmk3.application.game.required.GameCloseReason;
import mudmk3.application.game.required.GameMessage;
import mudmk3.application.game.required.GameMessage.InventoryMessage;
import mudmk3.application.game.required.GameMessage.InventoryMessage.ItemView;
import mudmk3.application.game.required.GameMessage.InventoryMessage.StoneView;
import mudmk3.application.game.required.GameMessage.NoticeMessage;
import mudmk3.application.game.required.GameMessage.PromptMessage;
import mudmk3.application.game.required.GameMessage.RoomMessage;
import mudmk3.application.game.required.GameMessage.RoomMessage.FloorItemView;
import mudmk3.application.game.required.GameMessage.RoomMessage.OccupantView;
import mudmk3.application.game.required.GameMessage.RoomMessage.OwnerView;
import mudmk3.application.game.required.GameMessage.RoomMessage.RoomView;
import mudmk3.application.game.required.GameMessage.RoomMessage.ZoneView;
import mudmk3.application.game.required.GameMessage.SayMessage;
import mudmk3.application.game.required.GameMessage.SessionMessage;
import mudmk3.application.game.required.GameMessage.SystemMessage;
import mudmk3.application.game.required.GameMessage.TalkMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class WebSocketGameMessageSenderTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final WebSocketGameMessageSender sender = new WebSocketGameMessageSender(jsonMapper);

    @AfterEach
    void stopSender() {
        sender.destroy();
    }

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
                new RoomView(2L, "마을 어귀", "흙길이 갈라진다."), null, List.of("북", "남동"),
                List.of(OccupantView.player("한립"), OccupantView.npc("왕씨")), List.of());

        assertJson(room, """
                {"type":"room","zone":{"id":1,"name":"청우촌"},"zoneEntered":true,
                 "room":{"id":2,"name":"마을 어귀","description":"흙길이 갈라진다."},"owner":null,
                 "exits":["북","남동"],"occupants":[{"name":"한립","kind":"player"},{"name":"왕씨","kind":"npc"}],"items":[]}""");
        assertThat(sender.toJson(room)).contains("\"owner\":null");
    }

    @Test
    void roomWithOwnerJson() {
        RoomMessage room = new RoomMessage(new ZoneView(3L, "청석성"), false,
                new RoomView(4L, "남문", "돌로 쌓은 성문이 우뚝 서 있다."), new OwnerView("castle", "청석성"),
                List.of("남"), List.of(), List.of());

        assertJson(room, """
                {"type":"room","zone":{"id":3,"name":"청석성"},"zoneEntered":false,
                 "room":{"id":4,"name":"남문","description":"돌로 쌓은 성문이 우뚝 서 있다."},
                 "owner":{"kind":"castle","name":"청석성"},"exits":["남"],"occupants":[],"items":[]}""");
    }

    @Test
    void roomWithItemsJson() {
        RoomMessage room = new RoomMessage(new ZoneView(1L, "청우촌"), false,
                new RoomView(2L, "마을 어귀", "흙길이 갈라진다."), null, List.of(),
                List.of(OccupantView.npc("왕씨")), List.of(new FloorItemView("늑대 가죽", 3), new FloorItemView("비검", 1)));

        assertJson(room, """
                {"type":"room","zone":{"id":1,"name":"청우촌"},"zoneEntered":false,
                 "room":{"id":2,"name":"마을 어귀","description":"흙길이 갈라진다."},"owner":null,
                 "exits":[],"occupants":[{"name":"왕씨","kind":"npc"}],
                 "items":[{"name":"늑대 가죽","count":3},{"name":"비검","count":1}]}""");
        assertThat(propertyNames(room))
                .containsExactly("type", "zone", "zoneEntered", "room", "owner", "exits", "occupants", "items");
    }

    @Test
    void inventoryJson() {
        InventoryMessage inventory = InventoryMessage.of(3, 22, List.of(new StoneView("하품 영석", 30)),
                List.of(new ItemView("하급 저물대", 1, false, false), new ItemView("회춘단(연기 초)", 3, false, false),
                        new ItemView("비검", 1, true, false), new ItemView("왕씨의 약초 꾸러미", 1, false, true)),
                "[소지품 3/22칸]  하품 영석 30\n하급 저물대   회춘단(연기 초) x3   비검 (귀속)\n왕씨의 약초 꾸러미 (퀘스트)");

        assertJson(inventory, """
                {"type":"inventory","usedSlots":3,"totalSlots":22,"stones":[{"name":"하품 영석","count":30}],
                 "items":[{"name":"하급 저물대","count":1,"bound":false,"quest":false},
                          {"name":"회춘단(연기 초)","count":3,"bound":false,"quest":false},
                          {"name":"비검","count":1,"bound":true,"quest":false},
                          {"name":"왕씨의 약초 꾸러미","count":1,"bound":false,"quest":true}],
                 "text":"[소지품 3/22칸]  하품 영석 30\\n하급 저물대   회춘단(연기 초) x3   비검 (귀속)\\n왕씨의 약초 꾸러미 (퀘스트)",
                 "tone":"item"}""");
        assertThat(propertyNames(inventory))
                .containsExactly("type", "usedSlots", "totalSlots", "stones", "items", "text", "tone");
    }

    @Test
    void talkJson() {
        assertJson(TalkMessage.talk("왕씨", null, "왕씨: \"어서 오시오.\"\n물어볼 수 있는 낱말: [단약]"), """
                {"type":"talk","npc":"왕씨","word":null,
                 "text":"왕씨: \\"어서 오시오.\\"\\n물어볼 수 있는 낱말: [단약]","tone":"talk"}""");
        assertJson(TalkMessage.talk("왕씨", "단약", "왕씨: \"회춘단이 좋소.\""), """
                {"type":"talk","npc":"왕씨","word":"단약","text":"왕씨: \\"회춘단이 좋소.\\"","tone":"talk"}""");
    }

    @Test
    void systemJson() {
        assertJson(SystemMessage.info("접속한 사람 (1)\n  한백    청우촌"), """
                {"type":"system","text":"접속한 사람 (1)\\n  한백    청우촌","tone":"info"}""");
        assertJson(SystemMessage.error("그쪽으로는 길이 없습니다."), """
                {"type":"system","text":"그쪽으로는 길이 없습니다.","tone":"error"}""");
        assertJson(SystemMessage.item("늑대 가죽 2개를 주웠습니다."), """
                {"type":"system","text":"늑대 가죽 2개를 주웠습니다.","tone":"item"}""");
    }

    @Test
    void noticeJson() {
        assertJson(NoticeMessage.movement("한백이 북쪽으로 떠난다.", "한백", "leave"), """
                {"type":"notice","text":"한백이 북쪽으로 떠난다.","tone":"movement","actor":"한백","event":"leave"}""");
        assertJson(NoticeMessage.item("한백이 늑대 가죽 2개를 줍는다.", "한백", "pick"), """
                {"type":"notice","text":"한백이 늑대 가죽 2개를 줍는다.","tone":"item","actor":"한백","event":"pick"}""");
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
    void sendToRegisteredSession() {
        FakeSession session = new FakeSession("s1");
        sender.register(session.mock);

        sender.send("s1", SystemMessage.info("안내"));

        session.awaitEvents(1);
        assertThat(jsonMapper.readTree(session.events().getFirst()))
                .isEqualTo(jsonMapper.readTree("""
                        {"type":"system","text":"안내","tone":"info"}"""));
    }

    @Test
    void sendToUnknownSessionIgnored() throws InterruptedException {
        FakeSession session = new FakeSession("s1");
        sender.register(session.mock);
        sender.unregister("s1");

        sender.send("s1", SystemMessage.info("안내"));
        sender.send("nobody", SystemMessage.info("안내"));
        sender.close("nobody", GameCloseReason.QUIT);

        Thread.sleep(100);
        assertThat(session.events()).isEmpty();
    }

    @Test
    void messagesKeepOrder() {
        FakeSession session = new FakeSession("s1");
        sender.register(session.mock);

        for (int i = 0; i < 200; i++) {
            sender.send("s1", SystemMessage.info("줄 " + i));
        }

        session.awaitEvents(200);
        List<String> texts = session.events().stream()
                .map(json -> jsonMapper.readTree(json).get("text").asString())
                .toList();
        assertThat(texts).containsExactlyElementsOf(
                IntStream.range(0, 200).mapToObj(i -> "줄 " + i).toList());
    }

    @Test
    void closeAfterPendingMessages() {
        FakeSession session = new FakeSession("s1");
        session.block();
        sender.register(session.mock);

        sender.send("s1", SystemMessage.info("하나"));
        sender.send("s1", SystemMessage.info("둘"));
        sender.close("s1", GameCloseReason.QUIT);
        sender.send("s1", SystemMessage.info("닫은 뒤"));
        session.unblock();

        session.awaitEvents(3);
        assertThat(session.events()).hasSize(3);
        assertThat(session.events().get(0)).contains("하나");
        assertThat(session.events().get(1)).contains("둘");
        assertThat(session.events().get(2)).isEqualTo("close:1000");
    }

    @Test
    void sendDoesNotWaitForBlockedSession() {
        FakeSession stuck = new FakeSession("stuck");
        stuck.block();
        FakeSession other = new FakeSession("other");
        sender.register(stuck.mock);
        sender.register(other.mock);

        long started = System.nanoTime();
        for (int i = 0; i < 50; i++) {
            sender.send("stuck", SystemMessage.info("안내 " + i));
            sender.send("other", SystemMessage.info("안내 " + i));
        }
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

        assertThat(elapsedMillis).isLessThan(1000);
        other.awaitEvents(50);
        assertThat(stuck.events()).isEmpty();
        stuck.unblock();
    }

    @Test
    void pendingLimitExceededClosesSession() {
        FakeSession stuck = new FakeSession("stuck");
        stuck.block();
        sender.register(stuck.mock);
        String big = "가".repeat(60_000); // 약 180KB

        sender.send("stuck", SystemMessage.info("첫 메시지에서 막힌다"));
        stuck.awaitSendStarted();
        for (int i = 0; i < 4; i++) {
            sender.send("stuck", SystemMessage.info(big));
        }
        sender.send("stuck", SystemMessage.info("넘친 뒤"));
        stuck.unblock();

        stuck.awaitEvents(2);
        assertThat(stuck.events()).hasSize(2);
        assertThat(stuck.events().getFirst()).contains("첫 메시지에서 막힌다");
        assertThat(stuck.events().get(1)).isEqualTo("close:4500");
    }

    @Test
    void sendFailureClosesSession() throws IOException {
        FakeSession session = new FakeSession("s1");
        doThrow(new IOException("끊김")).when(session.mock).sendMessage(any());
        sender.register(session.mock);

        assertThatNoException().isThrownBy(() -> sender.send("s1", SystemMessage.info("안내")));

        session.awaitEvents(1);
        assertThat(session.events()).containsExactly("close:4500");
    }

    @Test
    void closeCodes() {
        assertCloseCode(GameCloseReason.QUIT, 1000);
        assertCloseCode(GameCloseReason.REPLACED, 4409);
        assertCloseCode(GameCloseReason.WORLD_NOT_READY, 1013);
    }

    private void assertCloseCode(GameCloseReason reason, int code) {
        FakeSession session = new FakeSession("close-" + reason);
        sender.register(session.mock);

        sender.close("close-" + reason, reason);

        session.awaitEvents(1);
        assertThat(session.events()).containsExactly("close:" + code);
    }

    private List<String> propertyNames(GameMessage message) {
        return jsonMapper.readTree(sender.toJson(message)).properties().stream().map(java.util.Map.Entry::getKey).toList();
    }

    private void assertJson(GameMessage message, String expected) {
        String json = sender.toJson(message);
        JsonNode actual = jsonMapper.readTree(json);
        assertThat(actual).isEqualTo(jsonMapper.readTree(expected));
        assertThat(json).startsWith("{\"type\":\"" + message.type() + "\",");
    }

    /** 보낸 메시지(payload)와 닫기(close:코드)를 차례로 기록하는 세션. block() 하면 보내기가 unblock() 까지 막힌다 */
    private static final class FakeSession {

        private final WebSocketSession mock = Mockito.mock(WebSocketSession.class);
        private final List<String> events = new CopyOnWriteArrayList<>();
        private final CountDownLatch sendStarted = new CountDownLatch(1);
        private volatile CountDownLatch gate = new CountDownLatch(0);

        FakeSession(String id) {
            when(mock.getId()).thenReturn(id);
            when(mock.isOpen()).thenReturn(true);
            try {
                doAnswer(invocation -> {
                    sendStarted.countDown();
                    gate.await();
                    events.add(invocation.<TextMessage>getArgument(0).getPayload());
                    return null;
                }).when(mock).sendMessage(any());
                doAnswer(invocation -> {
                    events.add("close:" + invocation.<CloseStatus>getArgument(0).getCode());
                    return null;
                }).when(mock).close(any());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        void block() {
            gate = new CountDownLatch(1);
        }

        void unblock() {
            gate.countDown();
        }

        List<String> events() {
            return events;
        }

        void awaitSendStarted() {
            try {
                assertThat(sendStarted.await(5, TimeUnit.SECONDS)).isTrue();
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }

        void awaitEvents(int count) {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (events.size() < count) {
                if (System.nanoTime() > deadline) {
                    throw new AssertionError(count + "개를 기다렸으나 " + events.size() + "개: " + events);
                }
                Thread.onSpinWait();
            }
        }
    }
}
