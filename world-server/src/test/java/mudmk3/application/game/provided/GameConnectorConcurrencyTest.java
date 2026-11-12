package mudmk3.application.game.provided;

import mudmk3.TestFakesConfiguration;
import mudmk3.application.account.provided.AccountLogin;
import mudmk3.application.account.provided.AccountLoginRequest;
import mudmk3.application.character.provided.CharacterCreator;
import mudmk3.application.character.provided.CharacterFinder;
import mudmk3.application.character.provided.CharacterMover;
import mudmk3.application.game.RecordingGameMessageSender;
import mudmk3.application.game.required.GameMessage.NoticeMessage;
import mudmk3.application.game.required.GameMessage.RoomMessage;
import mudmk3.application.game.required.GameMessage.SystemMessage;
import mudmk3.application.room.provided.ExitSaveRequest;
import mudmk3.application.room.provided.RoomEditor;
import mudmk3.application.world.provided.WorldSettingsEditor;
import mudmk3.application.zone.provided.ZoneEditor;
import mudmk3.domain.room.Direction;
import mudmk3.domain.room.Room;
import mudmk3.domain.zone.Zone;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static mudmk3.domain.room.RoomFixture.createRoomSaveRequest;
import static mudmk3.domain.zone.ZoneFixture.createZoneSaveRequest;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 트랜잭션(테스트 트랜잭션 없음)과 여러 스레드로 방 삭제 반영을 확인한다.
 * 각 포트 호출이 자기 트랜잭션을 커밋하므로 따로 쓰는 DB를 두고, 테스트마다 이름을 달리한다.
 */
@SpringBootTest
@Import(TestFakesConfiguration.class)
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:game-concurrency;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;LOCK_TIMEOUT=60000")
class GameConnectorConcurrencyTest {

    private static final String RELOCATED = "서 있던 곳이 사라져 시작 장소로 옮겨졌습니다.";
    private static final long TIMEOUT_SECONDS = 20;

    private final GameConnector gameConnector;
    private final RecordingGameMessageSender sender;
    private final AccountLogin accountLogin;
    private final CharacterCreator characterCreator;
    private final CharacterFinder characterFinder;
    private final CharacterMover characterMover;
    private final ZoneEditor zoneEditor;
    private final RoomEditor roomEditor;
    private final WorldSettingsEditor worldSettingsEditor;

    private final List<String> openedSessions = new ArrayList<>();

    GameConnectorConcurrencyTest(
            GameConnector gameConnector,
            RecordingGameMessageSender sender,
            AccountLogin accountLogin,
            CharacterCreator characterCreator,
            CharacterFinder characterFinder,
            CharacterMover characterMover,
            ZoneEditor zoneEditor,
            RoomEditor roomEditor,
            WorldSettingsEditor worldSettingsEditor
    ) {
        this.gameConnector = gameConnector;
        this.sender = sender;
        this.accountLogin = accountLogin;
        this.characterCreator = characterCreator;
        this.characterFinder = characterFinder;
        this.characterMover = characterMover;
        this.zoneEditor = zoneEditor;
        this.roomEditor = roomEditor;
        this.worldSettingsEditor = worldSettingsEditor;
    }

    @AfterEach
    void disconnectAll() {
        openedSessions.forEach(gameConnector::disconnect);
        sender.clear();
    }

