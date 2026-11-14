package mudmk3.application.game;

import lombok.RequiredArgsConstructor;
import mudmk3.application.game.GameSession.PendingPurchase;
import mudmk3.application.game.required.GameMessage;
import mudmk3.application.game.required.GameMessage.SystemMessage;
import mudmk3.application.inventory.provided.InventoryEditor;
import mudmk3.application.inventory.provided.InventoryFinder;
import mudmk3.application.item.provided.ItemTypeFinder;
import mudmk3.application.npc.provided.NpcFinder;
import mudmk3.domain.game.Amount;
import mudmk3.domain.game.Command;
import mudmk3.domain.game.ItemArguments;
import mudmk3.domain.game.OnlinePlayer;
import mudmk3.domain.inventory.InventoryEntry;
import mudmk3.domain.inventory.PurchaseResult;
import mudmk3.domain.inventory.SaleResult;
import mudmk3.domain.item.ItemCategory;
import mudmk3.domain.item.ItemNames;
import mudmk3.domain.item.ItemQuery;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.item.Pricing;
import mudmk3.domain.item.SpiritStoneGrade;
import mudmk3.domain.npc.Npc;
import mudmk3.domain.npc.ShopItem;
import mudmk3.domain.npc.ShopKind;
import mudmk3.domain.shared.Josa;
import mudmk3.domain.shared.Numbers;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static mudmk3.application.game.ItemCommandHandler.BAG_IN_USE;
import static mudmk3.application.game.ItemCommandHandler.BAG_LIMIT;
import static mudmk3.application.game.ItemCommandHandler.INVALID_AMOUNT;
import static mudmk3.application.game.ItemCommandHandler.NOT_HELD;
import static mudmk3.application.game.ItemCommandHandler.NO_ROOM;

/**
 * 상점 명령(목록·사기·팔기·값·확인·취소, D42)을 처리한다. {@link GameService} 가 게임 락과 트랜잭션 안에서 부른다.
 * 묻는 사람에게만 답한다(같은 방 알림 없음). 상위 등급 영석을 깨는 사기는 확인을 받아 10초 안에 {@code 확인} 하면 산다
 */
@Component
@RequiredArgsConstructor
class ShopCommandHandler {

    static final String UNAVAILABLE = "이곳에서는 할 수 없습니다.";
    static final String NO_SUCH_MERCHANT = "여기에 그런 상대는 없습니다.";
    static final String BUY_NO_ARGUMENT = "무엇을 사시겠습니까? (예: 사기 회춘단 5)";
    static final String SELL_NO_ARGUMENT = "무엇을 파시겠습니까? (예: 팔기 늑대 가죽 3)";
    static final String APPRAISE_NO_ARGUMENT = "무엇의 값을 보시겠습니까? (예: 값 늑대 가죽)";
    static final String NOTHING_TO_CONFIRM = "확인할 것이 없습니다.";
    static final String NOTHING_TO_CANCEL = "그만둘 것이 없습니다.";
    static final String CANCELLED = "그만두었습니다.";

    /** 상위 등급 영석 깨기 확인을 기다리는 시간(룰북 commands.md 확인이 필요한 명령 — 결제 10초) */
    static final Duration CONFIRM_WINDOW = Duration.ofSeconds(10);

    private final NpcFinder npcFinder;
    private final ItemTypeFinder itemTypeFinder;
    private final InventoryFinder inventoryFinder;
    private final InventoryEditor inventoryEditor;
    private final Clock clock;

    /** 상점 명령을 처리한다. send 는 (연결 id, 메시지)로 보낼 것을 모은다 */
    void handle(Command command, OnlinePlayer player, GameSession session, BiConsumer<String, GameMessage> send) {
        Reply reply = new Reply(player.getSessionId(), send);
        switch (command) {
            case Command.ShopList list -> list(player, list.args(), reply);
            case Command.Buy buy -> buy(player, session, buy.args(), reply);
            case Command.Sell sell -> sell(player, sell.args(), reply);
            case Command.Appraise appraise -> appraise(player, appraise.args(), reply);
            case Command.Confirm _ -> confirm(player, session, reply);
            case Command.Cancel _ -> cancel(session, reply);
            default -> throw new IllegalArgumentException("상점 명령이 아닙니다: " + command);
        }
    }

