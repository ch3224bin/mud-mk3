package mudmk3.application.game;

import mudmk3.application.character.provided.CharacterCreator;
import mudmk3.application.character.provided.CharacterFinder;
import mudmk3.application.character.provided.CharacterMover;
import mudmk3.application.game.provided.GameConnector;
import mudmk3.application.game.required.GameCloseReason;
import mudmk3.application.game.required.GameMessage;
import mudmk3.application.game.required.GameMessage.NoticeMessage;
import mudmk3.application.game.required.GameMessage.PromptMessage;
import mudmk3.application.game.required.GameMessage.SayMessage;
import mudmk3.application.game.required.GameMessage.SessionMessage;
import mudmk3.application.game.required.GameMessage.SystemMessage;
import mudmk3.application.game.required.GameMessageSender;
import mudmk3.application.game.required.PresenceRepository;
import mudmk3.application.room.provided.RoomDeletedEvent;
import mudmk3.application.room.provided.RoomFinder;
import mudmk3.application.world.provided.WorldSettingsFinder;
import mudmk3.domain.character.CharacterName;
import mudmk3.domain.character.PlayerCharacter;
import mudmk3.domain.game.Command;
import mudmk3.domain.game.CommandParser;
import mudmk3.domain.game.OnlinePlayer;
import mudmk3.domain.room.Direction;
import mudmk3.domain.room.Exit;
import mudmk3.domain.room.Room;
import mudmk3.domain.shared.Josa;
import mudmk3.support.stereotype.ApplicationService;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * 게임 접속과 명령 처리. 서버 전체의 게임 처리(접속·입력·끊김·방 삭제 반영)를 공정한 락 하나로 직렬화한다.
 * 트랜잭션은 락 안에서 열고 닫는다(커밋이 끝난 뒤에 다음 처리가 DB를 읽게 — 예: 같은 이름 동시 생성).
 * 그래서 포트 메서드 자체는 트랜잭션을 열지 않고({@code SUPPORTS}) 락을 잡은 뒤 {@link TransactionTemplate} 으로 연다.
 */
@ApplicationService
public class GameService implements GameConnector {

    static final int MAX_COMMAND_LENGTH = 500;
    static final int MAX_SAY_LENGTH = 200;

    static final String WORLD_NOT_READY = "아직 세계가 열리지 않았습니다. 관리자가 시작 방을 정해야 합니다.";
    static final String NAME_PROMPT = "이름을 정하십시오. (한글 2~6자)";
    static final String INVALID_NAME = "이름은 띄어쓰기 없이 한글 2~6자로 정하십시오.";
    static final String DUPLICATE_NAME = "이미 있는 이름입니다. 다른 이름을 정하십시오.";
    static final String CONFIRM = "확인";
    static final String BACK = "뒤로";
    static final String CHARACTER_CREATED = "캐릭터를 만들었습니다.";
    static final String REPLACED = "다른 곳에서 접속해 이 연결을 끊습니다.";
    static final String COMMAND_TOO_LONG = "명령이 너무 깁니다.";
    static final String NO_DETAILED_LOOK = "아직 자세히 볼 수 없습니다.";
    static final String NO_EXIT = "그쪽으로는 길이 없습니다.";
    static final String SAY_EMPTY = "무엇이라 말하시겠습니까?";
    static final String SAY_TOO_LONG = "말은 200자 이내로 하십시오.";
    static final String HELP =
            "명령: 보기(ㅂ) · 동 서 남 북 북동 북서 남동 남서 위 아래 (e w s n ne nw se sw u d) · 말하기(') · 누구 · 도움말 · 종료";
    static final String QUIT = "다음에 또 뵙겠습니다.";
    static final String UNKNOWN = "무슨 말인지 모르겠습니다. ('도움말' 로 명령을 볼 수 있습니다)";
    static final String RELOCATED = "서 있던 곳이 사라져 시작 장소로 옮겨졌습니다.";

    private final CharacterFinder characterFinder;
    private final CharacterCreator characterCreator;
    private final CharacterMover characterMover;
    private final RoomFinder roomFinder;
    private final WorldSettingsFinder worldSettingsFinder;
    private final PresenceRepository presenceRepository;
    private final GameMessageSender sender;
    private final TransactionTemplate transactionTemplate;

