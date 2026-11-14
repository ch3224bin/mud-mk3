package mudmk3.application.game;

import lombok.RequiredArgsConstructor;
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
import mudmk3.domain.floor.FloorStack;
import mudmk3.domain.game.Amount;
import mudmk3.domain.game.Command;
import mudmk3.domain.game.ItemArguments;
import mudmk3.domain.game.OnlinePlayer;
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
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

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
 * 물건 명령(소지품·줍기·버리기·물건 보기·주기·귀속, D43~D47)을 처리한다. {@link GameService} 가 게임 락과 트랜잭션 안에서 부른다.
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
    static final String QUEST_DROP = "퀘스트 물건은 버릴 수 없습니다.";
    static final String BOUND_DROP = "귀속된 물건은 버릴 수 없습니다.";
    static final String BAG_IN_USE = "저물대에 든 물건을 먼저 비우십시오.";
    static final String EMPTY_INVENTORY = "가진 물건이 없습니다.";
    static final String NO_DETAILED_LOOK = "아직 자세히 볼 수 없습니다.";
    static final String NOTHING_TO_SEE = "그런 것은 보이지 않습니다.";
    static final String GIVE_NO_ARGUMENT = "누구에게 무엇을 주시겠습니까? (예: 주기 한립 회춘단 2)";
    static final String GIVE_SELF = "자기 자신에게는 줄 수 없습니다.";
    static final String QUEST_GIVE = "퀘스트 물건은 남에게 줄 수 없습니다.";
    static final String BOUND_GIVE = "귀속된 물건은 남에게 줄 수 없습니다.";
    static final String BIND_NO_ARGUMENT = "무엇을 귀속하시겠습니까? (예: 귀속 비검)";
    static final String UNBIND_NO_ARGUMENT = "무엇의 귀속을 푸시겠습니까? (예: 귀속 해제 비검)";
    static final String ALREADY_BOUND = "이미 귀속된 물건입니다.";
    static final String NOT_BOUND = "귀속된 물건이 아닙니다.";
    static final String QUEST_UNBIND = "퀘스트 물건과 본명법보는 이렇게 풀 수 없습니다.";

    private static final int ITEMS_PER_LINE = 3;

    private final RoomFloor roomFloor;
    private final InventoryFinder inventoryFinder;
    private final InventoryEditor inventoryEditor;
    private final NpcFinder npcFinder;
    private final PresenceRepository presenceRepository;

    /** 물건 명령(주기·귀속 포함)과 인자 있는 {@code 보기} 를 처리한다. send 는 (연결 id, 메시지)로 보낼 것을 모은다 */
    void handle(Command command, OnlinePlayer player, BiConsumer<String, GameMessage> send) {
        switch (command) {
            case Command.Inventory _ -> send.accept(player.getSessionId(), inventory(player));
            case Command.Pick pick -> pick(player, pick.args(), send);
            case Command.Drop drop -> drop(player, drop.args(), send);
            case Command.Look(String argument) when argument != null -> look(player, argument, send);
            case Command.Give give -> give(player, give, send);
            case Command.Bind bind -> bind(player, bind.args(), send);
            case Command.Unbind unbind -> unbind(player, unbind.args(), send);
            default -> throw new IllegalArgumentException("물건 명령이 아닙니다: " + command);
        }
    }

    // ---- 소지품

    private InventoryMessage inventory(OnlinePlayer player) {
        Inventory inventory = inventoryFinder.find(player.getCharacterId());
        List<StoneView> stones = inventory.spiritStones().entrySet().stream()
                .map(stone -> new StoneView(stone.getKey().itemName(), stone.getValue()))
                .toList();
        List<ItemView> items = inventory.getEntries().stream()
                .filter(entry -> !entry.getItemType().isSpiritStone())
                .map(entry -> new ItemView(entry.getItemType().getName(), entry.getCount(), entry.isBound(),
                        entry.getItemType().isQuest()))
                .toList();
        return InventoryMessage.of(inventory.usedSlots(), inventory.capacity(), stones, items, inventoryText(inventory));
    }

    /**
     * D45 형식: 첫 줄 {@code [소지품 3/22칸]} 과 가진 영석({@code  하품 영석 30 · 중품 영석 1}),
     * 다음 줄부터 영석 밖 물건을 한 줄에 셋씩(사이 공백 셋). 아무것도 없으면 둘째 줄 {@code 가진 물건이 없습니다.}
     */
    String inventoryText(Inventory inventory) {
        StringBuilder text = new StringBuilder("[소지품 ")
                .append(inventory.usedSlots()).append('/').append(inventory.capacity()).append("칸]");
        Map<SpiritStoneGrade, Long> stones = inventory.spiritStones();
        if (!stones.isEmpty()) {
            text.append("  ").append(stones.entrySet().stream()
                    .map(stone -> stone.getKey().itemName() + " " + Numbers.count(stone.getValue()))
                    .collect(Collectors.joining(" · ")));
        }
        List<String> items = inventory.getEntries().stream()
                .filter(entry -> !entry.getItemType().isSpiritStone())
                .map(ItemCommandHandler::inventoryItem)
                .toList();
        if (stones.isEmpty() && items.isEmpty()) {
            return text.append('\n').append(EMPTY_INVENTORY).toString();
        }
        for (int i = 0; i < items.size(); i += ITEMS_PER_LINE) {
            text.append('\n').append(String.join("   ", items.subList(i, Math.min(i + ITEMS_PER_LINE, items.size()))));
        }
        return text.toString();
    }

    private static String inventoryItem(InventoryEntry entry) {
        StringBuilder item = new StringBuilder(entry.getItemType().getName());
        if (entry.getCount() >= 2) item.append(" x").append(Numbers.count(entry.getCount()));
        if (entry.isBound()) item.append(" (귀속)");
        if (entry.getItemType().isQuest()) item.append(" (퀘스트)");
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
        int room = inventoryFinder.find(player.getCharacterId()).roomFor(itemType);
        if (room == 0) {
            send.accept(sessionId, SystemMessage.error(itemType.isBag() ? BAG_LIMIT : NO_ROOM));
            return;
        }

        Long itemTypeId = requireNonNull(itemType.getId());
        int count = Math.min(wanted, room);
        int taken = roomFloor.take(roomId, itemTypeId, count);
        int added = inventoryEditor.addUpTo(player.getCharacterId(), itemTypeId, taken);
        state(taken == count && added == taken, () -> "바닥과 소지품의 수가 어긋납니다: " + name);

        String things = name + " " + Numbers.count(count) + "개";
        send.accept(sessionId, SystemMessage.item(count < wanted
                ? "저물대에 자리가 없어 " + name + " " + Numbers.count(wanted) + "개 중 " + Numbers.count(count) + "개만 주웠습니다."
                : Josa.withEulReul(things) + " 주웠습니다."));
        notifyOthers(player, Josa.withIGa(player.getName()) + " " + Josa.withEulReul(things) + " 줍는다.", "pick", send);
    }

    // ---- 버리기

    /** 소지품(D37 순서)에서 찾는다. 검사 순서: 인자 없음 → 수량 오류 → 없음 → 퀘스트 → 귀속 → 수 넘침 → 저물대 칸 */
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
        if (itemType.isQuest()) {
            send.accept(sessionId, SystemMessage.error(QUEST_DROP));
            return;
        }
        if (entry.isBound()) {
            send.accept(sessionId, SystemMessage.error(BOUND_DROP));
            return;
        }
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
        if (inventoryEditor.remove(player.getCharacterId(), itemTypeId, false, count) == RemoveCheck.BAG_IN_USE) {
            send.accept(sessionId, SystemMessage.error(BAG_IN_USE));
            return;
        }
        roomFloor.drop(player.getRoomId(), itemTypeId, count);

        String things = name + " " + Numbers.count(count) + "개";
        send.accept(sessionId, SystemMessage.item(Josa.withEulReul(things) + " 버렸습니다."));
        notifyOthers(player, Josa.withIGa(player.getName()) + " " + Josa.withEulReul(things) + " 버린다.", "drop", send);
    }

    // ---- 보기

    /**
     * 방의 대상(같은 방 플레이어 → NPC → 바닥 물건)에서 먼저 고르고, 없으면 내 소지품에서 고른다(D45).
     * 물건이면 이름과 묘사, 사람·NPC면 아직 자세히 볼 수 없다
     */
    private void look(OnlinePlayer player, String argument, BiConsumer<String, GameMessage> send) {
        String sessionId = player.getSessionId();
        ItemQuery query = ItemQuery.parse(ItemNames.normalize(argument));
        Long roomId = player.getRoomId();

        List<LookTarget> targets = new ArrayList<>();
        presenceRepository.findAllInRoom(roomId).stream()
                .filter(other -> !other.getCharacterId().equals(player.getCharacterId()))
                .forEach(other -> targets.add(new LookTarget(other.getName(), null)));
        npcFinder.findAllInRoom(roomId).forEach(npc -> targets.add(new LookTarget(npc.getName(), null)));
        roomFloor.look(roomId).forEach(stack -> targets.add(new LookTarget(stack.itemType().getName(), stack.itemType())));

        Optional<LookTarget> inRoom = ItemNames.select(targets, LookTarget::name, query);
        if (inRoom.isPresent()) {
            ItemType itemType = inRoom.get().itemType();
            send.accept(sessionId, itemType == null ? SystemMessage.info(NO_DETAILED_LOOK) : describe(itemType));
            return;
        }
        Optional<InventoryEntry> held = selectHeld(inventoryFinder.find(player.getCharacterId()), query);
        send.accept(sessionId, held.map(entry -> describe(entry.getItemType()))
                .orElseGet(() -> SystemMessage.error(NOTHING_TO_SEE)));
    }

    private static SystemMessage describe(ItemType itemType) {
        String description = itemType.getDescription();
        return SystemMessage.item(description.isEmpty() ? itemType.getName() : itemType.getName() + " — " + description);
    }

    /** 보기 대상 하나. 물건이 아니면(사람·NPC) itemType 이 null */
    private record LookTarget(String name, @Nullable ItemType itemType) {
    }

    // ---- 주기

    /**
     * 같은 방 플레이어에게 준다(D44). 검사 순서: 인자 없음 → 수량 오류 → 대상 없음 → 자기 자신 → 없음 → 퀘스트 → 귀속
     * → 거래 불가 → 수 넘침 → 주는 쪽 저물대 칸 → 받는 쪽 저물대 세 개 → 받는 쪽 칸
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
        if (itemType.isQuest()) {
            send.accept(sessionId, SystemMessage.error(QUEST_GIVE));
            return;
        }
        if (entry.isBound()) {
            send.accept(sessionId, SystemMessage.error(BOUND_GIVE));
            return;
        }
        if (!itemType.isTradable()) {
            send.accept(sessionId, SystemMessage.error(Josa.withEunNeun(name) + " 남에게 줄 수 없는 물건입니다."));
            return;
        }
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
                requireNonNull(itemType.getId()), false, count);
        switch (result) {
            case GIVER_BAG_IN_USE -> send.accept(sessionId, SystemMessage.error(BAG_IN_USE));
            case RECEIVER_BAG_LIMIT -> send.accept(sessionId,
                    SystemMessage.error(Josa.withEunNeun(receiverName) + " 저물대를 더 찰 수 없습니다."));
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

    // ---- 귀속

    /** 직접 귀속 종류의 귀속 안 된 것 하나를 귀속한다(D43, 3차는 정혈을 쓰지 않는다) */
    private void bind(OnlinePlayer player, String argument, BiConsumer<String, GameMessage> send) {
        String sessionId = player.getSessionId();
        String name = ItemNames.normalize(argument);
        if (name.isEmpty()) {
            send.accept(sessionId, SystemMessage.error(BIND_NO_ARGUMENT));
            return;
        }
        Optional<InventoryEntry> found = selectPreferring(inventoryFinder.find(player.getCharacterId()),
                ItemQuery.parse(name), false);
        if (found.isEmpty()) {
            send.accept(sessionId, SystemMessage.error(NOT_HELD));
            return;
        }
        InventoryEntry entry = found.get();
        ItemType itemType = entry.getItemType();
        String itemName = itemType.getName();
        if (!itemType.isBindable()) {
            send.accept(sessionId, SystemMessage.error(Josa.withEunNeun(itemName) + " 귀속할 수 없습니다."));
            return;
        }
        if (entry.isBound()) {
            send.accept(sessionId, SystemMessage.error(ALREADY_BOUND));
            return;
        }
        inventoryEditor.bind(player.getCharacterId(), requireNonNull(itemType.getId()));
        send.accept(sessionId, SystemMessage.item(itemName + "에 정혈을 한 방울 떨어뜨린다. "
                + Josa.withIGa(itemName) + " 당신에게 귀속되었습니다."));
    }

    /** 귀속된 것 하나의 귀속을 푼다. 퀘스트 물건은 풀 수 없다 */
    private void unbind(OnlinePlayer player, String argument, BiConsumer<String, GameMessage> send) {
        String sessionId = player.getSessionId();
        String name = ItemNames.normalize(argument);
        if (name.isEmpty()) {
            send.accept(sessionId, SystemMessage.error(UNBIND_NO_ARGUMENT));
            return;
        }
        Optional<InventoryEntry> found = selectPreferring(inventoryFinder.find(player.getCharacterId()),
                ItemQuery.parse(name), true);
        if (found.isEmpty()) {
            send.accept(sessionId, SystemMessage.error(NOT_HELD));
            return;
        }
        InventoryEntry entry = found.get();
        ItemType itemType = entry.getItemType();
        if (itemType.isQuest()) {
            send.accept(sessionId, SystemMessage.error(QUEST_UNBIND));
            return;
        }
        if (!entry.isBound()) {
            send.accept(sessionId, SystemMessage.error(NOT_BOUND));
            return;
        }
        inventoryEditor.unbind(player.getCharacterId(), requireNonNull(itemType.getId()));
        send.accept(sessionId, SystemMessage.item(Josa.withGwaWa(itemType.getName())
                + " 이어진 정혈의 끈을 끊었습니다. 귀속이 풀렸습니다."));
    }

    // ---- 공통

    /**
     * 소지품(표시 순서)에서 이름으로 고른다. 번호 없이 고른 묶음이 귀속이고 같은 종류의 귀속 안 된 묶음이 있으면
     * 귀속 안 된 묶음을 쓴다(D46 — 버리기·주기·팔기·값)
     */
    static Optional<InventoryEntry> selectHeld(Inventory inventory, ItemQuery query) {
        return selectPreferring(inventory, query, false);
    }

    /**
     * 소지품(표시 순서)에서 이름으로 고르되, 번호 없이 고른 묶음의 귀속 여부가 {@code bound} 와 다르고 같은 종류의
     * {@code bound} 묶음이 있으면 그 묶음을 쓴다(귀속은 귀속 안 된 것을, 귀속 해제는 귀속된 것을 고른다)
     */
    private static Optional<InventoryEntry> selectPreferring(Inventory inventory, ItemQuery query, boolean bound) {
        List<InventoryEntry> entries = inventory.getEntries();
        return ItemNames.select(entries, entry -> entry.getItemType().getName(), query)
                .map(selected -> selected.isBound() == bound || query.hasOrdinal() ? selected : entries.stream()
                        .filter(entry -> entry.isBound() == bound && entry.getItemType().equals(selected.getItemType()))
                        .findFirst()
                        .orElse(selected));
    }

    /** {@code 늑대 가죽은 3개뿐입니다.} */
    static String tooMany(String name, int held) {
        return Josa.withEunNeun(name) + " " + Numbers.count(held) + "개뿐입니다.";
    }

    /** 입력한 이름(번호 포함). 영석 등급 낱말은 전체 이름으로 바뀐 뒤다 */
    private static String inputOf(ItemQuery query) {
        return query.hasOrdinal() ? query.ordinal() + "." + query.name() : query.name();
    }

    private void notifyOthers(OnlinePlayer actor, String text, String event, BiConsumer<String, GameMessage> send) {
        NoticeMessage notice = NoticeMessage.item(text, actor.getName(), event);
        presenceRepository.findAllInRoom(actor.getRoomId()).stream()
                .filter(other -> !other.getCharacterId().equals(actor.getCharacterId()))
                .forEach(other -> send.accept(other.getSessionId(), notice));
    }
}
