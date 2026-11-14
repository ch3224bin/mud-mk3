package mudmk3.application.game.provided;

import jakarta.persistence.EntityManager;
import mudmk3.ApplicationServiceTest;
import mudmk3.MutableClock;
import mudmk3.application.account.provided.AccountLogin;
import mudmk3.application.character.provided.CharacterCreator;
import mudmk3.application.character.provided.CharacterFinder;
import mudmk3.application.character.provided.CharacterMover;
import mudmk3.application.faction.provided.FactionEditor;
import mudmk3.application.floor.provided.FloorPlacementEditor;
import mudmk3.application.game.RecordingGameMessageSender;
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
import mudmk3.application.game.required.GameMessage.SayMessage;
import mudmk3.application.game.required.GameMessage.SessionMessage;
import mudmk3.application.game.required.GameMessage.SystemMessage;
import mudmk3.application.game.required.GameMessage.TalkMessage;
import mudmk3.application.inventory.provided.InventoryEditor;
import mudmk3.application.inventory.provided.InventoryFinder;
import mudmk3.application.item.provided.ItemTypeEditor;
import mudmk3.application.item.provided.ItemTypeFinder;
import mudmk3.application.npc.provided.NpcEditor;
import mudmk3.application.npc.provided.NpcFinder;
import mudmk3.application.npc.provided.NpcSaveRequest;
import mudmk3.application.npc.provided.NpcTopicRequest;
import mudmk3.application.room.provided.RoomEditor;
import mudmk3.application.world.provided.StartingItemRequest;
import mudmk3.application.world.provided.WorldSettingsEditor;
import mudmk3.application.zone.provided.ZoneEditor;
import mudmk3.application.zone.provided.ZoneSaveRequest;
import mudmk3.domain.faction.Faction;
import mudmk3.domain.faction.FactionKind;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static mudmk3.domain.faction.FactionFixture.createFactionSaveRequest;
import static mudmk3.domain.npc.NpcFixture.createNpcSaveRequest;
import static org.assertj.core.api.Assertions.assertThat;

@ApplicationServiceTest
class GameConnectorTest extends BaseGameTest {

    private static final String NAME_PROMPT = "이름을 정하십시오. (한글 2~6자)";
    private static final String UNKNOWN = "무슨 말인지 모르겠습니다. ('도움말' 로 명령을 볼 수 있습니다)";

    private static final String NO_TALK_TARGET = "누구에게 말을 거시겠습니까?";
    private static final String NO_SUCH_NPC = "여기에 그런 상대는 없습니다.";