    private final ReentrantLock lock = new ReentrantLock(true);
    /** 연결 id → 게임 상태. {@link #lock} 안에서만 다룬다 */
    private final Map<String, GameSession> sessions = new HashMap<>();
    /** 이번 처리에서 보낼 것. {@link #lock} 안에서만 다룬다 */
    private final List<Runnable> outbox = new ArrayList<>();

    public GameService(
            CharacterFinder characterFinder,
            CharacterCreator characterCreator,
            CharacterMover characterMover,
            RoomFinder roomFinder,
            WorldSettingsFinder worldSettingsFinder,
            PresenceRepository presenceRepository,
            GameMessageSender sender,
            PlatformTransactionManager transactionManager
    ) {
        this.characterFinder = characterFinder;
        this.characterCreator = characterCreator;
        this.characterMover = characterMover;
        this.roomFinder = roomFinder;
        this.worldSettingsFinder = worldSettingsFinder;
        this.presenceRepository = presenceRepository;
        this.sender = sender;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public void connect(String sessionId, Long accountId) {
        serialized(() -> doConnect(sessionId, accountId));
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public void handle(String sessionId, String text) {
        serialized(() -> doHandle(sessionId, text));
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public void disconnect(String sessionId) {
        serialized(() -> doDisconnect(sessionId));
    }

    /**
     * 방을 지운 트랜잭션이 커밋된 뒤 그 방의 캐릭터를 시작 방으로 옮긴다(D11).
     * 관리자 트랜잭션 안에서는 게임 락을 잡지 않는다(DB 행 락과 게임 락이 엇갈려 교착하지 않게).
     * 커밋과 이 반영 사이에 들어온 게임 처리는 사라진 방을 스스로 바로잡는다({@link #relocateIfRoomGone}).
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onRoomDeleted(RoomDeletedEvent event) {
        serialized(() -> relocateFrom(event.roomId()));
    }

    /**
     * 락을 잡고 그 안에서 트랜잭션을 열고 닫는다. 보낼 메시지는 모아 두었다가 커밋이 끝난 뒤(락 안) 보낸다.
     * 커밋이 실패하면 보내지 않는다(메모리 상태는 되돌리지 않는다 — 1차 한계).
     */
    private void serialized(Runnable work) {
        lock.lock();
        try {
            outbox.clear();
            transactionTemplate.executeWithoutResult(status -> work.run());
            List<Runnable> deliveries = List.copyOf(outbox);
            outbox.clear();
            deliveries.forEach(Runnable::run);
        } finally {
            // 실패한 처리가 남긴 outbox 는 다음 처리가 시작할 때 비운다
            lock.unlock();
        }
    }

    private void send(String sessionId, GameMessage message) {
        outbox.add(() -> sender.send(sessionId, message));
    }

    private void close(String sessionId, GameCloseReason reason) {
        outbox.add(() -> sender.close(sessionId, reason));
    }

    // ---- 접속·끊김

    private void doConnect(String sessionId, Long accountId) {
        Optional<Room> startRoom = worldSettingsFinder.findStartRoom();
        if (startRoom.isEmpty()) {
            send(sessionId, SystemMessage.error(WORLD_NOT_READY));
            close(sessionId, GameCloseReason.WORLD_NOT_READY);
            return;
        }
        replaceOtherSessionsOf(accountId);

        Optional<PlayerCharacter> character = characterFinder.findByAccountId(accountId);
        if (character.isEmpty()) {
            sessions.put(sessionId, GameSession.naming(accountId));
            send(sessionId, new SessionMessage(SessionMessage.CREATING, null));
            send(sessionId, new PromptMessage(NAME_PROMPT));
            return;
        }

        PlayerCharacter found = character.get();
        Long characterId = requireNonNull(found.getId());
        GameSession session = GameSession.playing(accountId, characterId);
        sessions.put(sessionId, session);

        Optional<OnlinePlayer> online = presenceRepository.findByCharacterId(characterId);
        if (online.isPresent()) {
            // 앞 연결에서 이어받는다: 세계에 그대로 있으므로 나타난다/사라진다 알림이 없다
            OnlinePlayer player = online.get();
            player.rebind(sessionId);
            presenceRepository.save(player);
            send(sessionId, new SessionMessage(SessionMessage.PLAYING, player.getName()));
            if (!relocateIfRoomGone(player.getRoomId())) {
                sendRoom(sessionId, session, roomFinder.find(player.getRoomId()));
            }
            return;
        }

        Room room = roomFinder.findOptional(found.getRoomId()).orElseGet(() -> {
            // 저장된 방이 지워졌으면 시작 방에서 시작한다
            characterMover.moveTo(characterId, requireNonNull(startRoom.get().getId()));
            return startRoom.get();
        });
        send(sessionId, new SessionMessage(SessionMessage.PLAYING, found.getName().value()));
        enterWorld(sessionId, session, accountId, found.getName().value(), room);
    }

    /** 같은 계정의 앞 연결을 닫는다. 캐릭터는 세계에 그대로 둔다(새 연결이 이어받는다) */
    private void replaceOtherSessionsOf(Long accountId) {
        List<String> previous = sessions.entrySet().stream()
                .filter(entry -> entry.getValue().getAccountId().equals(accountId))
                .map(Map.Entry::getKey)
                .toList();
        for (String previousSessionId : previous) {
            sessions.remove(previousSessionId);
            send(previousSessionId, SystemMessage.info(REPLACED));
            close(previousSessionId, GameCloseReason.REPLACED);
        }
    }

    private void enterWorld(String sessionId, GameSession session, Long accountId, String name, Room room) {
        Long roomId = requireNonNull(room.getId());
        OnlinePlayer player = OnlinePlayer.enter(session.requireCharacterId(), accountId, name, roomId, sessionId);
        presenceRepository.save(player);
        notifyOthers(player, roomId, Josa.withIGa(name) + " 나타난다.", "appear");
        sendRoom(sessionId, session, room);
    }

    private void doDisconnect(String sessionId) {
        GameSession session = sessions.remove(sessionId);
        if (session == null || !session.isPlaying()) return;
        // 밀려난 연결은 이미 sessions 에서 빠졌고, presence 도 새 연결 id 를 가리킨다
        presenceRepository.findBySessionId(sessionId).ifPresent(this::leaveWorld);
    }

    private void leaveWorld(OnlinePlayer player) {
        presenceRepository.delete(player.getCharacterId());
        notifyOthers(player, player.getRoomId(), Josa.withIGa(player.getName()) + " 사라진다.", "disappear");
    }

    // ---- 입력

    private void doHandle(String sessionId, String text) {
        GameSession session = sessions.get(sessionId);
        if (session == null) return;
        if (text.length() > MAX_COMMAND_LENGTH) {
            send(sessionId, SystemMessage.error(COMMAND_TOO_LONG));
            return;
        }
        switch (session.getPhase()) {
            case NAMING -> handleName(sessionId, session, text.strip());
            case CONFIRMING -> handleConfirm(sessionId, session, text.strip());
            case PLAYING -> handleCommand(sessionId, session, CommandParser.parse(text));
        }
    }

    // ---- 캐릭터 생성

    private void handleName(String sessionId, GameSession session, String name) {
        if (!CharacterName.isValid(name)) {
            askNameAgain(sessionId, session, INVALID_NAME);
            return;
        }
        if (characterFinder.isNameTaken(name)) {
            askNameAgain(sessionId, session, DUPLICATE_NAME);
            return;
        }
        session.confirmName(name);
        send(sessionId, new PromptMessage(confirmPrompt(name)));
    }

    private void handleConfirm(String sessionId, GameSession session, String answer) {
        switch (answer) {
            case CONFIRM -> createCharacter(sessionId, session);
            case BACK -> {
                session.askName();
                send(sessionId, new PromptMessage(NAME_PROMPT));
            }
            default -> send(sessionId, new PromptMessage(confirmPrompt(session.requirePendingName())));
        }
    }

    private void createCharacter(String sessionId, GameSession session) {
        String name = session.requirePendingName();
        // 생성 포트가 예외를 던지면 함께 쓰는 트랜잭션이 롤백 전용이 되므로 먼저 확인한다
        if (characterFinder.isNameTaken(name)) {
            askNameAgain(sessionId, session, DUPLICATE_NAME);
            return;
        }
        Optional<Room> startRoom = worldSettingsFinder.findStartRoom();
        if (startRoom.isEmpty()) {
            sessions.remove(sessionId);
            send(sessionId, SystemMessage.error(WORLD_NOT_READY));
            close(sessionId, GameCloseReason.WORLD_NOT_READY);
            return;
        }
        PlayerCharacter character = characterCreator.create(session.getAccountId(), name);
        session.startPlaying(requireNonNull(character.getId()));
        send(sessionId, SystemMessage.info(CHARACTER_CREATED));
        send(sessionId, new SessionMessage(SessionMessage.PLAYING, name));
        enterWorld(sessionId, session, session.getAccountId(), name, startRoom.get());
    }

    private static String confirmPrompt(String name) {
        return "이름 " + name + "\n이대로 만들려면 '확인', 고치려면 '뒤로'.";
    }

    private void askNameAgain(String sessionId, GameSession session, String error) {
        session.askName();
        send(sessionId, SystemMessage.error(error));
        send(sessionId, new PromptMessage(NAME_PROMPT));
    }

    // ---- 명령

    private void handleCommand(String sessionId, GameSession session, Command command) {
        OnlinePlayer player = presenceRepository.findByCharacterId(session.requireCharacterId())
                .orElseThrow(() -> new IllegalStateException("세계에 없는 캐릭터입니다. id: " + session.getCharacterId()));
        boolean needsRoom = !(command instanceof Command.Empty || command instanceof Command.Quit);
        if (needsRoom && relocateIfRoomGone(player.getRoomId())
                && command instanceof Command.Look(var argument) && argument == null) {
            return; // 옮겨지며 시작 방 보기를 이미 받았다
        }
        switch (command) {
            case Command.Look look -> look(sessionId, player, look);
            case Command.Move move -> move(sessionId, session, player, move.direction());
            case Command.Say say -> say(sessionId, player, say.message());
            case Command.Who _ -> who(sessionId);
            case Command.Help _ -> send(sessionId, SystemMessage.info(HELP));
            case Command.Quit _ -> quit(sessionId, player);
            case Command.Empty _ -> {
            }
            case Command.Unknown _ -> send(sessionId, SystemMessage.error(UNKNOWN));
        }
    }

    private void look(String sessionId, OnlinePlayer player, Command.Look look) {
        if (look.argument() != null) {
            send(sessionId, SystemMessage.info(NO_DETAILED_LOOK));
            return;
        }
        Room room = roomFinder.find(player.getRoomId());
        send(sessionId, RoomViewAssembler.assemble(
                room, false, presenceRepository.findAllInRoom(player.getRoomId()), player.getCharacterId()));
    }

    private void move(String sessionId, GameSession session, OnlinePlayer player, Direction direction) {
        Room from = roomFinder.find(player.getRoomId());
        Optional<Exit> exit = from.findExit(direction);
        if (exit.isEmpty()) {
            send(sessionId, SystemMessage.error(NO_EXIT));
            return;
        }
        Long fromId = player.getRoomId();
        Long targetId = requireNonNull(exit.get().getTarget().getId());
        String actor = Josa.withIGa(player.getName());

        characterMover.moveTo(player.getCharacterId(), targetId);
        notifyOthers(player, fromId, actor + " " + direction.towardPhrase() + " 떠난다.", "leave");
        player.moveTo(targetId);
        presenceRepository.save(player);
        notifyOthers(player, targetId, actor + " " + direction.opposite().fromPhrase() + " 다가온다.", "arrive");
        sendRoom(sessionId, session, roomFinder.find(targetId));
    }

    private void say(String sessionId, OnlinePlayer player, String message) {
        if (message.isEmpty()) {
            send(sessionId, SystemMessage.error(SAY_EMPTY));
            return;
        }
        if (message.length() > MAX_SAY_LENGTH) {
            send(sessionId, SystemMessage.error(SAY_TOO_LONG));
            return;
        }
        String name = player.getName();
        send(sessionId, SayMessage.speech(name, message, true, name + ": \"" + message + "\""));
        SayMessage heard = SayMessage.speech(name, message, false, Josa.withIGa(name) + " 말한다: \"" + message + "\"");
        othersInRoom(player, player.getRoomId()).forEach(other -> send(other.getSessionId(), heard));
    }

    private void who(String sessionId) {
        presenceRepository.findAll().stream()
                .map(OnlinePlayer::getRoomId)
                .distinct()
                .toList()
                .forEach(this::relocateIfRoomGone);
        List<OnlinePlayer> players = presenceRepository.findAll();
        StringBuilder text = new StringBuilder("접속한 사람 (" + players.size() + ")");
        for (OnlinePlayer player : players) {
            String zoneName = roomFinder.find(player.getRoomId()).getZone().getName();
            text.append("\n  ").append(player.getName()).append("    ").append(zoneName);
        }
        send(sessionId, SystemMessage.info(text.toString()));
    }

    private void quit(String sessionId, OnlinePlayer player) {
        send(sessionId, SystemMessage.info(QUIT));
        leaveWorld(player);
        close(sessionId, GameCloseReason.QUIT);
        sessions.remove(sessionId);
    }

    // ---- 방 삭제 반영

    /**
     * 그 방이 이미 지워졌으면(삭제 커밋 뒤 {@link #onRoomDeleted} 반영 전) 그 방 사람을 지금 시작 방으로 옮기고 true.
     * 뒤늦게 오는 반영은 옮길 사람이 없어 아무 일도 하지 않는다.
     */
    private boolean relocateIfRoomGone(Long roomId) {
        if (roomFinder.findOptional(roomId).isPresent()) return false;
        relocateFrom(roomId);
        return true;
    }

    private void relocateFrom(Long deletedRoomId) {
        // 시작 방은 지울 수 없고, 시작 방이 없으면 아무도 세계에 들어올 수 없으므로 옮길 사람이 없다
        Optional<Room> startRoom = worldSettingsFinder.findStartRoom();
        if (startRoom.isEmpty()) return;
        Room start = startRoom.get();
        Long startId = requireNonNull(start.getId());

        characterMover.relocateAll(deletedRoomId, startId);

        List<OnlinePlayer> stranded = presenceRepository.findAllInRoom(deletedRoomId);
        stranded.forEach(player -> {
            player.moveTo(startId);
            presenceRepository.save(player);
        });
        Set<Long> strandedIds = stranded.stream().map(OnlinePlayer::getCharacterId).collect(Collectors.toSet());
        for (OnlinePlayer player : stranded) {
            String sessionId = player.getSessionId();
            send(sessionId, SystemMessage.info(RELOCATED));
            GameSession session = sessions.get(sessionId);
            if (session != null) {
                sendRoom(sessionId, session, start);
            }
            NoticeMessage appear = NoticeMessage.movement(Josa.withIGa(player.getName()) + " 나타난다.", player.getName(), "appear");
            presenceRepository.findAllInRoom(startId).stream()
                    .filter(other -> !strandedIds.contains(other.getCharacterId()))
                    .forEach(other -> send(other.getSessionId(), appear));
        }
    }

    // ---- 보내기

    /** 방 보기를 보낸다. 앞서 본 지역과 다르면 지역에 들어선 것으로 표시한다 */
    private void sendRoom(String sessionId, GameSession session, Room room) {
        boolean zoneEntered = session.enterZone(requireNonNull(room.getZone().getId()));
        Long roomId = requireNonNull(room.getId());
        send(sessionId, RoomViewAssembler.assemble(
                room, zoneEntered, presenceRepository.findAllInRoom(roomId), session.requireCharacterId()));
    }

    private void notifyOthers(OnlinePlayer actor, Long roomId, String text, String event) {
        GameMessage notice = NoticeMessage.movement(text, actor.getName(), event);
        othersInRoom(actor, roomId).forEach(other -> send(other.getSessionId(), notice));
    }

    private List<OnlinePlayer> othersInRoom(OnlinePlayer self, Long roomId) {
        return presenceRepository.findAllInRoom(roomId).stream()
                .filter(other -> !other.getCharacterId().equals(self.getCharacterId()))
                .toList();
    }
}
