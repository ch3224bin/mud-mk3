package mudmk3.application.game.provided;

import jakarta.persistence.EntityManager;
import mudmk3.ApplicationServiceTest;
import mudmk3.application.account.provided.AccountLogin;
import mudmk3.application.character.provided.CharacterCreator;
import mudmk3.application.character.provided.CharacterFinder;
import mudmk3.application.character.provided.CharacterMover;
import mudmk3.application.game.RecordingGameMessageSender;
import mudmk3.application.game.required.GameCloseReason;
import mudmk3.application.game.required.GameMessage;
import mudmk3.application.game.required.GameMessage.NoticeMessage;
import mudmk3.application.game.required.GameMessage.PromptMessage;
import mudmk3.application.game.required.GameMessage.RoomMessage;
import mudmk3.application.game.required.GameMessage.RoomMessage.OccupantView;
import mudmk3.application.game.required.GameMessage.SayMessage;
import mudmk3.application.game.required.GameMessage.SessionMessage;
import mudmk3.application.game.required.GameMessage.SystemMessage;
import mudmk3.application.room.provided.RoomEditor;
import mudmk3.application.world.provided.WorldSettingsEditor;
import mudmk3.application.zone.provided.ZoneEditor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationServiceTest
class GameConnectorTest extends BaseGameTest {

    private static final String NAME_PROMPT = "이름을 정하십시오. (한글 2~6자)";
    private static final String UNKNOWN = "무슨 말인지 모르겠습니다. ('도움말' 로 명령을 볼 수 있습니다)";

    GameConnectorTest(
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
        super(gameConnector, sender, accountLogin, characterCreator, characterFinder, characterMover,
                zoneEditor, roomEditor, worldSettingsEditor, entityManager);
    }

    @Test
    void connectWithoutStartRoomFail() {
        Long accountId = prepareAccount("hanbaek@example.com");

        connect("s1", accountId);

        assertThat(sender.messagesTo("s1")).containsExactly(new SystemMessage(
                "아직 세계가 열리지 않았습니다. 관리자가 시작 방을 정해야 합니다.", "error"));
        assertThat(sender.closeReasonOf("s1")).contains(GameCloseReason.WORLD_NOT_READY);
    }

    @Test
    void createCharacterFlow() {
        GameMap map = prepareMap();
        Long accountId = prepareAccount("hanbaek@example.com");

        connect("s1", accountId);
        assertThat(sender.messagesTo("s1")).containsExactly(
                new SessionMessage("creating", null), new PromptMessage(NAME_PROMPT));

        sender.clear();
        gameConnector.handle("s1", "한 백");
        assertThat(sender.messagesTo("s1")).containsExactly(
                new SystemMessage("이름은 띄어쓰기 없이 한글 2~6자로 정하십시오.", "error"),
                new PromptMessage(NAME_PROMPT));

        sender.clear();
        gameConnector.handle("s1", "한백");
        assertThat(sender.messagesTo("s1")).containsExactly(
                new PromptMessage("이름 한백\n이대로 만들려면 '확인', 고치려면 '뒤로'."));

        sender.clear();
        gameConnector.handle("s1", "아니");
        assertThat(sender.messagesTo("s1")).containsExactly(
                new PromptMessage("이름 한백\n이대로 만들려면 '확인', 고치려면 '뒤로'."));

        sender.clear();
        gameConnector.handle("s1", "뒤로");
        assertThat(sender.messagesTo("s1")).containsExactly(new PromptMessage(NAME_PROMPT));

        gameConnector.handle("s1", "한백");
        sender.clear();
        gameConnector.handle("s1", "확인");

        List<GameMessage> messages = sender.messagesTo("s1");
        assertThat(messages).hasSize(3);
        assertThat(messages.get(0)).isEqualTo(new SystemMessage("캐릭터를 만들었습니다.", "info"));
        assertThat(messages.get(1)).isEqualTo(new SessionMessage("playing", "한백"));
        RoomMessage room = (RoomMessage) messages.get(2);
        assertThat(room.room().name()).isEqualTo("마을 어귀");
        assertThat(room.room().id()).isEqualTo(map.entranceId());
        assertThat(room.zone().name()).isEqualTo("청우촌");
        assertThat(room.zoneEntered()).isTrue();
        assertThat(room.exits()).containsExactly("북");
        assertThat(room.occupants()).isEmpty();
        assertThat(characterFinder.findByAccountId(accountId)).isPresent();
    }