    // ---- 상점 고르기

    /**
     * {@code 목록 [NPC]}: 인자가 없으면 상점이 하나일 때 그 상점, 인자가 있으면 상점 NPC 이름(전체 또는 앞부분)에 맞아야 한다
     */
    private Optional<Npc> chooseShopToList(OnlinePlayer player, String args, Reply reply) {
        List<Npc> shops = shopsIn(player);
        if (shops.isEmpty()) return reply.fail(UNAVAILABLE);
        String name = ItemNames.normalize(args);
        if (name.isEmpty()) {
            if (shops.size() == 1) return Optional.of(shops.getFirst());
            return reply.fail(manyMerchants(shops));
        }
        Optional<Npc> found = findByName(shops, name);
        if (found.isEmpty()) return reply.fail(NO_SUCH_MERCHANT);
        return found;
    }

    /**
     * 사기·팔기·값: 상점이 하나면 인자 첫 낱말이 그 NPC 이름 전체와 같을 때만 떼어 낸다. 둘 이상이면 첫 낱말이 상점 NPC 이름
     * (전체 또는 앞부분)이어야 한다
     */
    private Optional<ShopChoice> chooseShop(OnlinePlayer player, String args, Reply reply) {
        List<Npc> shops = shopsIn(player);
        if (shops.isEmpty()) return reply.fail(UNAVAILABLE);
        String normalized = ItemNames.normalize(args);
        int space = normalized.indexOf(' ');
        String first = space < 0 ? normalized : normalized.substring(0, space);
        String rest = space < 0 ? "" : normalized.substring(space + 1);

        if (shops.size() == 1) {
            Npc shop = shops.getFirst();
            return Optional.of(!first.isEmpty() && shop.isCalled(first)
                    ? new ShopChoice(shop, rest)
                    : new ShopChoice(shop, normalized));
        }
        Optional<Npc> found = first.isEmpty() ? Optional.empty() : findByName(shops, first);
        if (found.isEmpty()) return reply.fail(manyMerchants(shops));
        return Optional.of(new ShopChoice(found.get(), rest));
    }

    /** 같은 방의 상점 NPC(배치 순 = id 순) */
    private List<Npc> shopsIn(OnlinePlayer player) {
        return npcFinder.findAllInRoom(player.getRoomId()).stream().filter(Npc::hasShop).toList();
    }

    /** 이름 전체가 같은 NPC, 없으면 이름이 입력으로 시작하는 첫 NPC(2차 {@code 대화} 규칙) */
    private static Optional<Npc> findByName(List<Npc> npcs, String input) {
        return npcs.stream().filter(npc -> npc.isCalled(input)).findFirst()
                .or(() -> npcs.stream().filter(npc -> npc.nameStartsWith(input)).findFirst());
    }

    private static String manyMerchants(List<Npc> shops) {
        return "상인이 여럿입니다. 상인 이름을 붙이십시오: "
                + shops.stream().map(Npc::getName).collect(Collectors.joining(" · "));
    }

    // ---- 목록

    private void list(OnlinePlayer player, String args, Reply reply) {
        Optional<Npc> chosen = chooseShopToList(player, args, reply);
        if (chosen.isEmpty()) return;
        Npc npc = chosen.get();

        StringBuilder text = new StringBuilder("[").append(npc.shopName()).append("]  파는 것  값(하품)");
        List<Offer> offers = offersOf(npc);
        if (offers.isEmpty()) {
            text.append("\n  파는 물건이 없습니다.");
        }
        for (int i = 0; i < offers.size(); i++) {
            Offer offer = offers.get(i);
            text.append("\n  ").append(i + 1).append(". ").append(offer.itemType().getName())
                    .append("  ").append(Numbers.count(offer.price()));
        }
        List<ItemCategory> buys = shopKindOf(npc).buyCategories();
        text.append("\n사 주는 것: ").append(buys.isEmpty() ? "없음"
                : buys.stream().map(ItemCategory::koreanName).collect(Collectors.joining("·")));
        reply.item(text.toString());
    }

    // ---- 사기

