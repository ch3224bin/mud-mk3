package mudmk3.application.game;

import lombok.RequiredArgsConstructor;
import mudmk3.application.cultivation.provided.ReadResult;
import mudmk3.application.cultivation.provided.TechniqueReader;
import mudmk3.application.floor.provided.RoomFloor;
import mudmk3.application.game.required.GameMessage;
import mudmk3.application.game.required.GameMessage.InventoryMessage;
import mudmk3.application.game.required.GameMessage.InventoryMessage.ItemView;
import mudmk3.application.game.required.GameMessage.InventoryMessage.StoneView;
import mudmk3.application.game.required.GameMessage.NoticeMessage;
import mudmk3.application.game.required.GameMessage.SystemMessage;
import mudmk3.application.game.required.PresenceRepository;
import mudmk3.application.inventory.provided.InventoryEditor;
import mudmk3.application.inventory.provided.InventoryFinder;
import mudmk3.application.npc.provided.NpcFinder;
import mudmk3.application.technique.provided.ContainedTechniqueView;
import mudmk3.application.technique.provided.TechniqueFinder;
import mudmk3.domain.combat.Corpse;
import mudmk3.domain.combat.MobInstance;
import mudmk3.domain.floor.FloorStack;
import mudmk3.domain.game.Amount;
import mudmk3.domain.game.Command;
import mudmk3.domain.game.ItemArguments;
import mudmk3.domain.game.OnlinePlayer;
import mudmk3.domain.inventory.AddCheck;
import mudmk3.domain.inventory.GiveResult;
import mudmk3.domain.inventory.Inventory;
import mudmk3.domain.inventory.InventoryEntry;
import mudmk3.domain.inventory.RemoveCheck;
import mudmk3.domain.item.ItemNames;
import mudmk3.domain.item.ItemQuery;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.item.SpiritStoneGrade;
import mudmk3.domain.shared.Josa;
import mudmk3.domain.shared.Numbers;
import mudmk3.domain.shared.Realm;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.springframework.util.Assert.state;

/**
 * 물건 명령(소지품·줍기·버리기·물건 보기·주기·읽기, D44~D47·D65)을 처리한다. {@link GameService} 가 게임 락과 트랜잭션 안에서 부른다.
 * 바꾸는 명령은 검사를 다 한 뒤에 바꾸고, 같은 방 다른 사람에게 알린다
 */
@Component
@RequiredArgsConstructor
class ItemCommandHandler {

    static final String PICK_NO_ARGUMENT = "무엇을 주우시겠습니까? (예: 줍기 늑대 가죽)";
    static final String DROP_NO_ARGUMENT = "무엇을 버리시겠습니까? (예: 버리기 늑대 가죽)";
    static final String INVALID_AMOUNT = "수량은 1 이상의 수로 적으십시오.";
    static final String NO_ROOM = "저물대에 자리가 없습니다.";
    static final String BAG_LIMIT = "저물대는 세 개까지만 찰 수 있습니다.";
    static final String NOT_HELD = "그런 물건은 가지고 있지 않습니다.";
    static final String BAG_IN_USE = "저물대에 든 물건을 먼저 비우십시오.";
    static final String BRACELET_LIMIT = "저물탁은 하나까지만 찰 수 있습니다.";
    static final String BRACELET_IN_USE = "저물탁에 든 물건을 먼저 비우십시오.";
    static final String BRACELET_CLOSED = " (" + Realm.FOUNDATION_EARLY.koreanName() + " 필요)";
    static final String EMPTY_INVENTORY = "가진 물건이 없습니다.";
    static final String NO_DETAILED_LOOK = "아직 자세히 볼 수 없습니다.";
    static final String NOTHING_TO_SEE = "그런 것은 보이지 않습니다.";
    static final String GIVE_NO_ARGUMENT = "누구에게 무엇을 주시겠습니까? (예: 주기 한립 회춘단 2)";
    static final String GIVE_SELF = "자기 자신에게는 줄 수 없습니다.";
    static final String READ_NO_ARGUMENT = "무엇을 읽으시겠습니까? (예: 읽기 장춘공 옥간)";

    private static final int ITEMS_PER_LINE = 3;