    @Test
    void createCharacterNotifiesStartRoom() {
        prepareMap();
        enter("other", "한립");
        connect("s1", prepareAccount("hanbaek@example.com"));
        gameConnector.handle("s1", "한백");
        sender.clear();

        gameConnector.handle("s1", "확인");

        assertThat(sender.messagesTo("other"))
                .containsExactly(new NoticeMessage("한백이 나타난다.", "movement", "한백", "appear"));
        assertThat(sender.lastTo("s1", RoomMessage.class).occupants())
                .containsExactly(new OccupantView("한립", "player"));
    }

    @Test
    void createCharacterDuplicateNameFail() {
        prepareMap();
        characterCreator.create(prepareAccount("first@example.com"), "한백");
        connect("s1", prepareAccount("hanbaek@example.com"));
        sender.clear();

        gameConnector.handle("s1", "한백");

        assertThat(sender.messagesTo("s1")).containsExactly(
                new SystemMessage("이미 있는 이름입니다. 다른 이름을 정하십시오.", "error"),
                new PromptMessage(NAME_PROMPT));
    }

    @Test
    void createCharacterNameTakenWhileConfirmingFail() {
        prepareMap();
        connect("s1", prepareAccount("hanbaek@example.com"));
        gameConnector.handle("s1", "한백");
        characterCreator.create(prepareAccount("first@example.com"), "한백");
        sender.clear();

        gameConnector.handle("s1", "확인");

        assertThat(sender.messagesTo("s1")).containsExactly(
                new SystemMessage("이미 있는 이름입니다. 다른 이름을 정하십시오.", "error"),
                new PromptMessage(NAME_PROMPT));
    }

    @Test
    void connectExistingCharacter() {
        GameMap map = prepareMap();

        enter("s1", "한백");

        List<GameMessage> messages = sender.messagesTo("s1");
        assertThat(messages.get(0)).isEqualTo(new SessionMessage("playing", "한백"));
        RoomMessage room = (RoomMessage) messages.get(1);
        assertThat(room.room().id()).isEqualTo(map.entranceId());
        assertThat(room.zoneEntered()).isTrue();
        assertThat(messages).hasSize(2);
    }

    @Test
    void lookShowsOthersExcludingSelf() {
        prepareMap();
        enter("a", "한백");
        enter("b", "한립");
        sender.clear();

        gameConnector.handle("a", "보기");

        RoomMessage room = sender.lastTo("a", RoomMessage.class);
        assertThat(room.occupants()).containsExactly(new OccupantView("한립", "player"));
        assertThat(room.zoneEntered()).isFalse();
        assertThat(room.room().name()).isEqualTo("마을 어귀");
        assertThat(room.room().description()).isEqualTo("마을 어귀의 묘사다.");

        gameConnector.handle("a", "ㅂ");
        assertThat(sender.messagesTo("a", RoomMessage.class)).hasSize(2);
    }

    @Test
    void lookWithArgument() {
        prepareMap();
        enter("a", "한백");
        sender.clear();

        gameConnector.handle("a", "보기 한립");

        assertThat(sender.messagesTo("a")).containsExactly(new SystemMessage("아직 자세히 볼 수 없습니다.", "info"));
    }