    /** 실패 순서: 인자 없음 → 수량 오류 → 없는 물건 → 결제·칸 검사(D42) → 상위 등급 확인 */
    private void buy(OnlinePlayer player, GameSession session, String args, Reply reply) {
        Optional<ShopChoice> chosen = chooseShop(player, args, reply);
        if (chosen.isEmpty()) return;
        Npc npc = chosen.get().npc();
        ItemArguments parsed = ItemArguments.parse(chosen.get().rest());
        ItemQuery query = parsed.item();
        if (query == null) {
            reply.error(BUY_NO_ARGUMENT);
            return;
        }
        if (!(parsed.amount() instanceof Amount.Count(int count))) {
            reply.error(INVALID_AMOUNT); // 사기에 전부는 없다
            return;
        }
        Optional<Offer> offer = ItemNames.select(offersOf(npc), o -> o.itemType().getName(), query);
        if (offer.isEmpty()) {
            reply.error(npcSays(npc, "그런 물건은 팔지 않소."));
            return;
        }
        purchase(player, session, npc, offer.get(), count, false, reply);
    }

    /** 지금 값으로 산다. 확인 전이면 상위 등급 영석을 깨야 할 때 묻고 기다린다 */
    private void purchase(OnlinePlayer player, GameSession session, Npc npc, Offer offer, int count, boolean confirmed,
                          Reply reply) {
        ItemType itemType = offer.itemType();
        Long itemTypeId = requireNonNull(itemType.getId());
        long total = Math.multiplyExact(offer.price(), count);
        PurchaseResult result = inventoryEditor.purchase(player.getCharacterId(), itemTypeId, count, total, confirmed);
        switch (result) {
            case PurchaseResult.Bought _ -> reply.item(Josa.withEulReul(things(itemType, count))
                    + " 샀습니다. (하품 영석 -" + Numbers.count(total) + ")");
            case PurchaseResult.NeedsConfirm needsConfirm -> {
                session.askConfirm(new PendingPurchase(requireNonNull(npc.getId()), itemTypeId, count, clock.instant()));
                reply.item(confirmQuestion(needsConfirm.payment().brokenHighGrades(), needsConfirm.payment().change()));
            }
            case PurchaseResult.NotEnough notEnough -> reply.error("영석이 모자랍니다. (필요 " + Numbers.count(total)
                    + ", 가진 것 " + Numbers.count(notEnough.have()) + ")");
            case PurchaseResult.BagLimit _ -> reply.error(BAG_LIMIT);
            case PurchaseResult.NoRoom _ -> reply.error(NO_ROOM);
            case PurchaseResult.NoRoomForChange noRoom -> reply.error("거스름을 받을 자리가 없습니다. (하품 "
                    + Numbers.count(noRoom.change()) + "개, " + Numbers.count(noRoom.slotsShort()) + "칸 필요)");
        }
    }

    /**
     * {@code 값을 치르려면 상품 영석 1개를 깨야 합니다. 거스름 하품 9,880개를 받습니다. 계속하려면 '확인'.}
     * 거스름은 마지막으로 낸 등급의 돌 하나에서만 나오므로 깨는 돌은 그 등급 1개다
     */
    private static String confirmQuestion(List<SpiritStoneGrade> broken, long change) {
        String stones = broken.stream().map(grade -> grade.itemName() + " 1개").collect(Collectors.joining(", "));
        StringBuilder text = new StringBuilder("값을 치르려면 ").append(Josa.withEulReul(stones)).append(" 깨야 합니다.");
        if (change > 0) {
            text.append(" 거스름 하품 ").append(Numbers.count(change)).append("개를 받습니다.");
        }
        return text.append(" 계속하려면 '확인'.").toString();
    }

    // ---- 확인·취소