    private final RoomFloor roomFloor;
    private final CharacterRealms realms;
    private final InventoryFinder inventoryFinder;
    private final InventoryEditor inventoryEditor;
    private final NpcFinder npcFinder;
    private final PresenceRepository presenceRepository;
    private final RoomNotifier roomNotifier;
    private final TechniqueReader techniqueReader;
    private final TechniqueFinder techniqueFinder;
    private final CultivationTexts cultivationTexts;
    private final CombatEngine combatEngine;
    private final CombatTexts combatTexts;
    private final RoomRefresher roomRefresher;
    private final CorpseHandler corpseHandler;
    private final Clock clock;

    /**
     * 물건 명령(주기·읽기 포함)과 인자 있는 {@code 보기} 를 처리한다. 교전 중에는 {@code 줍기}·{@code 주기}·{@code 읽기} 가 막힌다(D86).
     * send 는 (연결 id, 메시지)로 보낼 것을 모은다
     */
    void handle(Command command, OnlinePlayer player, BiConsumer<String, GameMessage> send) {
        if ((command instanceof Command.Pick || command instanceof Command.Give || command instanceof Command.Read)
                && combatEngine.isFighting(player.getCharacterId())) {
            send.accept(player.getSessionId(), SystemMessage.error(CombatTexts.CANNOT_DO));
            return;
        }
        switch (command) {
            case Command.Inventory _ -> send.accept(player.getSessionId(), inventory(player));
            case Command.Pick pick -> pick(player, pick.args(), send);
            case Command.Drop drop -> drop(player, drop.args(), send);
            case Command.Look(String argument) when argument != null -> look(player, argument, send);
            case Command.Give give -> give(player, give, send);
            case Command.Read read -> read(player, read.item(), send);
            default -> throw new IllegalArgumentException("물건 명령이 아닙니다: " + command);
        }
    }

    // ---- 소지품

    private InventoryMessage inventory(OnlinePlayer player) {
        Inventory inventory = inventoryFinder.find(player.getCharacterId());
        boolean braceletOpen = realms.braceletOpen(player.getCharacterId());
        List<StoneView> stones = inventory.spiritStones().entrySet().stream()
                .map(stone -> new StoneView(stone.getKey().itemName(), stone.getValue()))
                .toList();
        List<ItemView> items = inventory.getEntries().stream()
                .filter(entry -> !entry.getItemType().isSpiritStone())
                .map(entry -> new ItemView(entry.getItemType().getName(), entry.getCount()))
                .toList();
        return InventoryMessage.of(inventory.usedSlots(), inventory.capacity(braceletOpen), stones, items,
                inventoryText(inventory, braceletOpen));
    }

    /**
     * D45 형식: 첫 줄 {@code [소지품 3/22칸]} 과 가진 영석({@code  하품 영석 30 · 중품 영석 1}),
     * 다음 줄부터 영석 밖 물건을 한 줄에 셋씩(사이 공백 셋). 아무것도 없으면 둘째 줄 {@code 가진 물건이 없습니다.}
     * 칸이 닫힌 저물탁 뒤에는 {@code  (축기 초기 필요)}. 칸을 넘으면 넘은 그대로({@code [소지품 130/22칸]})
     */
    String inventoryText(Inventory inventory, boolean braceletOpen) {
        StringBuilder text = new StringBuilder("[소지품 ")
                .append(inventory.usedSlots()).append('/').append(inventory.capacity(braceletOpen)).append("칸]");
        Map<SpiritStoneGrade, Long> stones = inventory.spiritStones();
        if (!stones.isEmpty()) {
            text.append("  ").append(stones.entrySet().stream()
                    .map(stone -> stone.getKey().itemName() + " " + Numbers.count(stone.getValue()))
                    .collect(Collectors.joining(" · ")));
        }
        List<String> items = inventory.getEntries().stream()
                .filter(entry -> !entry.getItemType().isSpiritStone())
                .map(entry -> inventoryItem(entry, braceletOpen))
                .toList();
        if (stones.isEmpty() && items.isEmpty()) {
            return text.append('\n').append(EMPTY_INVENTORY).toString();
        }
        for (int i = 0; i < items.size(); i += ITEMS_PER_LINE) {
            text.append('\n').append(String.join("   ", items.subList(i, Math.min(i + ITEMS_PER_LINE, items.size()))));
        }
        return text.toString();
    }

    private static String inventoryItem(InventoryEntry entry, boolean braceletOpen) {
        StringBuilder item = new StringBuilder(entry.getItemType().getName());
        if (entry.getCount() >= 2) item.append(" x").append(Numbers.count(entry.getCount()));
        if (entry.getItemType().isBracelet() && !braceletOpen) item.append(BRACELET_CLOSED);
        return item.toString();
    }