    @Test
    void moveNotifiesBothRooms() {
        GameMap map = prepareMap();
        Long hanbaek = enter("a", "한백");
        enterAt("b", "한립", map.wellId());
        enter("c", "여소");
        sender.clear();

        gameConnector.handle("a", "북");

        RoomMessage room = sender.lastTo("a", RoomMessage.class);
        assertThat(room.room().name()).isEqualTo("우물가");
        assertThat(room.zoneEntered()).isFalse();
        assertThat(room.exits()).containsExactly("동", "남");
        assertThat(room.occupants()).containsExactly(new OccupantView("한립", "player"));
        assertThat(sender.messagesTo("c"))
                .containsExactly(new NoticeMessage("한백이 북쪽으로 떠난다.", "movement", "한백", "leave"));
        assertThat(sender.messagesTo("b"))
                .containsExactly(new NoticeMessage("한백이 남쪽에서 다가온다.", "movement", "한백", "arrive"));
        assertThat(roomIdOf(hanbaek)).isEqualTo(map.wellId());

        sender.clear();
        gameConnector.handle("c", "보기");
        assertThat(sender.lastTo("c", RoomMessage.class).occupants()).isEmpty();
    }

    @Test
    void moveNoExitFail() {
        GameMap map = prepareMap();
        Long hanbaek = enter("a", "한백");
        sender.clear();

        gameConnector.handle("a", "남");

        assertThat(sender.messagesTo("a")).containsExactly(new SystemMessage("그쪽으로는 길이 없습니다.", "error"));
        assertThat(roomIdOf(hanbaek)).isEqualTo(map.entranceId());
    }

    @Test
    void moveIntoOtherZoneSetsZoneEntered() {
        GameMap map = prepareMap();
        enterAt("a", "한백", map.wellId());
        sender.clear();

        gameConnector.handle("a", "동");

        RoomMessage room = sender.lastTo("a", RoomMessage.class);
        assertThat(room.room().name()).isEqualTo("숲 입구");
        assertThat(room.zoneEntered()).isTrue();
        assertThat(room.zone().name()).isEqualTo("청석숲");
        assertThat(room.zone().id()).isEqualTo(map.forestZoneId());

        gameConnector.handle("a", "w");
        RoomMessage back = sender.lastTo("a", RoomMessage.class);
        assertThat(back.room().name()).isEqualTo("우물가");
        assertThat(back.zoneEntered()).isTrue();
    }

    @Test
    void sayReachesSameRoomOnly() {
        GameMap map = prepareMap();
        enter("a", "한백");
        enter("b", "한립");
        enterAt("c", "여소", map.wellId());
        sender.clear();

        gameConnector.handle("a", "'길을 묻겠소.");

        assertThat(sender.messagesTo("a")).containsExactly(
                new SayMessage("한백", "길을 묻겠소.", true, "한백: \"길을 묻겠소.\"", "speech"));
        assertThat(sender.messagesTo("b")).containsExactly(
                new SayMessage("한백", "길을 묻겠소.", false, "한백이 말한다: \"길을 묻겠소.\"", "speech"));
        assertThat(sender.messagesTo("c")).isEmpty();
    }

    @Test
    void sayEmptyFail() {
        prepareMap();
        enter("a", "한백");
        enter("b", "한립");
        sender.clear();

        gameConnector.handle("a", "말하기");

        assertThat(sender.messagesTo("a")).containsExactly(new SystemMessage("무엇이라 말하시겠습니까?", "error"));
        assertThat(sender.messagesTo("b")).isEmpty();
    }

    @Test
    void sayTooLongFail() {
        prepareMap();
        enter("a", "한백");
        enter("b", "한립");
        sender.clear();

        gameConnector.handle("a", "말하기 " + "가".repeat(201));

        assertThat(sender.messagesTo("a")).containsExactly(new SystemMessage("말은 200자 이내로 하십시오.", "error"));
        assertThat(sender.messagesTo("b")).isEmpty();

        sender.clear();
        gameConnector.handle("a", "'" + "가".repeat(200));
        assertThat(sender.messagesTo("b", SayMessage.class)).hasSize(1);
    }