    private final NpcFinder npcFinder;

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
            FactionEditor factionEditor,
            NpcEditor npcEditor,
            NpcFinder npcFinder,
            ItemTypeEditor itemTypeEditor,
            ItemTypeFinder itemTypeFinder,
            FloorPlacementEditor floorPlacementEditor,
            InventoryFinder inventoryFinder,
            InventoryEditor inventoryEditor,
            MutableClock clock,
            EntityManager entityManager
    ) {
        super(gameConnector, sender, accountLogin, characterCreator, characterFinder, characterMover,
                zoneEditor, roomEditor, worldSettingsEditor, factionEditor, npcEditor,
                itemTypeEditor, itemTypeFinder, floorPlacementEditor, inventoryFinder, inventoryEditor, clock, entityManager);
        this.npcFinder = npcFinder;
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
        assertThat(room.items()).isEmpty();
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
    void createCharacterWithNpcNameFail() {
        prepareMap();
        npcEditor.create(createNpcSaveRequest("왕씨", null));
        connect("s1", prepareAccount("hanbaek@example.com"));
        sender.clear();

        gameConnector.handle("s1", "왕씨");

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
        enter("b", "한립");
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
                "명령: 보기(ㅂ) · 동 서 남 북 북동 북서 남동 남서 위 아래 (e w s n ne nw se sw u d) · 말하기(') · 대화 <NPC> [낱말] · 누구 · 도움말 · 종료",
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
    // ---- 방의 소유와 NPC, 대화

    @Test
    void roomShowsNpcsAfterPlayers() {
        prepareNpcs(prepareMap());
        enter("a", "한백");
        enter("b", "한립");
        sender.clear();

        gameConnector.handle("a", "보기");

        RoomMessage room = sender.lastTo("a", RoomMessage.class);
        assertThat(room.occupants()).containsExactly(
                new OccupantView("한립", "player"), new OccupantView("왕씨", "npc"));
        assertThat(room.owner()).isNull();
    }

    @Test
    void roomShowsOwner() {
        GameMap map = prepareMap();
        NpcMap npcMap = prepareNpcs(map);
        enterAt("a", "한백", map.wellId());
        sender.clear();

        gameConnector.handle("a", "북");

        RoomMessage room = sender.lastTo("a", RoomMessage.class);
        assertThat(room.room().id()).isEqualTo(npcMap.southGateId());
        assertThat(room.owner()).isEqualTo(new OwnerView("castle", "청석성"));
        assertThat(room.zoneEntered()).isTrue();
    }

    @Test
    void talkGreetingListsWords() {
        prepareNpcs(prepareMap());
        enter("a", "한백");
        sender.clear();

        gameConnector.handle("a", "대화 왕씨");

        assertThat(sender.messagesTo("a")).containsExactly(new TalkMessage(
                "왕씨", null, "왕씨: \"어서 오시오.\"\n물어볼 수 있는 낱말: [단약] [일거리]", "talk"));
    }

    @Test
    void talkGreetingWithoutWords() {
        GameMap map = prepareMap();
        npcEditor.create(new NpcSaveRequest("이씨", "무슨 일이오?", "모르겠소.", List.of(), false, map.entranceId()));
        enter("a", "한백");
        sender.clear();

        gameConnector.handle("a", "대화 이씨");

        assertThat(sender.messagesTo("a")).containsExactly(new TalkMessage("이씨", null, "이씨: \"무슨 일이오?\"", "talk"));
    }

    @Test
    void talkKnownWord() {
        prepareNpcs(prepareMap());
        enter("a", "한백");
        sender.clear();

        gameConnector.handle("a", "대화 왕씨 일거리");
        gameConnector.handle("a", "대화 왕 [단약]");

        assertThat(sender.messagesTo("a")).containsExactly(
                new TalkMessage("왕씨", "일거리", "왕씨: \"늑대가 말썽이오.\"", "talk"),
                new TalkMessage("왕씨", "단약", "왕씨: \"회춘단이 좋소.\"", "talk"));
    }

    @Test
    void talkExactNameBeforePrefix() {
        GameMap map = prepareMap();
        prepareNpcs(map);
        npcEditor.create(new NpcSaveRequest("왕", "나는 왕이오.", "모르겠소.", List.of(), false, map.entranceId()));
        enter("a", "한백");
        sender.clear();

        gameConnector.handle("a", "대화 왕");

        assertThat(sender.lastTo("a", TalkMessage.class).npc()).isEqualTo("왕");
    }

    @Test
    void talkUnknownWordIsDefault() {
        prepareNpcs(prepareMap());
        enter("a", "한백");
        sender.clear();

        gameConnector.handle("a", "대화 왕씨 비석");

        assertThat(sender.messagesTo("a")).containsExactly(
                new TalkMessage("왕씨", "비석", "왕씨: \"글쎄, 잘 모르겠구려.\"", "talk"));
    }

    @Test
    void talkWithoutTargetFail() {
        prepareNpcs(prepareMap());
        enter("a", "한백");
        sender.clear();

        gameConnector.handle("a", "대화");
        gameConnector.handle("a", "대화   ");

        assertThat(sender.messagesTo("a")).containsExactly(
                new SystemMessage(NO_TALK_TARGET, "error"), new SystemMessage(NO_TALK_TARGET, "error"));
    }

    @Test
    void talkToPlayerOrAbsentNpcFail() {
        prepareNpcs(prepareMap());
        npcEditor.create(createNpcSaveRequest("조씨", null));
        enter("a", "한백");
        enter("b", "한립");
        sender.clear();

        gameConnector.handle("a", "대화 한립");
        gameConnector.handle("a", "대화 조씨");
        gameConnector.handle("a", "북");
        gameConnector.handle("a", "대화 왕씨");

        assertThat(sender.messagesTo("a", SystemMessage.class)).containsExactly(
                new SystemMessage(NO_SUCH_NPC, "error"),
                new SystemMessage(NO_SUCH_NPC, "error"),
                new SystemMessage(NO_SUCH_NPC, "error"));
        assertThat(sender.messagesTo("a", TalkMessage.class)).isEmpty();
    }

    @Test
    void talkIsPrivate() {
        prepareNpcs(prepareMap());
        enter("a", "한백");
        enter("b", "한립");
        sender.clear();

        gameConnector.handle("a", "대화 왕씨");
        gameConnector.handle("a", "대화 왕씨 단약");

        assertThat(sender.messagesTo("a", TalkMessage.class)).hasSize(2);
        assertThat(sender.messagesTo("b")).isEmpty();
    }

    @Test
    void llmEnabledNpcAnswersSame() {
        GameMap map = prepareMap();
        npcEditor.create(new NpcSaveRequest("유씨", "어서 오시오.", "글쎄, 잘 모르겠구려.", List.of(
                new NpcTopicRequest("단약", "회춘단이 좋소.")), true, map.entranceId()));
        enter("a", "한백");
        sender.clear();

        gameConnector.handle("a", "대화 유씨");
        gameConnector.handle("a", "대화 유씨 단약");
        gameConnector.handle("a", "대화 유씨 비석");

        assertThat(sender.messagesTo("a")).containsExactly(
                new TalkMessage("유씨", null, "유씨: \"어서 오시오.\"\n물어볼 수 있는 낱말: [단약]", "talk"),
                new TalkMessage("유씨", "단약", "유씨: \"회춘단이 좋소.\"", "talk"),
                new TalkMessage("유씨", "비석", "유씨: \"글쎄, 잘 모르겠구려.\"", "talk"));
    }

    @Test
    void ownerAndNpcChangesShowOnNextLook() {
        GameMap map = prepareMap();
        enter("a", "한백");
        sender.clear();

        Faction sect = factionEditor.create(createFactionSaveRequest(FactionKind.SECT, "황풍곡"));
        zoneEditor.update(map.villageZoneId(), new ZoneSaveRequest("청우촌", "작은 마을이다.", sect.getId()));
        npcEditor.create(createNpcSaveRequest("왕씨", map.entranceId()));
        assertThat(sender.messagesTo("a")).isEmpty();

        gameConnector.handle("a", "보기");

        RoomMessage room = sender.lastTo("a", RoomMessage.class);
        assertThat(room.owner()).isEqualTo(new OwnerView("sect", "황풍곡"));
        assertThat(room.occupants()).containsExactly(new OccupantView("왕씨", "npc"));
    }

    /** 테스트 트랜잭션은 커밋되지 않아 삭제 반영(AFTER_COMMIT)이 오지 않는다 — 다음 명령이 스스로 옮긴다 */
    @Test
    void roomDeletedWithNpcStillWorks() {
        GameMap map = prepareMap();
        Long npcId = npcEditor.create(createNpcSaveRequest("장씨", map.wellId())).getId();
        enterAt("a", "한백", map.wellId());
        enter("b", "한립");
        roomEditor.delete(map.wellId());
        sender.clear();

        gameConnector.handle("a", "보기");

        List<GameMessage> messages = sender.messagesTo("a");
        assertThat(messages.getFirst())
                .isEqualTo(new SystemMessage("서 있던 곳이 사라져 시작 장소로 옮겨졌습니다.", "info"));
        RoomMessage room = sender.lastTo("a", RoomMessage.class);
        assertThat(room.room().id()).isEqualTo(map.entranceId());
        assertThat(room.occupants()).containsExactly(new OccupantView("한립", "player"));
        assertThat(npcFinder.find(npcId).findRoomId()).isEmpty();

        sender.clear();
        gameConnector.handle("a", "대화 장씨");
        assertThat(sender.messagesTo("a")).containsExactly(new SystemMessage(NO_SUCH_NPC, "error"));
    }

    // ---- 물건 (3차)

    @Test
    void inventoryShowsStartingItems() {
        prepareMap();
        Items items = prepareItems();
        prepareStartingItems(new StartingItemRequest(items.bag(), 1), new StartingItemRequest(items.lowStone(), 30),
                new StartingItemRequest(items.pill(), 3), new StartingItemRequest(items.energyPill(), 3));
        enter("a", "한백");
        sender.clear();

        gameConnector.handle("a", "소지품");

        InventoryMessage inventory = sender.lastTo("a", InventoryMessage.class);
        assertThat(inventory.text()).isEqualTo(
                "[소지품 3/22칸]  하품 영석 30\n하급 저물대   회춘단(연기 초) x3   회기단(연기 초) x3");
        assertThat(inventory.usedSlots()).isEqualTo(3);
        assertThat(inventory.totalSlots()).isEqualTo(22);
        assertThat(inventory.tone()).isEqualTo("item");
        assertThat(inventory.stones()).containsExactly(new StoneView("하품 영석", 30));
        assertThat(inventory.items()).containsExactly(new ItemView("하급 저물대", 1, false, false),
                new ItemView("회춘단(연기 초)", 3, false, false), new ItemView("회기단(연기 초)", 3, false, false));

        sender.clear();
        gameConnector.handle("a", "ㅈ");
        assertThat(sender.lastTo("a", InventoryMessage.class)).isEqualTo(inventory);
    }

    @Test
    void inventoryMarksBoundAndQuestAndWraps() {
        prepareMap();
        Items items = prepareItems();
        Long hanbaek = enter("a", "한백");
        give(hanbaek, items.bag(), 1);
        give(hanbaek, items.sword(), 2);
        inventoryEditor.bind(characterIdOf(hanbaek), items.sword());
        give(hanbaek, items.quest(), 1);
        give(hanbaek, items.hide(), 1);
        sender.clear();

        gameConnector.handle("a", "소지품");

        InventoryMessage inventory = sender.lastTo("a", InventoryMessage.class);
        assertThat(inventory.text()).isEqualTo(
                "[소지품 4/22칸]\n하급 저물대   비검   비검 (귀속)\n왕씨의 약초 꾸러미 (퀘스트)   늑대 가죽");
        assertThat(inventory.items()).contains(new ItemView("비검", 1, true, false),
                new ItemView("왕씨의 약초 꾸러미", 1, false, true));
    }

    @Test
    void emptyInventory() {
        prepareMap();
        enter("a", "한백");
        sender.clear();

        gameConnector.handle("a", "소지품");

        InventoryMessage inventory = sender.lastTo("a", InventoryMessage.class);
        assertThat(inventory.text()).isEqualTo("[소지품 0/2칸]\n가진 물건이 없습니다.");
        assertThat(inventory.usedSlots()).isZero();
        assertThat(inventory.totalSlots()).isEqualTo(2);
        assertThat(inventory.stones()).isEmpty();
        assertThat(inventory.items()).isEmpty();
    }

    @Test
    void oldCharacterGetsEmptyInventory() {
        prepareMap();
        Long accountId = prepareAccount("hanbaek@example.com");
        Long characterId = characterCreator.create(accountId, "한백").getId();
        entityManager.flush();
        entityManager.createNativeQuery("DELETE FROM inventory WHERE character_id = ?1")
                .setParameter(1, characterId).executeUpdate();
        entityManager.clear();
        connect("a", accountId);
        sender.clear();

        gameConnector.handle("a", "소지품");

        assertThat(sender.lastTo("a", InventoryMessage.class).text()).isEqualTo("[소지품 0/2칸]\n가진 물건이 없습니다.");
    }

    @Test
    void inventoryWithArgumentFail() {
        prepareMap();
        enter("a", "한백");
        sender.clear();

        gameConnector.handle("a", "소지품 x");

        assertThat(sender.messagesTo("a")).containsExactly(SystemMessage.error(UNKNOWN));
    }

    @Test
    void roomShowsFloorItems() {
        GameMap map = prepareMap();
        prepareNpcs(map);
        Items items = prepareItems();
        place(map.entranceId(), items.hide(), 3, null);

        enter("a", "한백");

        RoomMessage room = sender.lastTo("a", RoomMessage.class);
        assertThat(room.occupants()).containsExactly(new OccupantView("왕씨", "npc"));
        assertThat(room.items()).containsExactly(new FloorItemView("늑대 가죽", 3));
    }

    @Test
    void pickSome() {
        GameMap map = prepareMap();
        Items items = prepareItems();
        place(map.entranceId(), items.hide(), 3, null);
        enter("a", "한백");
        enter("b", "한립");
        sender.clear();

        gameConnector.handle("a", "줍기 가죽 2");

        assertThat(sender.messagesTo("a")).containsExactly(SystemMessage.item("늑대 가죽 2개를 주웠습니다."));
        assertThat(sender.messagesTo("b")).containsExactly(
                new NoticeMessage("한백이 늑대 가죽 2개를 줍는다.", "item", "한백", "pick"));
        gameConnector.handle("a", "보기");
        assertThat(sender.lastTo("a", RoomMessage.class).items()).containsExactly(new FloorItemView("늑대 가죽", 1));
        gameConnector.handle("a", "소지품");
        assertThat(sender.lastTo("a", InventoryMessage.class).items()).containsExactly(new ItemView("늑대 가죽", 2, false, false));
    }

    @Test
    void pickAll() {
        GameMap map = prepareMap();
        Items items = prepareItems();
        place(map.entranceId(), items.hide(), 3, null);
        enter("a", "한백");
        sender.clear();

        gameConnector.handle("a", "줍기 늑대 가죽 전부");

        assertThat(sender.messagesTo("a")).containsExactly(SystemMessage.item("늑대 가죽 3개를 주웠습니다."));
        gameConnector.handle("a", "보기");
        assertThat(sender.lastTo("a", RoomMessage.class).items()).isEmpty();
        gameConnector.handle("a", "소지품");
        assertThat(sender.lastTo("a", InventoryMessage.class).items()).containsExactly(new ItemView("늑대 가죽", 3, false, false));
    }

    @Test
    void pickPartial() {
        GameMap map = prepareMap();
        Items items = prepareItems();
        place(map.entranceId(), items.sword(), 3, null);
        Long hanbaek = enter("a", "한백");
        give(hanbaek, items.lowStone(), 1);
        sender.clear();

        gameConnector.handle("a", "줍기 비검 3");

        assertThat(sender.messagesTo("a")).containsExactly(
                SystemMessage.item("저물대에 자리가 없어 비검 3개 중 1개만 주웠습니다."));
        gameConnector.handle("a", "보기");
        assertThat(sender.lastTo("a", RoomMessage.class).items()).containsExactly(new FloorItemView("비검", 2));
    }

    @Test
    void pickFails() {
        GameMap map = prepareMap();
        Items items = prepareItems();
        place(map.entranceId(), items.hide(), 3, null);
        place(map.entranceId(), items.bag(), 1, null);
        Long hanbaek = enter("a", "한백");
        enter("b", "한립");

        assertError("a", "줍기", "무엇을 주우시겠습니까? (예: 줍기 늑대 가죽)");
        assertError("a", "줍기 곰", "이곳에 '곰'은 없습니다.");
        assertError("a", "줍기 2.가죽", "이곳에 '2.가죽'은 없습니다.");
        assertError("a", "줍기 가죽 5", "늑대 가죽은 3개뿐입니다.");
        assertError("a", "줍기 가죽 0", "수량은 1 이상의 수로 적으십시오.");
        assertError("a", "줍기 가죽 1234567890", "수량은 1 이상의 수로 적으십시오.");

        give(hanbaek, items.sword(), 2);
        assertError("a", "줍기 가죽", "저물대에 자리가 없습니다.");

        give(hanbaek, items.bag(), 3);
        assertError("a", "줍기 저물대", "저물대는 세 개까지만 찰 수 있습니다.");
        assertThat(sender.messagesTo("b")).isEmpty();
    }

    @Test
    void dropAndPickBack() {
        prepareMap();
        Items items = prepareItems();
        Long hanbaek = enter("a", "한백");
        enter("b", "한립");
        give(hanbaek, items.fang(), 2);
        sender.clear();

        gameConnector.handle("a", "버리기 이빨 1");

        assertThat(sender.messagesTo("a")).containsExactly(SystemMessage.item("늑대 이빨 1개를 버렸습니다."));
        assertThat(sender.messagesTo("b")).containsExactly(
                new NoticeMessage("한백이 늑대 이빨 1개를 버린다.", "item", "한백", "drop"));

        gameConnector.handle("a", "버리기 이빨");
        gameConnector.handle("b", "보기");
        assertThat(sender.lastTo("b", RoomMessage.class).items()).containsExactly(new FloorItemView("늑대 이빨", 2));

        sender.clear();
        gameConnector.handle("a", "줍기 이빨 전부");
        assertThat(sender.messagesTo("a")).containsExactly(SystemMessage.item("늑대 이빨 2개를 주웠습니다."));
        gameConnector.handle("a", "소지품");
        assertThat(sender.lastTo("a", InventoryMessage.class).items()).containsExactly(new ItemView("늑대 이빨", 2, false, false));
    }

    @Test
    void dropStones() {
        prepareMap();
        Items items = prepareItems();
        Long hanbaek = enter("a", "한백");
        give(hanbaek, items.lowStone(), 30);
        sender.clear();

        gameConnector.handle("a", "버리기 하품 10");

        assertThat(sender.messagesTo("a")).containsExactly(SystemMessage.item("하품 영석 10개를 버렸습니다."));
        gameConnector.handle("a", "소지품");
        assertThat(sender.lastTo("a", InventoryMessage.class).stones()).containsExactly(new StoneView("하품 영석", 20));
        gameConnector.handle("a", "보기");
        assertThat(sender.lastTo("a", RoomMessage.class).items()).containsExactly(new FloorItemView("하품 영석", 10));
    }

    @Test
    void dropFails() {
        prepareMap();
        Items items = prepareItems();
        Long hanbaek = enter("a", "한백");
        enter("b", "한립");
        give(hanbaek, items.bag(), 1);
        give(hanbaek, items.fang(), 2);
        give(hanbaek, items.quest(), 1);
        give(hanbaek, items.sword(), 1);
        inventoryEditor.bind(characterIdOf(hanbaek), items.sword());

        assertError("a", "버리기", "무엇을 버리시겠습니까? (예: 버리기 늑대 가죽)");
        assertError("a", "버리기 곰", "그런 물건은 가지고 있지 않습니다.");
        assertError("a", "버리기 이빨 0", "수량은 1 이상의 수로 적으십시오.");
        assertError("a", "버리기 이빨 3", "늑대 이빨은 2개뿐입니다.");
        assertError("a", "버리기 꾸러미", "퀘스트 물건은 버릴 수 없습니다.");
        assertError("a", "버리기 비검", "귀속된 물건은 버릴 수 없습니다.");
        assertThat(sender.messagesTo("b")).isEmpty();
    }

    @Test
    void dropPrefersUnboundEntry() {
        prepareMap();
        Items items = prepareItems();
        Long hanbaek = enter("a", "한백");
        give(hanbaek, items.sword(), 2);
        inventoryEditor.bind(characterIdOf(hanbaek), items.sword());
        sender.clear();

        gameConnector.handle("a", "버리기 비검");

        assertThat(sender.messagesTo("a")).containsExactly(SystemMessage.item("비검 1개를 버렸습니다."));
        gameConnector.handle("a", "소지품");
        assertThat(sender.lastTo("a", InventoryMessage.class).items()).containsExactly(new ItemView("비검", 1, true, false));
    }

    @Test
    void dropBagInUseFail() {
        prepareMap();
        Items items = prepareItems();
        Long hanbaek = enter("a", "한백");
        give(hanbaek, items.bag(), 1);
        give(hanbaek, items.hide(), 99 * 21);
        sender.clear();

        assertError("a", "버리기 저물대", "저물대에 든 물건을 먼저 비우십시오.");

        gameConnector.handle("a", "소지품");
        InventoryMessage inventory = sender.lastTo("a", InventoryMessage.class);
        assertThat(inventory.usedSlots()).isEqualTo(21);
        assertThat(inventory.totalSlots()).isEqualTo(22);
        gameConnector.handle("a", "보기");
        assertThat(sender.lastTo("a", RoomMessage.class).items()).isEmpty();
    }

    @Test
    void droppedItemDisappearsAfterFiveMinutes() {
        prepareMap();
        Items items = prepareItems();
        Long hanbaek = enter("a", "한백");
        give(hanbaek, items.fang(), 1);
        gameConnector.handle("a", "버리기 이빨");

        clock.advance(Duration.ofMinutes(5));

        gameConnector.handle("a", "보기");
        assertThat(sender.lastTo("a", RoomMessage.class).items()).isEmpty();
        assertError("a", "줍기 이빨", "이곳에 '이빨'은 없습니다.");
    }

    @Test
    void placementRespawns() {
        GameMap map = prepareMap();
        Items items = prepareItems();
        place(map.entranceId(), items.hide(), 2, 10);
        enter("a", "한백");
        gameConnector.handle("a", "줍기 가죽 전부");
        gameConnector.handle("a", "보기");
        assertThat(sender.lastTo("a", RoomMessage.class).items()).isEmpty();

        clock.advance(Duration.ofMinutes(10));

        gameConnector.handle("a", "보기");
        assertThat(sender.lastTo("a", RoomMessage.class).items()).containsExactly(new FloorItemView("늑대 가죽", 2));
    }

    @Test
    void lookAtItem() {
        GameMap map = prepareMap();
        prepareNpcs(map);
        Items items = prepareItems();
        place(map.entranceId(), items.hide(), 3, null);
        Long hanbaek = enter("a", "한백");
        give(hanbaek, items.sword(), 1);
        give(hanbaek, items.fang(), 1);
        sender.clear();

        assertOnly("a", "보기 가죽", SystemMessage.item("늑대 가죽 — 거칠고 질긴 늑대의 가죽이다."));
        assertOnly("a", "보기 비검", SystemMessage.item("비검 — 푸른 빛이 도는 작은 비검이다."));
        assertOnly("a", "보기 이빨", SystemMessage.item("늑대 이빨"));
        assertOnly("a", "보기 왕씨", SystemMessage.info("아직 자세히 볼 수 없습니다."));
        assertOnly("a", "보기 곰", SystemMessage.error("그런 것은 보이지 않습니다."));
    }

    @Test
    void lookPrefersRoomTargets() {
        GameMap map = prepareMap();
        prepareNpcs(map);
        Items items = prepareItems();
        place(map.entranceId(), items.hide(), 1, null);
        Long hanbaek = enter("a", "한백");
        give(hanbaek, items.quest(), 1);
        give(hanbaek, items.fang(), 1);
        sender.clear();

        assertOnly("a", "보기 왕씨", SystemMessage.info("아직 자세히 볼 수 없습니다."));
        assertOnly("a", "보기 늑대", SystemMessage.item("늑대 가죽 — 거칠고 질긴 늑대의 가죽이다."));
    }

    private void assertError(String sessionId, String command, String text) {
        assertOnly(sessionId, command, SystemMessage.error(text));
    }

    private void assertOnly(String sessionId, String command, GameMessage expected) {
        sender.clear();
        gameConnector.handle(sessionId, command);
        assertThat(sender.messagesTo(sessionId)).containsExactly(expected);
    }
}