    // ---- 줍기

    /**
     * 바닥(D40 묶음)에서 찾는다. 줍는 수는 min(요청 수, 바닥 수, 넣을 수 있는 수)로 먼저 정하고
     * 그만큼 바닥에서 덜어 소지품에 넣는다(두 쪽 수가 어긋나지 않게, D53)
     */
    private void pick(OnlinePlayer player, ItemArguments args, BiConsumer<String, GameMessage> send) {
        String sessionId = player.getSessionId();
        ItemQuery query = args.item();
        if (query == null) {
            send.accept(sessionId, SystemMessage.error(PICK_NO_ARGUMENT));
            return;
        }
        if (args.amount() instanceof Amount.Invalid) {
            send.accept(sessionId, SystemMessage.error(INVALID_AMOUNT));
            return;
        }
        Long roomId = player.getRoomId();
        Optional<FloorStack> found = ItemNames.select(roomFloor.look(roomId), stack -> stack.itemType().getName(), query);
        if (found.isEmpty()) {
            String input = inputOf(query);
            send.accept(sessionId, SystemMessage.error("이곳에 '" + input + "'" + Josa.eunNeun(input) + " 없습니다."));
            return;
        }
        FloorStack stack = found.get();
        ItemType itemType = stack.itemType();
        String name = itemType.getName();
        int wanted = switch (args.amount()) {
            case Amount.Count(int value) -> value;
            case Amount.All _ -> stack.count();
            case Amount.Invalid _ -> throw new IllegalStateException("수량 오류는 위에서 거른다");
        };
        if (wanted > stack.count()) {
            send.accept(sessionId, SystemMessage.error(tooMany(name, stack.count())));
            return;
        }
        boolean braceletOpen = realms.braceletOpen(player.getCharacterId());
        Inventory inventory = inventoryFinder.find(player.getCharacterId());
        int room = inventory.roomFor(itemType, braceletOpen);
        if (room == 0) {
            send.accept(sessionId, SystemMessage.error(switch (inventory.checkAdd(itemType, 1, braceletOpen)) {
                case BAG_LIMIT -> BAG_LIMIT;
                case BRACELET_LIMIT -> BRACELET_LIMIT;
                case NO_ROOM, OK -> NO_ROOM; // 칸을 넘은 소지품은 저물대 하나로 칸이 맞아도 넣을 자리가 없다
            }));
            return;
        }

        Long itemTypeId = requireNonNull(itemType.getId());
        int count = Math.min(wanted, room);
        int taken = roomFloor.take(roomId, itemTypeId, count);
        int added = inventoryEditor.addUpTo(player.getCharacterId(), itemTypeId, taken, braceletOpen);
        state(taken == count && added == taken, () -> "바닥과 소지품의 수가 어긋납니다: " + name);

        String things = name + " " + Numbers.count(count) + "개";
        send.accept(sessionId, SystemMessage.item(count < wanted
                ? "저물대에 자리가 없어 " + name + " " + Numbers.count(wanted) + "개 중 " + Numbers.count(count) + "개만 주웠습니다."
                : Josa.withEulReul(things) + " 주웠습니다."));
        roomNotifier.notifyOthers(player, NoticeMessage.item(
                Josa.withIGa(player.getName()) + " " + Josa.withEulReul(things) + " 줍는다.", player.getName(), "pick"), send);
        roomRefresher.mark(roomId);
    }

    // ---- 버리기

