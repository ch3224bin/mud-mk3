package mudmk3.application.game.provided;

import jakarta.persistence.EntityManager;
import mudmk3.application.account.provided.AccountLogin;
import mudmk3.application.account.provided.AccountLoginRequest;
import mudmk3.application.character.provided.CharacterCreator;
import mudmk3.application.character.provided.CharacterFinder;
import mudmk3.application.character.provided.CharacterMover;
import mudmk3.application.game.RecordingGameMessageSender;
import mudmk3.application.room.provided.ExitSaveRequest;
import mudmk3.application.room.provided.RoomEditor;
import mudmk3.application.world.provided.WorldSettingsEditor;
import mudmk3.application.zone.provided.ZoneEditor;
import mudmk3.domain.room.Direction;
import mudmk3.domain.room.Room;
import mudmk3.domain.zone.Zone;
import org.junit.jupiter.api.AfterEach;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;

import static mudmk3.domain.room.RoomFixture.createRoomSaveRequest;
import static mudmk3.domain.zone.ZoneFixture.createZoneSaveRequest;

/**
 * 게임 포트 테스트의 기반. 접속 상태(presence, 세션)는 메모리 싱글턴이라 테스트 롤백으로 지워지지 않으므로
 * 테스트가 연 세션을 끝날 때 모두 끊는다.
 */
abstract class BaseGameTest {

    protected final GameConnector gameConnector;
    protected final RecordingGameMessageSender sender;
    protected final AccountLogin accountLogin;
    protected final CharacterCreator characterCreator;
    protected final CharacterFinder characterFinder;
    protected final CharacterMover characterMover;
    protected final ZoneEditor zoneEditor;
    protected final RoomEditor roomEditor;
    protected final WorldSettingsEditor worldSettingsEditor;
    protected final EntityManager entityManager;

    private final List<String> openedSessions = new ArrayList<>();

    protected BaseGameTest(
            GameConnector gameConnector,
            RecordingGameMessageSender sender,
            AccountLogin accountLogin,
            CharacterCreator characterCreator,
            CharacterFinder characterFinder,
            CharacterMover characterMover,
            ZoneEditor zoneEditor,
            RoomEditor roomEditor,
            WorldSettingsEditor worldSettingsEditor,
            EntityManager entityManager
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
        this.entityManager = entityManager;
    }

    @BeforeEach
    void clearSender() {
        sender.clear();
    }

    @AfterEach
    void disconnectAll() {
        openedSessions.forEach(gameConnector::disconnect);
    }

    /** 지역 청우촌(마을 어귀(시작) 북↔남 우물가), 지역 청석숲(숲 입구), 우물가 동↔서 숲 입구 */
    protected GameMap prepareMap() {
        Zone village = zoneEditor.create(createZoneSaveRequest("청우촌"));
        Zone forest = zoneEditor.create(createZoneSaveRequest("청석숲"));
        Room entrance = roomEditor.create(createRoomSaveRequest(village.getId(), "마을 어귀"));
        Room well = roomEditor.create(createRoomSaveRequest(village.getId(), "우물가"));
        Room forestEntrance = roomEditor.create(createRoomSaveRequest(forest.getId(), "숲 입구"));
        roomEditor.putExit(entrance.getId(), Direction.NORTH, new ExitSaveRequest(well.getId(), true));
        roomEditor.putExit(well.getId(), Direction.EAST, new ExitSaveRequest(forestEntrance.getId(), true));
        worldSettingsEditor.designateStartRoom(entrance.getId());
        return new GameMap(id(village.getId()), id(forest.getId()),
                id(entrance.getId()), id(well.getId()), id(forestEntrance.getId()));
    }

    protected Long prepareAccount(String email) {
        return accountLogin.login(new AccountLoginRequest("google-" + email, email)).accountId();
    }

    /** 캐릭터를 만든 계정으로 접속한다(시작 방에서 시작) */
    protected Long enter(String sessionId, String name) {
        Long accountId = prepareAccount(emailOf(name));
        characterCreator.create(accountId, name);
        connect(sessionId, accountId);
        return accountId;
    }

    /** 캐릭터를 그 방에 옮겨 둔 뒤 접속한다 */
    protected Long enterAt(String sessionId, String name, Long roomId) {
        Long accountId = prepareAccount(emailOf(name));
        Long characterId = id(characterCreator.create(accountId, name).getId());
        characterMover.moveTo(characterId, roomId);
        connect(sessionId, accountId);
        return accountId;
    }

    protected void connect(String sessionId, Long accountId) {
        openedSessions.add(sessionId);
        gameConnector.connect(sessionId, accountId);
    }

    protected Long roomIdOf(Long accountId) {
        entityManager.flush();
        entityManager.clear();
        return characterFinder.findByAccountId(accountId).orElseThrow().getRoomId();
    }

    /** 한글 이름은 이메일 로컬 파트로 쓸 수 없어 코드 포인트로 바꾼다 */
    private static String emailOf(String name) {
        return "p" + name.codePoints().mapToObj(Integer::toHexString).reduce("", String::concat) + "@example.com";
    }

    private static Long id(@Nullable Long id) {
        if (id == null) throw new IllegalStateException("저장된 엔티티에 id가 없습니다");
        return id;
    }

    protected record GameMap(Long villageZoneId, Long forestZoneId, Long entranceId, Long wellId, Long forestEntranceId) {
    }
}