    /** 이웃한(양방향 출구로 이어진) 방 둘을 동시에 지워도 멈추지 않고, 두 방 사람이 시작 방으로 옮겨진다 */
    @Test
    void neighborRoomsDeletedConcurrently() throws Exception {
        Zone zone = zoneEditor.create(createZoneSaveRequest("동시삭제촌"));
        Long start = room(zone, "광장");
        Long first = room(zone, "동쪽 집");
        Long second = room(zone, "서쪽 집");
        roomEditor.putExit(start, Direction.NORTH, new ExitSaveRequest(first, true));
        roomEditor.putExit(first, Direction.EAST, new ExitSaveRequest(second, true));
        worldSettingsEditor.designateStartRoom(start);
        Long hanbaek = enterAt("n-a", "한백", first);
        Long hanli = enterAt("n-b", "한립", second);
        enterAt("n-c", "여소", start);
        sender.clear();

        runConcurrently(() -> deleteWithRetry(first), () -> deleteWithRetry(second));

        assertThat(roomIdOf(hanbaek)).isEqualTo(start);
        assertThat(roomIdOf(hanli)).isEqualTo(start);
        for (String sessionId : List.of("n-a", "n-b")) {
            assertThat(sender.messagesTo(sessionId).getFirst()).isEqualTo(SystemMessage.info(RELOCATED));
            assertThat(sender.lastTo(sessionId, RoomMessage.class).room().id()).isEqualTo(start);
        }
        assertThat(sender.messagesTo("n-c", NoticeMessage.class))
                .extracting(NoticeMessage::text)
                .containsExactlyInAnyOrder("한백이 나타난다.", "한립이 나타난다.");

        sender.clear();
        gameConnector.handle("n-a", "보기");
        assertThat(sender.lastTo("n-a", RoomMessage.class).occupants())
                .extracting(RoomMessage.OccupantView::name)
                .containsExactlyInAnyOrder("한립", "여소");
    }

    /** 방이 지워지는 동안(커밋과 반영 사이 포함) 그 방 사람의 명령이 들어와도 예외 없이 처리되고 시작 방에 이른다 */
    @Test
    void commandsWhileRoomBeingDeleted() throws Exception {
        Zone zone = zoneEditor.create(createZoneSaveRequest("명령촌"));
        Long start = room(zone, "어귀");
        Long doomed = room(zone, "헛간");
        roomEditor.putExit(start, Direction.EAST, new ExitSaveRequest(doomed, true));
        worldSettingsEditor.designateStartRoom(start);
        Long hanbaek = enterAt("c-a", "남궁완", doomed);
        enterAt("c-b", "묵대부", start);
        CountDownLatch deleted = new CountDownLatch(1);

        runConcurrently(
                () -> {
                    roomEditor.delete(doomed);
                    deleted.countDown();
                    return null;
                },
                () -> {
                    for (int i = 0; i < 30 || deleted.getCount() > 0; i++) {
                        gameConnector.handle("c-a", i % 3 == 0 ? "보기" : i % 3 == 1 ? "'여보시오" : "누구");
                        if (i > 10_000) break;
                    }
                    return null;
                });

        sender.clear();
        gameConnector.handle("c-a", "보기");
        assertThat(sender.lastTo("c-a", RoomMessage.class).room().id()).isEqualTo(start);
        assertThat(roomIdOf(hanbaek)).isEqualTo(start);
    }

    private void runConcurrently(Callable<?>... tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.length);
        CountDownLatch ready = new CountDownLatch(1);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (Callable<?> task : tasks) {
                futures.add(executor.submit(() -> {
                    ready.await();
                    return task.call();
                }));
            }
            ready.countDown();
            for (Future<?> future : futures) {
                future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    /** 두 삭제가 같은 출구 행을 엇갈려 잡으면 DB가 교착을 알아채 한쪽을 실패시킨다 — 관리자가 다시 시도하는 셈 */
    private @Nullable Void deleteWithRetry(Long roomId) throws InterruptedException {
        for (int attempt = 1; ; attempt++) {
            try {
                roomEditor.delete(roomId);
                return null;
            } catch (RuntimeException e) {
                if (attempt >= 5) throw e;
                Thread.sleep(50L * attempt);
            }
        }
    }

    private Long room(Zone zone, String name) {
        Room room = roomEditor.create(createRoomSaveRequest(zone.getId(), name));
        return id(room.getId());
    }

    private Long enterAt(String sessionId, String name, Long roomId) {
        String email = "c" + name.codePoints().mapToObj(Integer::toHexString).reduce("", String::concat) + "@example.com";
        Long accountId = accountLogin.login(new AccountLoginRequest("google-" + email, email)).accountId();
        Long characterId = id(characterCreator.create(accountId, name).getId());
        characterMover.moveTo(characterId, roomId);
        openedSessions.add(sessionId);
        gameConnector.connect(sessionId, accountId);
        return accountId;
    }

    private Long roomIdOf(Long accountId) {
        return characterFinder.findByAccountId(accountId).orElseThrow().getRoomId();
    }

    private static Long id(@Nullable Long id) {
        if (id == null) throw new IllegalStateException("저장된 엔티티에 id가 없습니다");
        return id;
    }
}
