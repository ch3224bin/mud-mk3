package mudmk3.application.game;

import mudmk3.application.character.provided.CharacterCreator;
import mudmk3.application.character.provided.CharacterFinder;
import mudmk3.application.character.provided.CharacterMover;
import mudmk3.application.character.provided.CharacterRemover;
import mudmk3.application.character.provided.DeletedCharacter;
import mudmk3.application.character.provided.RealmChangedEvent;
import mudmk3.application.cultivation.provided.CultivationFinder;
import mudmk3.application.floor.provided.RoomFloor;
import mudmk3.application.game.provided.CharacterOnlineException;
import mudmk3.application.game.provided.GameConnector;
import mudmk3.application.game.provided.GameTicker;
import mudmk3.application.game.provided.OfflineCharacterRemover;
import mudmk3.application.game.required.GameCloseReason;
import mudmk3.application.game.required.GameMessage;
import mudmk3.application.game.required.GameMessage.NoticeMessage;
import mudmk3.application.game.required.GameMessage.PromptMessage;
import mudmk3.application.game.required.GameMessage.SayMessage;
import mudmk3.application.game.required.GameMessage.SessionMessage;
import mudmk3.application.game.required.GameMessage.StatusMessage;
import mudmk3.application.game.required.GameMessage.SystemMessage;
import mudmk3.application.game.required.GameMessage.TalkMessage;
import mudmk3.application.game.required.GameMessageSender;
import mudmk3.application.game.required.PresenceRepository;
import mudmk3.application.npc.provided.NpcFinder;
import mudmk3.application.room.provided.RoomDeletedEvent;
import mudmk3.application.room.provided.RoomFinder;
import mudmk3.application.world.provided.WorldSettingsFinder;
import mudmk3.domain.character.CharacterName;
import mudmk3.domain.character.PlayerCharacter;
import mudmk3.domain.character.SpiritRoot;
import mudmk3.domain.cultivation.Insight;
import mudmk3.domain.cultivation.StatSheet;
import mudmk3.domain.game.Command;
import mudmk3.domain.game.CommandParser;
import mudmk3.domain.game.OnlinePlayer;
import mudmk3.domain.npc.Npc;
import mudmk3.domain.npc.NpcTopic;
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

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * 게임 접속과 명령 처리, 서버 틱. 서버 전체의 게임 처리(접속·입력·끊김·방 삭제 반영·틱)를 공정한 락 하나로 직렬화한다.
 * 트랜잭션은 락 안에서 열고 닫는다(커밋이 끝난 뒤에 다음 처리가 DB를 읽게 — 예: 같은 이름 동시 생성).
 * 그래서 포트 메서드 자체는 트랜잭션을 열지 않고({@code SUPPORTS}) 락을 잡은 뒤 {@link TransactionTemplate} 으로 연다.
 */
@ApplicationService
public class GameService implements GameConnector, GameTicker, OfflineCharacterRemover {

    static final int MAX_COMMAND_LENGTH = 500;
    static final int MAX_SAY_LENGTH = 200;