    /** 소지품(D37 순서)에서 찾는다. 검사 순서: 인자 없음 → 수량 오류 → 없음 → 수 넘침 → 저물대·저물탁 칸 */
    private void drop(OnlinePlayer player, ItemArguments args, BiConsumer<String, GameMessage> send) {
        String sessionId = player.getSessionId();
        ItemQuery query = args.item();
        if (query == null) {
            send.accept(sessionId, SystemMessage.error(DROP_NO_ARGUMENT));
            return;
        }
        if (args.amount() instanceof Amount.Invalid) {
            send.accept(sessionId, SystemMessage.error(INVALID_AMOUNT));
            return;
        }
        Inventory inventory = inventoryFinder.find(player.getCharacterId());
        Optional<InventoryEntry> found = selectHeld(inventory, query);
        if (found.isEmpty()) {
            send.accept(sessionId, SystemMessage.error(NOT_HELD));
            return;
        }
        InventoryEntry entry = found.get();
        ItemType itemType = entry.getItemType();
        String name = itemType.getName();
        int count = switch (args.amount()) {
            case Amount.Count(int value) -> value;
            case Amount.All _ -> entry.getCount();
            case Amount.Invalid _ -> throw new IllegalStateException("수량 오류는 위에서 거른다");
        };
        if (count > entry.getCount()) {
            send.accept(sessionId, SystemMessage.error(tooMany(name, entry.getCount())));
            return;
        }

        Long itemTypeId = requireNonNull(itemType.getId());
        RemoveCheck check = inventoryEditor.remove(player.getCharacterId(), itemTypeId, count,
                realms.braceletOpen(player.getCharacterId()));
        if (check != RemoveCheck.OK) {
            send.accept(sessionId, SystemMessage.error(check == RemoveCheck.BRACELET_IN_USE ? BRACELET_IN_USE : BAG_IN_USE));
            return;
        }
        roomFloor.drop(player.getRoomId(), itemTypeId, count);

        String things = name + " " + Numbers.count(count) + "개";
        send.accept(sessionId, SystemMessage.item(Josa.withEulReul(things) + " 버렸습니다."));
        roomNotifier.notifyOthers(player, NoticeMessage.item(
                Josa.withIGa(player.getName()) + " " + Josa.withEulReul(things) + " 버린다.", player.getName(), "drop"), send);
        roomRefresher.mark(player.getRoomId());
    }

    // ---- 보기

    /**
     * 방의 대상(같은 방 플레이어 → NPC → 몹(방 보기 순서) → 시체 → 바닥 물건)에서 먼저 고르고, 없으면 내 소지품에서 고른다
     * (D45·D89). 물건이면 이름과 묘사, 몹이면 급·체급·묘사·기혈 상태, 시체면 남은 물건(시체 보기), 사람·NPC면 아직 자세히 볼 수 없다
     */
    private void look(OnlinePlayer player, String argument, BiConsumer<String, GameMessage> send) {
        String sessionId = player.getSessionId();
        ItemQuery query = ItemQuery.parse(ItemNames.normalize(argument));
        Long roomId = player.getRoomId();

        List<LookTarget> targets = new ArrayList<>();
        presenceRepository.findAllInRoom(roomId).stream()
                .filter(other -> !other.getCharacterId().equals(player.getCharacterId()))
                .forEach(other -> targets.add(LookTarget.other(other.getName())));
        npcFinder.findAllInRoom(roomId).forEach(npc -> targets.add(LookTarget.other(npc.getName())));
        combatEngine.mobsInRoom(roomId).forEach(mob -> targets.add(LookTarget.mob(mob)));
        corpseHandler.inRoom(roomId).forEach(corpse -> targets.add(LookTarget.corpse(corpse)));
        roomFloor.look(roomId).forEach(stack -> targets.add(LookTarget.item(stack.itemType())));

        Optional<LookTarget> inRoom = ItemNames.select(targets, LookTarget::name, query);
        if (inRoom.isPresent()) {
            LookTarget target = inRoom.get();
            ItemType itemType = target.itemType();
            MobInstance mob = target.mob();
            Corpse corpse = target.corpse();
            if (corpse != null) {
                send.accept(sessionId, corpseHandler.look(corpse, player.getCharacterId(), clock.instant()));
            } else if (mob != null) {
                Realm viewerRealm = realms.realm(player.getCharacterId());
                send.accept(sessionId, SystemMessage.info(combatTexts.lookMob(mob, viewerRealm)));
            } else {
                send.accept(sessionId, itemType == null ? SystemMessage.info(NO_DETAILED_LOOK) : describe(itemType));
            }
            return;
        }
        Optional<InventoryEntry> held = selectHeld(inventoryFinder.find(player.getCharacterId()), query);
        send.accept(sessionId, held.map(entry -> describe(entry.getItemType()))
                .orElseGet(() -> SystemMessage.error(NOTHING_TO_SEE)));
    }

    /** 이름과 묘사. 담긴 공법이 있으면 둘째 줄 {@code 담긴 공법: 장춘공 (목 · 오행 · 9성 · 범인부터)} */
    private SystemMessage describe(ItemType itemType) {
        String description = itemType.getDescription();
        String text = description.isEmpty() ? itemType.getName() : itemType.getName() + " — " + description;
        return SystemMessage.item(itemType.findTechnique()
                .map(technique -> text + "\n" + containedTechnique(techniqueFinder.viewContained(requireNonNull(technique.getId()))))
                .orElse(text));
    }