    @Test
    void commandTooLongFail() {
        prepareMap();
        enter("a", "한백");
        sender.clear();

        gameConnector.handle("a", "'" + "가".repeat(500));

        assertThat(sender.messagesTo("a")).containsExactly(new SystemMessage("명령이 너무 깁니다.", "error"));
    }

    @Test
    void whoListsOnlinePlayers() {
        GameMap map = prepareMap();
        enter("a", "한백");
        enterAt("b", "한립", map.forestEntranceId());
        sender.clear();

        gameConnector.handle("a", "누구");

        assertThat(sender.messagesTo("a")).containsExactly(new SystemMessage(
                "접속한 사람 (2)\n  한립    청석숲\n  한백    청우촌", "info"));
    }

    @Test
    void unknownCommand() {
        prepareMap();
        enter("a", "한백");
        sender.clear();

        gameConnector.handle("a", "말 안녕");
        gameConnector.handle("a", "보");

        assertThat(sender.messagesTo("a")).containsExactly(
                new SystemMessage(UNKNOWN, "error"), new SystemMessage(UNKNOWN, "error"));
    }

    @Test
    void emptyLineIgnored() {
        prepareMap();
        enter("a", "한백");
        sender.clear();

        gameConnector.handle("a", "   ");

        assertThat(sender.messagesTo("a")).isEmpty();
    }

    @Test
    void help() {
        prepareMap();
        enter("a", "한백");
        sender.clear();

        gameConnector.handle("a", "도움말");

        assertThat(sender.messagesTo("a")).containsExactly(new SystemMessage(
                "명령: 보기(ㅂ) · 동 서 남 북 북동 북서 남동 남서 위 아래 (e w s n ne nw se sw u d) · 말하기(') · 누구 · 도움말 · 종료",
                "info"));
    }

    @Test
    void quitRemovesAndCloses() {
        prepareMap();
        enter("a", "한백");
        enter("b", "한립");
        sender.clear();

        gameConnector.handle("a", "종료");

        assertThat(sender.messagesTo("a")).containsExactly(new SystemMessage("다음에 또 뵙겠습니다.", "info"));
        assertThat(sender.closeReasonOf("a")).contains(GameCloseReason.QUIT);
        assertThat(sender.messagesTo("b"))
                .containsExactly(new NoticeMessage("한백이 사라진다.", "movement", "한백", "disappear"));

        gameConnector.disconnect("a");
        assertThat(sender.messagesTo("b")).hasSize(1);

        gameConnector.handle("b", "누구");
        assertThat(sender.lastTo("b", SystemMessage.class).text()).isEqualTo("접속한 사람 (1)\n  한립    청우촌");
    }

    @Test
    void disconnectNotifiesAppearDisappear() {
        prepareMap();
        enter("b", "한립");
        sender.clear();

        enter("a", "한백");
        assertThat(sender.messagesTo("b"))
                .containsExactly(new NoticeMessage("한백이 나타난다.", "movement", "한백", "appear"));

        sender.clear();
        gameConnector.disconnect("a");
        assertThat(sender.messagesTo("b"))
                .containsExactly(new NoticeMessage("한백이 사라진다.", "movement", "한백", "disappear"));
        assertThat(sender.closeReasonOf("a")).isEmpty();
    }

    @Test
    void disconnectWhileCreatingIsSilent() {
        prepareMap();
        enter("b", "한립");
        connect("a", prepareAccount("hanbaek@example.com"));
        sender.clear();

        gameConnector.disconnect("a");

        assertThat(sender.messagesTo("b")).isEmpty();
    }