    static final String WORLD_NOT_READY = "아직 세계가 열리지 않았습니다. 관리자가 시작 방을 정해야 합니다.";
    static final String NAME_PROMPT = "이름을 정하십시오. (한글 2~6자)";
    static final String INVALID_NAME = "이름은 띄어쓰기 없이 한글 2~6자로 정하십시오.";
    static final String DUPLICATE_NAME = "이미 있는 이름입니다. 다른 이름을 정하십시오.";
    static final String CONFIRM = "확인";
    static final String BACK = "뒤로";
    static final String CHARACTER_CREATED = "캐릭터를 만들었습니다.";
    static final String ROOT_PROMPT = "영근을 고르십시오. 오행(금 목 수 화 토) 가운데 1~5개를 띄어 쓰십시오.";
    static final String INVALID_ROOT = "오행(금 목 수 화 토) 가운데 1~5개를 겹치지 않게 띄어 쓰십시오.";
    static final String ROOT_MISSING = "영근을 아직 정하지 않았습니다. 영근을 고르면 세계에 들어갑니다.";
    static final String ROOT_CHOSEN = "영근을 정했습니다.";
    static final String CONFIRM_CREATE = "이대로 만들려면 '확인', 고치려면 '뒤로'.";
    static final String CONFIRM_ROOT = "이대로 정하려면 '확인', 고치려면 '뒤로'.";
    static final String REPLACED = "다른 곳에서 접속해 이 연결을 끊습니다.";
    static final String COMMAND_TOO_LONG = "명령이 너무 깁니다.";
    static final String NO_EXIT = "그쪽으로는 길이 없습니다.";
    static final String SAY_EMPTY = "무엇이라 말하시겠습니까?";
    static final String SAY_TOO_LONG = "말은 200자 이내로 하십시오.";
    static final String HELP =
            "명령: 보기(봐 보) · 동 서 남 북 북동 북서 남동 남서 위 아래 (ㄷ ㅅ ㄴ ㅂ ㅂㄷ ㅂㅅ ㄴㄷ ㄴㅅ ㅇ ㅇㄹ / e w s n ne nw se sw u d)"
                    + " · 보기 <방위> · 말하기(') · 대화 <NPC> [낱말]"
                    + " · 공격 <대상> · <신통> [대상] · 중지 · 수습 <시체> · 상태(ㅅㅌ) · 공법 · 신통 · 읽기 <물건>"
                    + " · 소지품(ㅈ) · 보기 <물건> · 줍기 <물건> [수량] · 버리기 <물건> [수량] · 주기 <대상> <물건> [수량]"
                    + " · 목록 · 사기 <물건> [수량] · 팔기 <물건> [수량] · 값 <물건>"
                    + " · 확인 · 취소 · 누구 · 도움말 · 종료";
    static final String TALK_NO_TARGET = "누구에게 말을 거시겠습니까?";
    static final String TALK_NO_SUCH_NPC = "여기에 그런 상대는 없습니다.";
    static final String TALK_WORDS = "물어볼 수 있는 낱말:";
    static final String QUIT = "다음에 또 뵙겠습니다.";
    static final String UNKNOWN = "무슨 말인지 모르겠습니다. ('도움말' 로 명령을 볼 수 있습니다)";
    static final String RELOCATED = "서 있던 곳이 사라져 시작 장소로 옮겨졌습니다.";
    static final String CHARACTER_ONLINE = "접속 중인 캐릭터는 지울 수 없습니다. 접속을 끊은 뒤 다시 시도하십시오: ";

