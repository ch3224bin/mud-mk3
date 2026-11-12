package mudmk3.adapter.websocket;

import mudmk3.application.character.provided.CharacterCreator;
import mudmk3.application.game.provided.GameConnector;
import mudmk3.application.game.required.PresenceRepository;
import mudmk3.application.room.provided.ExitSaveRequest;
import mudmk3.application.room.provided.RoomEditor;
import mudmk3.application.world.provided.WorldSettingsEditor;
import mudmk3.application.zone.provided.ZoneEditor;
import mudmk3.domain.room.Direction;
import mudmk3.domain.room.Room;
import mudmk3.domain.zone.Zone;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static mudmk3.domain.room.RoomFixture.createRoomSaveRequest;
import static mudmk3.domain.zone.ZoneFixture.createZoneSaveRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 실제 서버(무작위 포트)에 개발용 로그인 세션 쿠키로 WebSocket 접속한다.
 * 포트 호출이 각자 커밋하므로 따로 쓰는 DB를 두고, 테스트가 끝나면 연결을 모두 닫고 표를 비운다.
 * 맵: 지역 청우촌, 마을 어귀(시작) 북↔남 우물가.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "local"})
@TestPropertySource(properties = {
        "mud.auth.dev-login.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:game-websocket-it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
class GameWebSocketIntegrationTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:3000";
    private static final long TIMEOUT_SECONDS = 10;

    private final int port;
    private final ZoneEditor zoneEditor;
    private final RoomEditor roomEditor;
    private final WorldSettingsEditor worldSettingsEditor;
    private final CharacterCreator characterCreator;
    private final PresenceRepository presenceRepository;
    private final GameConnector gameConnector;
    private final WebSocketGameMessageSender sender;
    private final JdbcTemplate jdbcTemplate;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final List<Client> clients = new ArrayList<>();

    GameWebSocketIntegrationTest(
            @LocalServerPort int port,
            ZoneEditor zoneEditor,
            RoomEditor roomEditor,
            WorldSettingsEditor worldSettingsEditor,
            CharacterCreator characterCreator,
            PresenceRepository presenceRepository,
            GameConnector gameConnector,
            WebSocketGameMessageSender sender,
            JdbcTemplate jdbcTemplate
    ) {
        this.port = port;
        this.zoneEditor = zoneEditor;
        this.roomEditor = roomEditor;
        this.worldSettingsEditor = worldSettingsEditor;
        this.characterCreator = characterCreator;
        this.presenceRepository = presenceRepository;
        this.gameConnector = gameConnector;
        this.sender = sender;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void prepareMap() {
        Zone village = zoneEditor.create(createZoneSaveRequest("청우촌"));
        Room entrance = roomEditor.create(createRoomSaveRequest(village.getId(), "마을 어귀"));
        Room well = roomEditor.create(createRoomSaveRequest(village.getId(), "우물가"));
        roomEditor.putExit(entrance.getId(), Direction.NORTH, new ExitSaveRequest(well.getId(), true));
        worldSettingsEditor.designateStartRoom(entrance.getId());
    }

    @AfterEach
    void cleanUp() throws InterruptedException {
        for (Client client : clients) {
            if (client.session != null && client.session.isOpen()) {
                try {
                    client.session.close();
                } catch (Exception ignored) {
                    // 이미 닫히는 중
                }
            }
        }
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        while (!presenceRepository.findAll().isEmpty()) {
            if (System.nanoTime() > deadline) fail("접속한 사람이 남았다: " + presenceRepository.findAll());
            Thread.sleep(20);
        }
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class)
                .forEach(table -> jdbcTemplate.execute("TRUNCATE TABLE " + table + " RESTART IDENTITY"));
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    @Test
    void authAndCreateCharacter() throws Exception {
        Client client = connect(login("hanbai@example.com").cookie(), ALLOWED_ORIGIN);

        assertThat(client.next()).isEqualTo(json("""
                {"type":"session","state":"creating","characterName":null}"""));
        assertThat(client.next()).isEqualTo(json("""
                {"type":"prompt","text":"이름을 정하십시오. (한글 2~6자)"}"""));

        client.command("한백");
        assertThat(client.next().get("type").asString()).isEqualTo("prompt");
        client.command("확인");

        assertThat(client.next()).isEqualTo(json("""
                {"type":"system","text":"캐릭터를 만들었습니다.","tone":"info"}"""));
        assertThat(client.next()).isEqualTo(json("""
                {"type":"session","state":"playing","characterName":"한백"}"""));
        JsonNode room = client.next();
        assertThat(room.get("type").asString()).isEqualTo("room");
        assertThat(room.get("zoneEntered").asBoolean()).isTrue();
        assertThat(room.get("zone").get("name").asString()).isEqualTo("청우촌");
        assertThat(room.get("room").get("name").asString()).isEqualTo("마을 어귀");
        assertThat(room.get("exits")).isEqualTo(json("[\"북\"]"));
        assertThat(room.get("occupants")).isEqualTo(json("[]"));
    }

    @Test
    void noSessionClosesWith4401() throws Exception {
        Client client = connect(null, ALLOWED_ORIGIN);

        assertThat(client.next()).isEqualTo(json("""
                {"type":"error","code":"AUTH_REQUIRED","text":"로그인이 필요합니다. 다시 로그인하십시오."}"""));
        assertThat(client.closeStatus().getCode()).isEqualTo(4401);
    }

    @Test
    void foreignOriginRejected() throws Exception {
        String cookie = login("hanbai@example.com").cookie();

        assertThat(handshakeStatus("http://evil.example")).isEqualTo(403);
        assertThat(handshakeStatus(ALLOWED_ORIGIN)).isEqualTo(101);
        assertThatThrownBy(() -> connect(cookie, "http://evil.example"))
                .isInstanceOf(ExecutionException.class);
    }

    @Test
    void malformedJsonIsBadMessage() throws Exception {
        Client client = enter("hanbai@example.com", "한백");
        JsonNode badMessage = json("""
                {"type":"error","code":"BAD_MESSAGE","text":"알 수 없는 메시지입니다."}""");

        client.send("명령이 아님");
        assertThat(client.next()).isEqualTo(badMessage);
        client.send("{\"type\":\"chat\",\"text\":\"보기\"}");
        assertThat(client.next()).isEqualTo(badMessage);
        client.send("{\"type\":\"command\"}");
        assertThat(client.next()).isEqualTo(badMessage);
        client.session().sendMessage(new BinaryMessage(new byte[]{1, 2, 3}));
        assertThat(client.next()).isEqualTo(badMessage);

        client.command("보기");
        assertThat(client.next().get("type").asString()).isEqualTo("room");
        assertThat(client.session().isOpen()).isTrue();
    }

    @Test
    void twoPlayersSeeEachOther() throws Exception {
        Client a = enter("hanbai@example.com", "한백");
        Client b = enter("hanli@example.com", "한립");
        assertThat(b.lastRoom().get("occupants")).isEqualTo(json("""
                [{"name":"한백","kind":"player"}]"""));
        assertThat(a.next()).isEqualTo(json("""
                {"type":"notice","text":"한립이 나타난다.","tone":"movement","actor":"한립","event":"appear"}"""));

        a.command("북");
        assertThat(a.next().get("room").get("name").asString()).isEqualTo("우물가");
        assertThat(b.next()).isEqualTo(json("""
                {"type":"notice","text":"한백이 북쪽으로 떠난다.","tone":"movement","actor":"한백","event":"leave"}"""));

        a.command("'안녕");
        assertThat(a.next()).isEqualTo(json("""
                {"type":"say","speaker":"한백","message":"안녕","self":true,"text":"한백: \\"안녕\\"","tone":"speech"}"""));

        b.command("북");
        // 다른 방이던 B에게 A의 말이 갔다면 방 보기보다 먼저 왔을 것이다
        JsonNode bRoom = b.next();
        assertThat(bRoom.get("type").asString()).isEqualTo("room");
        assertThat(bRoom.get("occupants")).isEqualTo(json("""
                [{"name":"한백","kind":"player"}]"""));
        assertThat(a.next()).isEqualTo(json("""
                {"type":"notice","text":"한립이 남쪽에서 다가온다.","tone":"movement","actor":"한립","event":"arrive"}"""));

        b.command("'안녕하시오");
        assertThat(b.next().get("self").asBoolean()).isTrue();
        assertThat(a.next()).isEqualTo(json("""
                {"type":"say","speaker":"한립","message":"안녕하시오","self":false,
                 "text":"한립이 말한다: \\"안녕하시오\\"","tone":"speech"}"""));
    }

    @Test
    void secondConnectionClosesFirstWith4409() throws Exception {
        Login login = login("hanbai@example.com");
        characterCreator.create(login.accountId(), "한백");
        Client first = connect(login.cookie(), ALLOWED_ORIGIN);
        first.awaitRoom();

        Client second = connect(login.cookie(), ALLOWED_ORIGIN);

        assertThat(first.next()).isEqualTo(json("""
                {"type":"system","text":"다른 곳에서 접속해 이 연결을 끊습니다.","tone":"info"}"""));
        assertThat(first.closeStatus().getCode()).isEqualTo(4409);
        assertThat(second.next()).isEqualTo(json("""
                {"type":"session","state":"playing","characterName":"한백"}"""));
        assertThat(second.next().get("type").asString()).isEqualTo("room");
        assertThat(presenceRepository.findAll()).hasSize(1);
    }

    @Test
    void abruptCloseRemovesPresence() throws Exception {
        Client a = enter("hanbai@example.com", "한백");
        Client b = enter("hanli@example.com", "한립");
        assertThat(a.next().get("event").asString()).isEqualTo("appear");

        a.session().close();

        assertThat(b.next()).isEqualTo(json("""
                {"type":"notice","text":"한백이 사라진다.","tone":"movement","actor":"한백","event":"disappear"}"""));
        b.command("누구");
        assertThat(b.next().get("text").asString()).startsWith("접속한 사람 (1)");
    }

    @Test
    void stuckClientDoesNotDelayOthers() throws Exception {
        // 받아 가지 않는 클라이언트: 보내기가 풀어 줄 때까지 막힌다
        CountDownLatch release = new CountDownLatch(1);
        WebSocketSession stuck = mock(WebSocketSession.class);
        when(stuck.getId()).thenReturn("stuck-session");
        when(stuck.isOpen()).thenReturn(true);
        doAnswer(invocation -> {
            release.await();
            return null;
        }).when(stuck).sendMessage(any());
        sender.register(stuck);
        Long stuckAccountId = login("wangsun@example.com").accountId();
        characterCreator.create(stuckAccountId, "왕선");
        gameConnector.connect("stuck-session", stuckAccountId);
        try {
            Client client = enter("hanbai@example.com", "한백");
            assertThat(client.lastRoom().get("occupants")).isEqualTo(json("""
                    [{"name":"왕선","kind":"player"}]"""));

            long started = System.nanoTime();
            for (int i = 0; i < 20; i++) {
                client.command("'안녕 " + i);
                JsonNode said = client.next(1);
                assertThat(said.get("message").asString()).isEqualTo("안녕 " + i);
            }
            assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)).isLessThan(5000);
        } finally {
            release.countDown();
            gameConnector.disconnect("stuck-session");
            sender.unregister("stuck-session");
        }
    }

    @Test
    void quitClosesWith1000() throws Exception {
        Client client = enter("hanbai@example.com", "한백");

        client.command("종료");

        assertThat(client.next()).isEqualTo(json("""
                {"type":"system","text":"다음에 또 뵙겠습니다.","tone":"info"}"""));
        assertThat(client.closeStatus().getCode()).isEqualTo(1000);
    }

    // ---- 준비

    private record Login(String cookie, Long accountId) {
    }

    private Login login(String email) {
        ResponseEntity<String> response = RestClient.create().post()
                .uri("http://localhost:" + port + "/api/auth/dev")
                .header("X-MUD-Client", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"email\":\"" + email + "\"}")
                .retrieve()
                .toEntity(String.class);
        String cookie = response.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE).stream()
                .filter(header -> header.startsWith("JSESSIONID="))
                .map(header -> header.split(";", 2)[0])
                .findFirst()
                .orElseThrow(() -> new AssertionError("세션 쿠키가 없다: " + response.getHeaders()));
        long accountId = jsonMapper.readTree(response.getBody()).get("id").asLong();
        return new Login(cookie, accountId);
    }

    /** 캐릭터를 만든 계정으로 접속해 방 보기까지 받는다 */
    private Client enter(String email, String name) throws Exception {
        Login login = login(email);
        characterCreator.create(login.accountId(), name);
        Client client = connect(login.cookie(), ALLOWED_ORIGIN);
        client.awaitRoom();
        return client;
    }

    private Client connect(@Nullable String cookie, String origin) throws Exception {
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.setOrigin(origin);
        if (cookie != null) {
            headers.add(HttpHeaders.COOKIE, cookie);
        }
        Client client = new Client();
        client.session = new StandardWebSocketClient()
                .execute(client, headers, URI.create("ws://localhost:" + port + "/ws/game"))
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        clients.add(client);
        return client;
    }

    /** 쿠키 없이 핸드셰이크 요청만 보내 응답 상태 코드를 본다 */
    private int handshakeStatus(String origin) throws IOException {
        try (Socket socket = new Socket("localhost", port)) {
            socket.setSoTimeout((int) TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
            String request = "GET /ws/game HTTP/1.1\r\n"
                    + "Host: localhost:" + port + "\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n"
                    + "Sec-WebSocket-Version: 13\r\n"
                    + "Origin: " + origin + "\r\n\r\n";
            OutputStream out = socket.getOutputStream();
            out.write(request.getBytes(StandardCharsets.US_ASCII));
            out.flush();
            String statusLine = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII))
                    .readLine();
            if (statusLine == null) throw new AssertionError("핸드셰이크 응답이 없다");
            return Integer.parseInt(statusLine.split(" ")[1]);
        }
    }

    private JsonNode json(String text) {
        return jsonMapper.readTree(text);
    }

    /** 받은 메시지를 줄 세워 두는 클라이언트 */
    private final class Client extends TextWebSocketHandler {

        private final BlockingQueue<String> received = new LinkedBlockingQueue<>();
        private final CompletableFuture<CloseStatus> closed = new CompletableFuture<>();
        private @Nullable WebSocketSession session;
        private @Nullable JsonNode lastRoom;

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            received.add(message.getPayload());
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            closed.complete(status);
        }

        WebSocketSession session() {
            if (session == null) throw new IllegalStateException("연결되지 않았다");
            return session;
        }

        JsonNode next() throws InterruptedException {
            return next(TIMEOUT_SECONDS);
        }

        JsonNode next(long timeoutSeconds) throws InterruptedException {
            String payload = received.poll(timeoutSeconds, TimeUnit.SECONDS);
            if (payload == null) throw new AssertionError("메시지가 오지 않았다");
            return jsonMapper.readTree(payload);
        }

        /** 접속 뒤 session(playing) 과 room 을 받는다 */
        void awaitRoom() throws InterruptedException {
            assertThat(next().get("state").asString()).isEqualTo("playing");
            JsonNode room = next();
            assertThat(room.get("type").asString()).isEqualTo("room");
            lastRoom = room;
        }

        JsonNode lastRoom() {
            if (lastRoom == null) throw new IllegalStateException("방 보기를 받지 않았다");
            return lastRoom;
        }

        void command(String text) throws Exception {
            send(jsonMapper.writeValueAsString(new InboundMessage("command", text)));
        }

        void send(String payload) throws Exception {
            session().sendMessage(new TextMessage(payload));
        }

        CloseStatus closeStatus() throws Exception {
            return closed.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }
}