    @Test
    void reconnectSameAccountReplacesOldSession() {
        GameMap map = prepareMap();
        Long hanbaek = enter("a1", "한백");
        enter("b", "한립");
        sender.clear();

        connect("a2", hanbaek);

        assertThat(sender.messagesTo("a1"))
                .containsExactly(new SystemMessage("다른 곳에서 접속해 이 연결을 끊습니다.", "info"));
        assertThat(sender.closeReasonOf("a1")).contains(GameCloseReason.REPLACED);
        assertThat(sender.messagesTo("b")).isEmpty();
        assertThat(sender.messagesTo("a2").getFirst()).isEqualTo(new SessionMessage("playing", "한백"));
        RoomMessage room = sender.lastTo("a2", RoomMessage.class);
        assertThat(room.room().id()).isEqualTo(map.entranceId());
        assertThat(room.occupants()).containsExactly(new OccupantView("한립", "player"));

        gameConnector.disconnect("a1");
        assertThat(sender.messagesTo("b")).isEmpty();

        gameConnector.handle("b", "보기");
        assertThat(sender.lastTo("b", RoomMessage.class).occupants())
                .containsExactly(new OccupantView("한백", "player"));
        gameConnector.handle("a2", "'여기 있소");
        assertThat(sender.messagesTo("b", SayMessage.class)).hasSize(1);
    }

    @Test
    void reconnectWhileCreatingReplacesOldSession() {
        prepareMap();
        Long accountId = prepareAccount("hanbaek@example.com");
        connect("a1", accountId);
        gameConnector.handle("a1", "한백");
        sender.clear();

        connect("a2", accountId);

        assertThat(sender.closeReasonOf("a1")).contains(GameCloseReason.REPLACED);
        assertThat(sender.messagesTo("a2"))
                .containsExactly(new SessionMessage("creating", null), new PromptMessage(NAME_PROMPT));
    }

    @Test
    void reconnectStartsInSavedRoom() {
        GameMap map = prepareMap();
        Long hanbaek = enter("a1", "한백");
        gameConnector.handle("a1", "북");
        gameConnector.disconnect("a1");
        sender.clear();

        connect("a2", hanbaek);

        RoomMessage room = sender.lastTo("a2", RoomMessage.class);
        assertThat(room.room().id()).isEqualTo(map.wellId());
        assertThat(room.zoneEntered()).isTrue();
    }

    @Test
    void savedRoomDeletedStartsInStartRoom() {
        GameMap map = prepareMap();
        Long hanbaek = enter("a1", "한백");
        gameConnector.handle("a1", "북");
        gameConnector.disconnect("a1");
        // 접속해 있지 않을 때 방이 지워짐 — 리스너가 DB의 캐릭터를 옮기지만, 옮겨지지 않은 경우도 대비한다
        characterMover.moveTo(characterFinder.findByAccountId(hanbaek).orElseThrow().getId(), -1L);
        sender.clear();

        connect("a2", hanbaek);

        assertThat(sender.lastTo("a2", RoomMessage.class).room().id()).isEqualTo(map.entranceId());
        assertThat(roomIdOf(hanbaek)).isEqualTo(map.entranceId());
    }

    @Test
    void savedRoomDeletedWhileOfflineStartsInStartRoom() {
        GameMap map = prepareMap();
        Long hanbaek = enter("a1", "한백");
        gameConnector.handle("a1", "북");
        gameConnector.disconnect("a1");
        roomEditor.delete(map.wellId());
        sender.clear();

        connect("a2", hanbaek);

        assertThat(sender.lastTo("a2", RoomMessage.class).room().id()).isEqualTo(map.entranceId());
        assertThat(roomIdOf(hanbaek)).isEqualTo(map.entranceId());
    }