    /** 10초 안이면 같은 상점·같은 물건·같은 수로 지금 값을 다시 계산해 확인 없이 산다(그 사이 바뀐 것은 다시 검사한다) */
    private void confirm(OnlinePlayer player, GameSession session, Reply reply) {
        Optional<PendingPurchase> pending = livePending(session);
        session.clearPending();
        if (pending.isEmpty()) {
            reply.error(NOTHING_TO_CONFIRM);
            return;
        }
        PendingPurchase purchase = pending.get();
        Optional<Npc> npc = shopsIn(player).stream()
                .filter(shop -> purchase.npcId().equals(shop.getId()))
                .findFirst();
        if (npc.isEmpty()) {
            reply.error(UNAVAILABLE);
            return;
        }
        Optional<Offer> offer = offersOf(npc.get()).stream()
                .filter(o -> purchase.itemTypeId().equals(o.itemType().getId()))
                .findFirst();
        if (offer.isEmpty()) {
            reply.error(npcSays(npc.get(), "그런 물건은 팔지 않소."));
            return;
        }
        purchase(player, session, npc.get(), offer.get(), purchase.count(), true, reply);
    }

    private void cancel(GameSession session, Reply reply) {
        boolean waiting = livePending(session).isPresent();
        session.clearPending();
        if (waiting) {
            reply.info(CANCELLED);
        } else {
            reply.error(NOTHING_TO_CANCEL);
        }
    }

    /** 10초가 지나지 않은 확인 대기 */
    private Optional<PendingPurchase> livePending(GameSession session) {
        return session.findPending()
                .filter(pending -> !clock.instant().isAfter(pending.askedAt().plus(CONFIRM_WINDOW)));
    }

    // ---- 팔기·값

    /**
     * 실패 순서: 인자 없음 → 수량 오류 → 가지고 있지 않음 → 영석 → 퀘스트 물건 → 사 주지 않는 종류 → 수 넘침
     * → 값이 1 미만 → 저물대 칸 → 받을 영석 칸(D42)
     */
    private void sell(OnlinePlayer player, String args, Reply reply) {
        Optional<ShopChoice> chosen = chooseShop(player, args, reply);
        if (chosen.isEmpty()) return;
        Npc npc = chosen.get().npc();
        ItemArguments parsed = ItemArguments.parse(chosen.get().rest());
        ItemQuery query = parsed.item();
        if (query == null) {
            reply.error(SELL_NO_ARGUMENT);
            return;
        }
        if (parsed.amount() instanceof Amount.Invalid) {
            reply.error(INVALID_AMOUNT);
            return;
        }
        Optional<InventoryEntry> found = findSellable(player, npc, query, reply);
        if (found.isEmpty()) return;
        InventoryEntry entry = found.get();
        ItemType itemType = entry.getItemType();
        int count = switch (parsed.amount()) {
            case Amount.Count(int value) -> value;
            case Amount.All _ -> entry.getCount();
            case Amount.Invalid _ -> throw new IllegalStateException("수량 오류는 위에서 거른다");
        };
        if (count > entry.getCount()) {
            reply.error(ItemCommandHandler.tooMany(itemType.getName(), entry.getCount()));
            return;
        }
        long payout = Pricing.buyTotal(itemType, count);
        if (payout < 1) {
            reply.error(npcSays(npc, "값이 너무 적어 사 줄 수가 없소. 더 모아 오시오."));
            return;
        }
        SaleResult result = inventoryEditor.sell(player.getCharacterId(), requireNonNull(itemType.getId()), count, payout);
        switch (result) {
            case SOLD -> reply.item(Josa.withEulReul(things(itemType, count))
                    + " 팔았습니다. (하품 영석 +" + Numbers.count(payout) + ")");
            case BAG_IN_USE -> reply.error(BAG_IN_USE);
            case NO_ROOM -> reply.error(NO_ROOM);
        }
    }

    /** {@code 늑대 가죽 1개에 하품 영석 0.8 — 10개면 8}. 수량을 빼면 가진 수. 수 넘침·1 미만 검사는 하지 않는다 */
    private void appraise(OnlinePlayer player, String args, Reply reply) {
        Optional<ShopChoice> chosen = chooseShop(player, args, reply);
        if (chosen.isEmpty()) return;
        Npc npc = chosen.get().npc();
        String rest = chosen.get().rest();
        ItemArguments parsed = ItemArguments.parse(rest);
        ItemQuery query = parsed.item();
        if (query == null) {
            reply.error(APPRAISE_NO_ARGUMENT);
            return;
        }
        if (parsed.amount() instanceof Amount.Invalid) {
            reply.error(INVALID_AMOUNT);
            return;
        }
        Optional<InventoryEntry> found = findSellable(player, npc, query, reply);
        if (found.isEmpty()) return;
        InventoryEntry entry = found.get();
        ItemType itemType = entry.getItemType();
        int count = parsed.amount() instanceof Amount.Count(int value) && ItemArguments.hasAmount(rest)
                ? value
                : entry.getCount();
        reply.item(itemType.getName() + " 1개에 하품 영석 " + Numbers.decimal(Pricing.unitBuyPrice(itemType))
                + " — " + Numbers.count(count) + "개면 " + Numbers.count(Pricing.buyTotal(itemType, count)));
    }