    /** {@code 담긴 공법: 장춘공 (목 · 오행 · 9성 · 범인부터)}, 연체술이면 {@code (연체술 · 9성 · 범인부터)} */
    private static String containedTechnique(ContainedTechniqueView technique) {
        String elements = technique.elementNames() == null ? "" : technique.elementNames() + " · ";
        return "담긴 공법: " + technique.name() + " (" + elements + technique.lineName() + " · "
                + technique.stageCount() + "성 · " + technique.startRealmName() + "부터)";
    }

    /** 보기 대상 하나. 물건이면 itemType, 몹이면 mob, 시체면 corpse, 자세히 볼 수 없는 것(사람·NPC)은 셋 다 null */
    private record LookTarget(String name, @Nullable ItemType itemType, @Nullable MobInstance mob,
                              @Nullable Corpse corpse) {
        static LookTarget other(String name) {
            return new LookTarget(name, null, null, null);
        }

        static LookTarget item(ItemType itemType) {
            return new LookTarget(itemType.getName(), itemType, null, null);
        }

        static LookTarget mob(MobInstance mob) {
            return new LookTarget(mob.getProfile().name(), null, mob, null);
        }

        static LookTarget corpse(Corpse corpse) {
            return new LookTarget(corpse.name(), null, null, corpse);
        }
    }

    // ---- 읽기

    /**
     * 소지품(표시 순서)에서 고른 옥간·서책 하나를 읽는다(command.md {@code ### 읽기}). 인자 없음 → 가지고 있지 않음은 여기서,
     * 나머지 검사와 익히기는 {@link TechniqueReader} 가 한다. 성공은 {@code growth}, 거절은 {@code error}. 같은 방 알림 없음
     */
    private void read(OnlinePlayer player, @Nullable ItemQuery query, BiConsumer<String, GameMessage> send) {
        String sessionId = player.getSessionId();
        if (query == null) {
            send.accept(sessionId, SystemMessage.error(READ_NO_ARGUMENT));
            return;
        }
        Optional<InventoryEntry> held = selectHeld(inventoryFinder.find(player.getCharacterId()), query);
        if (held.isEmpty()) {
            send.accept(sessionId, SystemMessage.error(NOT_HELD));
            return;
        }
        ReadResult result = techniqueReader.read(player.getCharacterId(), requireNonNull(held.get().getItemType().getId()));
        send.accept(sessionId, result instanceof ReadResult.Learned learned
                ? SystemMessage.growth(cultivationTexts.readSuccess(learned))
                : SystemMessage.error(cultivationTexts.readFailure(result)));
    }

    // ---- 주기