    private final CharacterFinder characterFinder;
    private final CharacterCreator characterCreator;
    private final CharacterMover characterMover;
    private final CharacterRemover characterRemover;
    private final RoomFinder roomFinder;
    private final NpcFinder npcFinder;
    private final WorldSettingsFinder worldSettingsFinder;
    private final PresenceRepository presenceRepository;
    private final RoomFloor roomFloor;
    private final ItemCommandHandler itemCommandHandler;
    private final ShopCommandHandler shopCommandHandler;
    private final CultivationFinder cultivationFinder;
    private final CultivationTexts cultivationTexts;
    private final CombatEngine combatEngine;
    private final CombatCommandHandler combatCommandHandler;
    private final AbilityCommandHandler abilityCommandHandler;
    private final RoomNotifier roomNotifier;
    private final RoomRefresher roomRefresher;
    private final Clock clock;
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
            CharacterRemover characterRemover,
            RoomFinder roomFinder,
            NpcFinder npcFinder,
            WorldSettingsFinder worldSettingsFinder,
            PresenceRepository presenceRepository,
            RoomFloor roomFloor,
            ItemCommandHandler itemCommandHandler,
            ShopCommandHandler shopCommandHandler,
            CultivationFinder cultivationFinder,
            CultivationTexts cultivationTexts,
            CombatEngine combatEngine,
            CombatCommandHandler combatCommandHandler,
            AbilityCommandHandler abilityCommandHandler,
            RoomNotifier roomNotifier,
            RoomRefresher roomRefresher,
            Clock clock,
            GameMessageSender sender,
            PlatformTransactionManager transactionManager
    ) {
        this.characterFinder = characterFinder;
        this.characterCreator = characterCreator;
        this.characterMover = characterMover;
        this.characterRemover = characterRemover;
        this.roomFinder = roomFinder;
        this.npcFinder = npcFinder;
        this.worldSettingsFinder = worldSettingsFinder;
        this.presenceRepository = presenceRepository;
        this.roomFloor = roomFloor;
        this.itemCommandHandler = itemCommandHandler;
        this.shopCommandHandler = shopCommandHandler;
        this.cultivationFinder = cultivationFinder;
        this.cultivationTexts = cultivationTexts;
        this.combatEngine = combatEngine;
        this.combatCommandHandler = combatCommandHandler;
        this.abilityCommandHandler = abilityCommandHandler;
        this.roomNotifier = roomNotifier;
        this.roomRefresher = roomRefresher;
        this.clock = clock;
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
     * 서버 틱 하나(D85). 명령처럼 락 안에서 트랜잭션 하나로 처리하고, 보낼 것은 커밋 뒤 보낸다.
     * 이번 틱에 진 캐릭터({@link CombatOutcome})는 진 순서대로 시작 방에서 깨어난다(D88)
     */
    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public void tick() {
        serialized(() -> combatEngine.tick(clock.instant(), this::send).defeatedCharacterIds()
                .forEach(this::wakeAtStart));
    }

    /**
     * 관리 툴의 캐릭터 삭제. 접속 판정과 지우기를 명령·틱·접속과 같은 게임 락 안에서 트랜잭션 하나로 한다(판정과 지우기 사이에
     * 그 계정이 세계에 들어오지 못함). 그 계정의 게임 연결이 있으면(영근을 고르는 중 포함) 거절한다. DB 행을 지운 뒤 메모리의
     * 교전·대기·몹 기록을 지운다. 예외는 트랜잭션을 되돌리고 그대로 나간다
     */
    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public DeletedCharacter delete(Long characterId) {
        return serialized(() -> {
            PlayerCharacter character = characterFinder.find(characterId);
            Long accountId = character.getAccountId();
            boolean connected = sessions.values().stream().anyMatch(session -> session.getAccountId().equals(accountId));
            if (connected || presenceRepository.findByCharacterId(characterId).isPresent()) {
                throw new CharacterOnlineException(CHARACTER_ONLINE + character.getName().value());
            }
            DeletedCharacter deleted = characterRemover.delete(characterId);
            combatEngine.forget(characterId, clock.instant());
            return deleted;
        });
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
     * 관리 툴의 경지 지정이 커밋된 뒤, 접속 중인 그 캐릭터에게만 {@code growth} 한 줄을 보낸다(D69, 같은 방 알림 없음).
     * {@link #onRoomDeleted} 처럼 관리자 트랜잭션 밖에서 게임 락을 잡는다. 경지는 메모리에 두지 않으므로 반영 전에 온 명령도
     * DB에서 새 경지를 읽는다
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onRealmChanged(RealmChangedEvent event) {
        serialized(() -> presenceRepository.findByCharacterId(event.characterId()).ifPresent(player ->
                send(player.getSessionId(), SystemMessage.growth(cultivationTexts.realmChanged(event.from(), event.to())))));
    }

    /**
     * 락을 잡고 그 안에서 트랜잭션을 열고 닫는다. 보낼 메시지는 모아 두었다가 커밋이 끝난 뒤(락 안) 보낸다.
     * 커밋이 실패하면 보내지 않는다(메모리 상태는 되돌리지 않는다 — 1차 한계).
     * 처리 끝(같은 트랜잭션 안)에 이번 처리가 표시한 방마다 조용한 방 상태를 한 번 보낸다({@link #sendQuietRooms}).
     */
    private void serialized(Runnable work) {
        serialized(() -> {
            work.run();
            return Boolean.TRUE;
        });
    }

    /** {@link #serialized(Runnable)} 와 같고 처리의 결과를 커밋 뒤 돌려준다 */
    private <T> T serialized(Supplier<T> work) {
        lock.lock();
        try {
            outbox.clear();
            roomRefresher.clear();
            T result = requireNonNull(transactionTemplate.execute(status -> {
                T value = work.get();
                sendQuietRooms();
                return value;
            }));
            List<Runnable> deliveries = List.copyOf(outbox);
            outbox.clear();
            deliveries.forEach(Runnable::run);
            return result;
        } finally {
            // 실패한 처리가 남긴 outbox·방 표시는 다음 처리가 시작할 때 비운다
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
        if (found.findSpiritRoot().isEmpty()) {
            // 4차 전에 만든 캐릭터: 세계에 들어가기 전에 영근을 고른다
            String name = found.getName().value();
            sessions.put(sessionId, GameSession.rootingExisting(accountId, characterId, name));
            send(sessionId, new SessionMessage(SessionMessage.CREATING, name));
            send(sessionId, SystemMessage.info(ROOT_MISSING));
            send(sessionId, new PromptMessage(ROOT_PROMPT));
            return;
        }
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

        send(sessionId, new SessionMessage(SessionMessage.PLAYING, found.getName().value()));
        enterWorld(sessionId, session, accountId, found.getName().value(), savedRoomOf(found, startRoom.get()));
    }

    /** 캐릭터의 저장된 방. 지워졌으면 시작 방으로 옮기고 시작 방 */
    private Room savedRoomOf(PlayerCharacter character, Room startRoom) {
        return roomFinder.findOptional(character.getRoomId()).orElseGet(() -> {
            characterMover.moveTo(requireNonNull(character.getId()), requireNonNull(startRoom.getId()));
            return startRoom;
        });
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
        roomNotifier.notifyOthers(player, roomId, NoticeMessage.movement(Josa.withIGa(name) + " 나타난다.", name, "appear"),
                this::send);
        sendRoom(sessionId, session, room);
        combatEngine.entered(player.getCharacterId(), clock.instant());
    }

    private void doDisconnect(String sessionId) {
        GameSession session = sessions.remove(sessionId);
        if (session == null || !session.isPlaying()) return;
        // 밀려난 연결은 이미 sessions 에서 빠졌고, presence 도 새 연결 id 를 가리킨다
        presenceRepository.findBySessionId(sessionId).ifPresent(this::leaveWorld);
    }

    /** 세계에서 빠진다. 교전 중이었으면 교전이 바로 끝난다(5차에 30초 남는 몸은 없음) */
    private void leaveWorld(OnlinePlayer player) {
        combatEngine.leave(player.getCharacterId(), clock.instant());
        presenceRepository.delete(player.getCharacterId());
        roomNotifier.notifyOthers(player, NoticeMessage.movement(Josa.withIGa(player.getName()) + " 사라진다.",
                player.getName(), "disappear"), this::send);
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
            case ROOTING, ROOTING_EXISTING -> handleRoot(sessionId, session, text.strip());
            case CONFIRMING, CONFIRMING_EXISTING -> handleConfirm(sessionId, session, text.strip());
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
        session.rooting(name);
        send(sessionId, new PromptMessage(ROOT_PROMPT));
    }

    /** 영근 입력. '뒤로' 는 새 캐릭터면 이름 묻기로, 기존 캐릭터면 같은 영근 묻기로 */
    private void handleRoot(String sessionId, GameSession session, String input) {
        if (input.equals(BACK)) {
            if (!session.isExisting()) {
                session.askName();
                send(sessionId, new PromptMessage(NAME_PROMPT));
                return;
            }
            send(sessionId, new PromptMessage(ROOT_PROMPT));
            return;
        }
        Optional<SpiritRoot> root = SpiritRoot.parse(input);
        if (root.isEmpty()) {
            send(sessionId, SystemMessage.error(INVALID_ROOT));
            send(sessionId, new PromptMessage(ROOT_PROMPT));
            return;
        }
        session.confirmRoot(root.get());
        send(sessionId, new PromptMessage(confirmPrompt(session)));
    }

    private void handleConfirm(String sessionId, GameSession session, String answer) {
        switch (answer) {
            case CONFIRM -> {
                if (session.isExisting()) {
                    chooseRoot(sessionId, session);
                } else {
                    createCharacter(sessionId, session);
                }
            }
            case BACK -> {
                session.askRoot();
                send(sessionId, new PromptMessage(ROOT_PROMPT));
            }
            default -> send(sessionId, new PromptMessage(confirmPrompt(session)));
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
        PlayerCharacter character = characterCreator.create(session.getAccountId(), name, session.requirePendingRoot());
        session.startPlaying(requireNonNull(character.getId()));
        send(sessionId, SystemMessage.info(CHARACTER_CREATED));
        send(sessionId, new SessionMessage(SessionMessage.PLAYING, name));
        enterWorld(sessionId, session, session.getAccountId(), name, startRoom.get());
    }

    /** 영근이 없는 기존 캐릭터의 영근을 정하고 저장된 방으로 세계에 들어간다(보통 접속과 같다) */
    private void chooseRoot(String sessionId, GameSession session) {
        Optional<Room> startRoom = worldSettingsFinder.findStartRoom();
        if (startRoom.isEmpty()) {
            sessions.remove(sessionId);
            send(sessionId, SystemMessage.error(WORLD_NOT_READY));
            close(sessionId, GameCloseReason.WORLD_NOT_READY);
            return;
        }
        Long characterId = session.requireCharacterId();
        characterCreator.chooseSpiritRoot(characterId, session.requirePendingRoot());
        String name = session.requirePendingName();
        session.startPlaying(characterId);
        send(sessionId, SystemMessage.info(ROOT_CHOSEN));
        send(sessionId, new SessionMessage(SessionMessage.PLAYING, name));
        Room room = savedRoomOf(characterFinder.find(characterId), startRoom.get());
        enterWorld(sessionId, session, session.getAccountId(), name, room);
    }

    /**
     * 영근 확인 prompt 네 줄: {@code 목·화 진영근 — 목 50, 화 50} / 구분선 / {@code 이름 한백 · 영근 목·화 진영근 (목 50, 화 50)} /
     * 새 캐릭터면 만들기, 기존 캐릭터면 정하기 안내
     */
    private static String confirmPrompt(GameSession session) {
        SpiritRoot root = session.requirePendingRoot();
        return root.name() + " — " + root.summary()
                + "\n────────"
                + "\n이름 " + session.requirePendingName() + " · 영근 " + root.name() + " (" + root.summary() + ")"
                + "\n" + (session.isExisting() ? CONFIRM_ROOT : CONFIRM_CREATE);
    }

    private void askNameAgain(String sessionId, GameSession session, String error) {
        session.askName();
        send(sessionId, SystemMessage.error(error));
        send(sessionId, new PromptMessage(NAME_PROMPT));
    }

    // ---- 명령

    private void handleCommand(String sessionId, GameSession session, Command command) {
        // 확인 대기는 확인·취소가 아닌 명령이 오면 조용히 사라진다(빈 입력은 명령이 아니다)
        if (!(command instanceof Command.Confirm || command instanceof Command.Cancel || command instanceof Command.Empty)) {
            session.clearPending();
        }
        OnlinePlayer player = presenceRepository.findByCharacterId(session.requireCharacterId())
                .orElseThrow(() -> new IllegalStateException("세계에 없는 캐릭터입니다. id: " + session.getCharacterId()));
        boolean needsRoom = !(command instanceof Command.Empty || command instanceof Command.Quit);
        if (needsRoom && relocateIfRoomGone(player.getRoomId())
                && command instanceof Command.Look(var argument) && argument == null) {
            return; // 옮겨지며 시작 방 보기를 이미 받았다
        }
        switch (command) {
            case Command.Look look -> look(sessionId, player, look);
            case Command.LookToward lookToward -> lookToward(sessionId, player, lookToward.direction());
            case Command.Move move -> move(sessionId, session, player, move.direction());
            case Command.Say say -> say(sessionId, player, say.message());
            case Command.Talk talk -> talk(sessionId, player, talk);
            case Command.Inventory _, Command.Pick _, Command.Drop _, Command.Give _, Command.Read _ ->
                    itemCommandHandler.handle(command, player, this::send);
            case Command.ShopList _, Command.Buy _, Command.Sell _, Command.Appraise _, Command.Confirm _, Command.Cancel _ ->
                    shopCommandHandler.handle(command, player, session, this::send);
            case Command.Attack _, Command.Collect _ -> combatCommandHandler.handle(command, player, this::send);
            case Command.Status _ -> status(sessionId, player);
            case Command.Techniques _ -> send(sessionId, SystemMessage.info(cultivationTexts.techniques(
                    characterFinder.find(player.getCharacterId()).getRealm(),
                    cultivationFinder.findLearned(player.getCharacterId()),
                    cultivationFinder.insight(player.getCharacterId()))));
            case Command.Abilities _ -> send(sessionId, SystemMessage.info(cultivationTexts.abilities(
                    cultivationFinder.findLearned(player.getCharacterId()),
                    abilityCommandHandler.timers(player.getCharacterId()).orElse(null), clock.instant())));
            case Command.Who _ -> who(sessionId);
            case Command.Help _ -> send(sessionId, SystemMessage.info(HELP));
            case Command.Quit _ -> quit(sessionId, player);
            case Command.Empty _ -> {
            }
            case Command.UseAbility useAbility -> abilityCommandHandler.use(player, useAbility, clock.instant(), this::send);
            case Command.Stop _ -> abilityCommandHandler.stop(player, this::send);
            case Command.Unknown _ -> send(sessionId, SystemMessage.error(UNKNOWN));
        }
    }

    /** 경지·스탯은 메모리에 두지 않고 명령마다 DB와 공법 정의로 계산한다 */
    private void status(String sessionId, OnlinePlayer player) {
        PlayerCharacter character = characterFinder.find(player.getCharacterId());
        StatSheet stats = cultivationFinder.stats(player.getCharacterId());
        Insight insight = cultivationFinder.insight(player.getCharacterId());
        String text = cultivationTexts.status(character, stats, insight);
        String combatLine = combatEngine.statusLine(player.getCharacterId());
        if (!combatLine.isEmpty()) {
            text = text + "\n" + combatLine;
        }
        send(sessionId, StatusMessage.of(character, stats, insight, text));
    }

    private void look(String sessionId, OnlinePlayer player, Command.Look look) {
        if (look.argument() != null) {
            itemCommandHandler.handle(look, player, this::send);
            return;
        }
        send(sessionId, roomView(roomFinder.find(player.getRoomId()), false, player.getCharacterId(), false));
    }

    /** 그 방위 출구 너머 방의 이름만 보인다: {@code 동쪽으로 숲 입구가 보인다.} 교전·시전 중에도 된다 */
    private void lookToward(String sessionId, OnlinePlayer player, Direction direction) {
        Optional<Exit> exit = roomFinder.find(player.getRoomId()).findExit(direction);
        if (exit.isEmpty()) {
            send(sessionId, SystemMessage.error(NO_EXIT));
            return;
        }
        String target = exit.get().getTarget().getName();
        send(sessionId, SystemMessage.info(direction.towardPhrase() + " " + Josa.withIGa(target) + " 보인다."));
    }

    /** 교전 중이면 5차 문구가 먼저, 아니고 시전 중이면 시전 문구로 막는다(D113) */
    private void move(String sessionId, GameSession session, OnlinePlayer player, Direction direction) {
        if (combatEngine.isFighting(player.getCharacterId())) {
            send(sessionId, SystemMessage.error(CombatTexts.CANNOT_MOVE));
            return;
        }
        if (abilityCommandHandler.isCasting(player.getCharacterId())) {
            send(sessionId, SystemMessage.error(AbilityTexts.CASTING));
            return;
        }
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
        roomNotifier.notifyOthers(player, fromId, NoticeMessage.movement(
                actor + " " + direction.towardPhrase() + " 떠난다.", player.getName(), "leave"), this::send);
        player.moveTo(targetId);
        presenceRepository.save(player);
        roomNotifier.notifyOthers(player, targetId, NoticeMessage.movement(
                actor + " " + direction.opposite().fromPhrase() + " 다가온다.", player.getName(), "arrive"), this::send);
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
        roomNotifier.notifyOthers(player, heard, this::send);
    }

    /** 같은 방 NPC에게 말을 건다. 이름 전체가 같은 NPC, 없으면 이름이 입력으로 시작하는 첫 NPC. 묻는 사람에게만 보낸다 */
    private void talk(String sessionId, OnlinePlayer player, Command.Talk talk) {
        if (combatEngine.isFighting(player.getCharacterId())) {
            send(sessionId, SystemMessage.error(CombatTexts.CANNOT_DO));
            return;
        }
        String target = talk.target();
        if (target == null || target.isBlank()) {
            send(sessionId, SystemMessage.error(TALK_NO_TARGET));
            return;
        }
        List<Npc> npcs = npcFinder.findAllInRoom(player.getRoomId());
        Optional<Npc> found = npcs.stream().filter(npc -> npc.isCalled(target)).findFirst()
                .or(() -> npcs.stream().filter(npc -> npc.nameStartsWith(target)).findFirst());
        if (found.isEmpty()) {
            send(sessionId, SystemMessage.error(TALK_NO_SUCH_NPC));
            return;
        }
        Npc npc = found.get();
        String word = talk.word();
        String text = word == null ? greetingOf(npc) : npc.getName() + ": \"" + npc.answerTo(word) + "\"";
        send(sessionId, TalkMessage.talk(npc.getName(), word, text));
    }

    /** 인사말과, 낱말이 있으면 다음 줄에 물어볼 수 있는 낱말(입력 순서) */
    private static String greetingOf(Npc npc) {
        String greeting = npc.getName() + ": \"" + npc.getGreeting() + "\"";
        List<NpcTopic> topics = npc.getTopics();
        if (topics.isEmpty()) return greeting;
        return greeting + "\n" + TALK_WORDS + " " + topics.stream()
                .map(topic -> "[" + topic.word() + "]")
                .collect(Collectors.joining(" "));
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
        if (combatEngine.isFighting(player.getCharacterId())) {
            send(sessionId, SystemMessage.error(CombatTexts.CANNOT_QUIT));
            return;
        }
        send(sessionId, SystemMessage.info(QUIT));
        leaveWorld(player);
        close(sessionId, GameCloseReason.QUIT);
        sessions.remove(sessionId);
    }

    // ---- 질 때

    /**
     * 진 캐릭터가 깨어난다(D88·D96): 시작 방이 지금 방과 다르면 옮기고 시작 방 사람에게 {@code 한백이 나타난다.} →
     * {@code 마을 어귀에서 깨어났습니다. (기혈 1/60)}(tone {@code combat}) → 지금 방 보기. 시작 방이 없으면 그 방에서 깨어난다.
     * 옮기면 옛 방을 표시한다(그 방 사람에게 조용한 방 상태). 세션이 없으면 옮기기만 한다
     */
    private void wakeAtStart(Long characterId) {
        Optional<OnlinePlayer> found = presenceRepository.findByCharacterId(characterId);
        if (found.isEmpty()) return;
        OnlinePlayer player = found.get();
        Room room = worldSettingsFinder.findStartRoom().orElseGet(() -> roomFinder.find(player.getRoomId()));
        Long roomId = requireNonNull(room.getId());
        if (!roomId.equals(player.getRoomId())) {
            roomRefresher.mark(player.getRoomId());
            characterMover.moveTo(characterId, roomId);
            player.moveTo(roomId);
            presenceRepository.save(player);
            roomNotifier.notifyOthers(player, roomId, NoticeMessage.movement(Josa.withIGa(player.getName()) + " 나타난다.",
                    player.getName(), "appear"), this::send);
        }
        String sessionId = player.getSessionId();
        GameSession session = sessions.get(sessionId);
        if (session == null) return;
        int maxQiBlood = cultivationFinder.stats(characterId).qiBlood();
        send(sessionId, SystemMessage.combat(CombatTexts.wokeUp(room.getName(), maxQiBlood)));
        sendRoom(sessionId, session, room);
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

    /** 그 방 사람을 시작 방으로 옮기고, 그 방의 시체·몹을 메모리에서 지운다 */
    private void relocateFrom(Long deletedRoomId) {
        // 시작 방은 지울 수 없고, 시작 방이 없으면 아무도 세계에 들어올 수 없으므로 옮길 사람이 없다
        worldSettingsFinder.findStartRoom().ifPresent(start -> relocatePlayers(deletedRoomId, start));
        combatEngine.forgetRoom(deletedRoomId);
    }

    private void relocatePlayers(Long deletedRoomId, Room start) {
        Long startId = requireNonNull(start.getId());

        characterMover.relocateAll(deletedRoomId, startId);

        List<OnlinePlayer> stranded = presenceRepository.findAllInRoom(deletedRoomId);
        stranded.forEach(player -> {
            combatEngine.endAll(player.getCharacterId(), clock.instant());
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
        send(sessionId, roomView(room, zoneEntered, session.requireCharacterId(), false));
    }

    /**
     * 이번 처리에서 표시한 방(표시한 순서)마다, 방이 아직 있으면 그 방 사람 각자에게 그 사람 기준 조용한 방 상태
     * ({@code zoneEntered} false, {@code quiet} true)를 보낸다. 이번 처리의 다른 메시지 뒤에 간다
     */
    private void sendQuietRooms() {
        for (Long roomId : roomRefresher.drain()) {
            Optional<Room> room = roomFinder.findOptional(roomId);
            if (room.isEmpty()) continue;
            for (OnlinePlayer player : presenceRepository.findAllInRoom(roomId)) {
                send(player.getSessionId(), roomView(room.get(), false, player.getCharacterId(), true));
            }
        }
    }

    /** 방 보기(quiet 면 조용한 방 상태): 그 방의 사람(나 빼고)·배치된 NPC·몹·시체·바닥 물건을 지금 읽어 넣는다 */
    private GameMessage roomView(Room room, boolean zoneEntered, Long viewerCharacterId, boolean quiet) {
        Long roomId = requireNonNull(room.getId());
        return RoomViewAssembler.assemble(room, zoneEntered, presenceRepository.findAllInRoom(roomId),
                npcFinder.findAllInRoom(roomId), roomFloor.look(roomId), viewerCharacterId,
                combatEngine.mobsInRoom(roomId), combatEngine.corpsesInRoom(roomId),
                characterFinder.find(viewerCharacterId).getRealm(), clock.instant(), quiet);
    }
}