    /** 내 소지품에서 고른 것이 그 상점이 사 줄 수 있는 물건인지 본다. 아니면 문구를 보내고 비운다 */
    private Optional<InventoryEntry> findSellable(OnlinePlayer player, Npc npc, ItemQuery query, Reply reply) {
        Optional<InventoryEntry> found = ItemCommandHandler.selectHeld(inventoryFinder.find(player.getCharacterId()), query);
        if (found.isEmpty()) return reply.fail(NOT_HELD);
        InventoryEntry entry = found.get();
        ItemType itemType = entry.getItemType();
        if (itemType.isSpiritStone()) return reply.fail(npcSays(npc, "그런 물건은 받지 않소."));
        ItemCategory category = itemType.getCategory();
        // 퀘스트 물건은 어느 상점도 사지 않는다(매입 비율 0). 문구만 따로다
        if (category == ItemCategory.QUEST) return reply.fail(npcSays(npc, "그건 내가 살 물건이 아니오."));
        if (!shopKindOf(npc).buys(category)) {
            String hint = ShopKind.buyerOf(category)
                    .map(buyer -> " (" + Josa.withEunNeun(category.koreanName()) + " " + buyer.koreanName() + "에 팔 수 있습니다)")
                    .orElse("");
            return reply.fail(npcSays(npc, "그런 물건은 받지 않소.") + hint);
        }
        return found;
    }

    // ---- 공통

    /** 상점이 파는 물건(목록 순서)과 지금 값. 종류를 찾을 수 없는 줄은 뺀다 */
    private List<Offer> offersOf(Npc npc) {
        List<ShopItem> items = npc.getShopItems();
        Map<Long, ItemType> itemTypes = itemTypeFinder.findAllById(items.stream().map(ShopItem::itemTypeId).toList());
        return items.stream()
                .filter(item -> itemTypes.containsKey(item.itemTypeId()))
                .map(item -> {
                    ItemType itemType = itemTypes.get(item.itemTypeId());
                    return new Offer(itemType, item.effectivePrice(itemType));
                })
                .toList();
    }

    private static ShopKind shopKindOf(Npc npc) {
        return npc.findShopKind().orElseThrow(() -> new IllegalStateException("상점이 아닙니다: " + npc.getName()));
    }

    /** {@code 왕씨: "그런 물건은 팔지 않소."} */
    private static String npcSays(Npc npc, String words) {
        return npc.getName() + ": \"" + words + "\"";
    }

    /** {@code 회춘단(연기 초) 5개} */
    private static String things(ItemType itemType, long count) {
        return itemType.getName() + " " + Numbers.count(count) + "개";
    }

    /** 고른 상점과 NPC 이름을 뗀 나머지 인자 */
    private record ShopChoice(Npc npc, String rest) {
    }

    /** 상점이 파는 물건 한 줄과 지금 값(하품) */
    private record Offer(ItemType itemType, long price) {
    }

    /** 묻는 사람에게 답한다 */
    private record Reply(String sessionId, BiConsumer<String, GameMessage> send) {

        void item(String text) {
            send.accept(sessionId, SystemMessage.item(text));
        }

        void info(String text) {
            send.accept(sessionId, SystemMessage.info(text));
        }

        void error(String text) {
            send.accept(sessionId, SystemMessage.error(text));
        }

        /** 오류를 보내고 빈 값을 돌려준다 */
        <T> Optional<T> fail(String text) {
            error(text);
            return Optional.empty();
        }
    }
}