    /**
     * 같은 방 플레이어에게 준다(D44). 검사 순서: 인자 없음 → 수량 오류 → 대상 없음 → 자기 자신 → 없음 → 수 넘침
     * → 주는 쪽 저물대·저물탁 칸 → 받는 쪽 저물대 세 개·저물탁 하나 → 받는 쪽 칸
     */
    private void give(OnlinePlayer player, Command.Give give, BiConsumer<String, GameMessage> send) {
        String sessionId = player.getSessionId();
        String target = give.target();
        ItemArguments args = ItemArguments.parse(give.args());
        ItemQuery query = args.item();
        if (target == null || target.isBlank() || query == null) {
            send.accept(sessionId, SystemMessage.error(GIVE_NO_ARGUMENT));
            return;
        }
        if (args.amount() instanceof Amount.Invalid) {
            send.accept(sessionId, SystemMessage.error(INVALID_AMOUNT));
            return;
        }
        Optional<OnlinePlayer> found = findReceiver(player, target);
        if (found.isEmpty()) {
            send.accept(sessionId, SystemMessage.error("여기에 " + Josa.withEunNeun(target) + " 없습니다."));
            return;
        }
        OnlinePlayer receiver = found.get();
        if (receiver.getCharacterId().equals(player.getCharacterId())) {
            send.accept(sessionId, SystemMessage.error(GIVE_SELF));
            return;
        }
        Optional<InventoryEntry> held = selectHeld(inventoryFinder.find(player.getCharacterId()), query);
        if (held.isEmpty()) {
            send.accept(sessionId, SystemMessage.error(NOT_HELD));
            return;
        }
        InventoryEntry entry = held.get();
        ItemType itemType = entry.getItemType();
        String name = itemType.getName();
        int count = switch (args.amount()) {
            case Amount.Count(int value) -> value;
            case Amount.All _ -> entry.getCount();
            case Amount.Invalid _ -> throw new IllegalStateException("수량 오류는 위에서 거른다");
        };
        if (count > entry.getCount()) {
            send.accept(sessionId, SystemMessage.error(tooMany(name, entry.getCount())));
            return;
        }

        String receiverName = receiver.getName();
        GiveResult result = inventoryEditor.give(player.getCharacterId(), receiver.getCharacterId(),
                requireNonNull(itemType.getId()), count,
                realms.braceletOpen(player.getCharacterId()), realms.braceletOpen(receiver.getCharacterId()));
        switch (result) {
            case GIVER_BAG_IN_USE -> send.accept(sessionId, SystemMessage.error(BAG_IN_USE));
            case GIVER_BRACELET_IN_USE -> send.accept(sessionId, SystemMessage.error(BRACELET_IN_USE));
            case RECEIVER_BAG_LIMIT -> send.accept(sessionId,
                    SystemMessage.error(Josa.withEunNeun(receiverName) + " 저물대를 더 찰 수 없습니다."));
            case RECEIVER_BRACELET_LIMIT -> send.accept(sessionId,
                    SystemMessage.error(Josa.withEunNeun(receiverName) + " 저물탁을 더 찰 수 없습니다."));
            case RECEIVER_NO_ROOM -> send.accept(sessionId, SystemMessage.error(receiverName + "의 저물대에 자리가 없습니다."));
            case GIVEN -> {
                String things = Josa.withEulReul(name + " " + Numbers.count(count) + "개");
                String giver = Josa.withIGa(player.getName());
                send.accept(sessionId, SystemMessage.item(receiverName + "에게 " + things + " 주었습니다."));
                send.accept(receiver.getSessionId(), SystemMessage.item(giver + " " + things + " 건넸습니다."));
                NoticeMessage notice = NoticeMessage.item(giver + " " + receiverName + "에게 " + things + " 건넨다.",
                        player.getName(), "give");
                presenceRepository.findAllInRoom(player.getRoomId()).stream()
                        .filter(other -> !other.getCharacterId().equals(player.getCharacterId())
                                && !other.getCharacterId().equals(receiver.getCharacterId()))
                        .forEach(other -> send.accept(other.getSessionId(), notice));
            }
        }
    }

    /**
     * 같은 방에 접속해 있는 플레이어(들어온 순서)에서 받는 사람을 찾는다. 이름 전체가 같은 사람(나 포함), 없으면 이름이 입력으로
     * 시작하는 첫 다른 사람, 그것도 없고 내 이름이 입력으로 시작하면 나
     */
    private Optional<OnlinePlayer> findReceiver(OnlinePlayer player, String target) {
        List<OnlinePlayer> inRoom = presenceRepository.findAllInRoom(player.getRoomId());
        String input = target.toLowerCase(Locale.ROOT);
        return inRoom.stream().filter(other -> other.getName().equalsIgnoreCase(target)).findFirst()
                .or(() -> inRoom.stream()
                        .filter(other -> !other.getCharacterId().equals(player.getCharacterId()))
                        .filter(other -> other.getName().toLowerCase(Locale.ROOT).startsWith(input))
                        .findFirst())
                .or(() -> player.getName().toLowerCase(Locale.ROOT).startsWith(input) ? Optional.of(player) : Optional.empty());
    }

    // ---- 공통

    /** 소지품(표시 순서)에서 이름으로 고른다(D46 — 버리기·주기·팔기·값). 종류마다 묶음이 하나뿐이다 */
    static Optional<InventoryEntry> selectHeld(Inventory inventory, ItemQuery query) {
        return ItemNames.select(inventory.getEntries(), entry -> entry.getItemType().getName(), query);
    }

    /** {@code 늑대 가죽은 3개뿐입니다.} */
    static String tooMany(String name, int held) {
        return Josa.withEunNeun(name) + " " + Numbers.count(held) + "개뿐입니다.";
    }

    /** 입력한 이름(번호 포함). 영석 등급 낱말은 전체 이름으로 바뀐 뒤다 */
    static String inputOf(ItemQuery query) {
        return query.hasOrdinal() ? query.ordinal() + "." + query.name() : query.name();
    }
}