    /** 테스트 트랜잭션은 커밋되지 않아 삭제 반영(AFTER_COMMIT)이 오지 않는다 — "지워졌지만 반영 전" 상태다 */
    @Test
    void lookAfterRoomDeletedBeforeReflectionMovesToStartRoom() {
        GameMap map = prepareMap();
        Long hanbaek = enter("a", "한백");
        enter("b", "한립");
        gameConnector.handle("a", "북");
        roomEditor.delete(map.wellId());
        sender.clear();

        gameConnector.handle("a", "보기");

        List<GameMessage> messages = sender.messagesTo("a");
        assertThat(messages).hasSize(2);
        assertThat(messages.getFirst())
                .isEqualTo(new SystemMessage("서 있던 곳이 사라져 시작 장소로 옮겨졌습니다.", "info"));
        RoomMessage room = (RoomMessage) messages.get(1);
        assertThat(room.room().id()).isEqualTo(map.entranceId());
        assertThat(room.exits()).isEmpty();
        assertThat(room.occupants()).containsExactly(new OccupantView("한립", "player"));
        assertThat(sender.messagesTo("b"))
                .containsExactly(new NoticeMessage("한백이 나타난다.", "movement", "한백", "appear"));
        assertThat(roomIdOf(hanbaek)).isEqualTo(map.entranceId());

        sender.clear();
        gameConnector.handle("a", "북");
        assertThat(sender.messagesTo("a")).containsExactly(new SystemMessage("그쪽으로는 길이 없습니다.", "error"));
    }

    @Test
    void moveAfterRoomDeletedBeforeReflectionUsesStartRoom() {
        GameMap map = prepareMap();
        enterAt("a", "한백", map.wellId());
        roomEditor.delete(map.wellId());
        sender.clear();

        gameConnector.handle("a", "남");

        List<GameMessage> messages = sender.messagesTo("a");
        assertThat(messages.getFirst())
                .isEqualTo(new SystemMessage("서 있던 곳이 사라져 시작 장소로 옮겨졌습니다.", "info"));
        assertThat(sender.lastTo("a", RoomMessage.class).room().id()).isEqualTo(map.entranceId());
        assertThat(sender.lastTo("a", SystemMessage.class).text()).isEqualTo("그쪽으로는 길이 없습니다.");
    }

    @Test
    void sayAfterRoomDeletedBeforeReflectionReachesStartRoom() {
        GameMap map = prepareMap();
        enterAt("a", "한백", map.wellId());
        enter("b", "한립");
        roomEditor.delete(map.wellId());
        sender.clear();

        gameConnector.handle("a", "'여기가 어디요");

        assertThat(sender.messagesTo("b")).containsExactly(
                new NoticeMessage("한백이 나타난다.", "movement", "한백", "appear"),
                new SayMessage("한백", "여기가 어디요", false, "한백이 말한다: \"여기가 어디요\"", "speech"));
    }

    @Test
    void whoAfterOthersRoomDeletedBeforeReflection() {
        GameMap map = prepareMap();
        enterAt("a", "한백", map.forestEntranceId());
        enter("b", "한립");
        roomEditor.delete(map.forestEntranceId());
        sender.clear();

        gameConnector.handle("b", "누구");

        assertThat(sender.lastTo("b", SystemMessage.class).text())
                .isEqualTo("접속한 사람 (2)\n  한립    청우촌\n  한백    청우촌");
        assertThat(sender.messagesTo("a").getFirst())
                .isEqualTo(new SystemMessage("서 있던 곳이 사라져 시작 장소로 옮겨졌습니다.", "info"));
    }

    @Test
    void reconnectAfterRoomDeletedBeforeReflection() {
        GameMap map = prepareMap();
        Long hanbaek = enterAt("a1", "한백", map.wellId());
        roomEditor.delete(map.wellId());
        sender.clear();

        connect("a2", hanbaek);

        assertThat(sender.messagesTo("a2")).hasSize(3);
        assertThat(sender.messagesTo("a2").get(1))
                .isEqualTo(new SystemMessage("서 있던 곳이 사라져 시작 장소로 옮겨졌습니다.", "info"));
        assertThat(sender.lastTo("a2", RoomMessage.class).room().id()).isEqualTo(map.entranceId());
    }
}
