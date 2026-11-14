package mudmk3.domain.inventory;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import mudmk3.domain.AbstractEntity;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.item.Slots;
import mudmk3.domain.item.SpiritStoneGrade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;

import static java.util.Objects.requireNonNull;
import static org.springframework.util.Assert.state;

/**
 * 소지품: 캐릭터마다 하나. 칸 = 기본 2칸 + 가진 저물대 칸의 합 + (저물탁이 열리면) 저물탁 칸. 저물대는 세 개, 저물탁은 하나까지이고
 * 둘 다 칸을 차지하지 않는다. 그 밖의 물건은 종류 묶음마다 올림(수 ÷ 겹침) 칸을 차지한다(룰북 item.md 저물대 · 저물탁).
 * 영석도 칸을 차지하는 물건이다. 소지품은 경지를 모르고, 칸을 보는 메서드는 저물탁이 열렸는지({@code braceletOpen}, 축기 초기 이상)를
 * 마지막 인자로 받는다. 캐릭터는 id로만 갖는다(FK 없음). 같은 종류의 묶음은 하나뿐이고, 이미 있으면(수가 0이라도) 그 묶음을 쓴다.
 * <p>
 * 경지가 내려가 저물탁이 닫히면 쓰는 칸이 칸보다 많을 수 있다(칸을 넘은 소지품). 늘리는 변경(넣기·사기)은 변경 뒤 쓰는 칸이
 * 칸 이하일 때만 되고, 줄이는 변경(빼기·팔기)은 그 조건이거나 쓰는 칸이 늘지 않고 칸도 줄지 않으면 된다.
 * 바꾸는 메서드는 검사를 통과하지 못하면 {@code IllegalStateException} 이고 아무것도 바꾸지 않으며, 바꾼 뒤 수가 0인 묶음을 뺀다
 */
@Entity
@Getter
@ToString(callSuper = true, exclude = "entries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inventory extends AbstractEntity {

    /** 영석(하품 → 극품) 먼저, 나머지는 묶음이 처음 생긴 순서(목록 순서) */
    private static final Comparator<InventoryEntry> DISPLAY_ORDER = Comparator.comparingInt(
            entry -> entry.getItemType().findSpiritStoneGrade().map(Enum::ordinal).orElse(Integer.MAX_VALUE));

    private Long characterId;

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "inventory")
    private List<InventoryEntry> entries = new ArrayList<>();

    /** 아무것도 없는 소지품(기본 2칸) */
    public static Inventory empty(Long characterId) {
        Inventory inventory = new Inventory();
        inventory.characterId = requireNonNull(characterId);
        return inventory;
    }

    /** 표시 순서의 묶음 목록(바꿀 수 없음): 영석 하품 → 극품, 그다음 나머지를 얻은 순서로 */
    public List<InventoryEntry> getEntries() {
        List<InventoryEntry> sorted = new ArrayList<>(entries);
        sorted.sort(DISPLAY_ORDER);
        return Collections.unmodifiableList(sorted);
    }

    public int usedSlots() {
        return Math.toIntExact(now(false).usedSlots());
    }

    /** 전체 칸. 저물탁 칸은 {@code braceletOpen} 일 때만 더한다 */
    public int capacity(boolean braceletOpen) {
        return Math.toIntExact(now(braceletOpen).capacity());
    }

    /** 가진 저물대 수(저물탁은 세지 않음) */
    public int bagCount() {
        return Math.toIntExact(now(false).bagCount());
    }

    /** 가진 저물탁 수 */
    public int braceletCount() {
        return Math.toIntExact(now(false).braceletCount());
    }

    /** 가진 영석의 등급별 수(하품 → 극품, 가진 등급만) */
    public Map<SpiritStoneGrade, Long> spiritStones() {
        Map<SpiritStoneGrade, Long> stones = new EnumMap<>(SpiritStoneGrade.class);
        for (InventoryEntry entry : entries) {
            if (entry.getCount() == 0) continue;
            entry.getItemType().findSpiritStoneGrade()
                    .ifPresent(grade -> stones.merge(grade, (long) entry.getCount(), Long::sum));
        }
        return Collections.unmodifiableMap(stones);
    }

    /** 가진 영석의 하품 기준 합 */
    public long spiritStoneValue() {
        long sum = 0;
        for (Map.Entry<SpiritStoneGrade, Long> stone : spiritStones().entrySet()) {
            sum = Math.addExact(sum, Math.multiplyExact(stone.getValue(), stone.getKey().value()));
        }
        return sum;
    }

    /** 그 물건을 더 넣을 수 있는 최대 수. 저물대는 세 개, 저물탁은 하나 한도까지. 칸을 넘은 소지품에는 0 */
    public int roomFor(ItemType itemType, boolean braceletOpen) {
        requireNonNull(itemType);
        Usage now = now(braceletOpen);
        if (now.usedSlots() > now.capacity()) return 0;
        if (itemType.isBracelet()) {
            return Math.toIntExact(Math.max(0, Slots.MAX_BRACELETS - now.braceletCount()));
        }
        if (itemType.isBag()) {
            return Math.toIntExact(Math.max(0, Slots.MAX_BAGS - now.bagCount()));
        }
        long stack = itemType.stackSize();
        long held = held(itemType);
        long slack = Math.ceilDiv(held, stack) * stack - held;
        long free = now.capacity() - now.usedSlots();
        return Math.toIntExact(free * stack + slack);
    }

    /** 그 물건 {@code count} 개를 넣을 수 있는지. 어떤 소지품에도 들어가지 않을 만큼 큰 수는 계산하지 않고 거절한다 */
    public AddCheck checkAdd(ItemType itemType, long count, boolean braceletOpen) {
        requireNonNull(itemType);
        requirePositive(count);
        if (count > Integer.MAX_VALUE) {
            if (itemType.isBracelet()) return AddCheck.BRACELET_LIMIT;
            return itemType.isBag() ? AddCheck.BAG_LIMIT : AddCheck.NO_ROOM;
        }
        return checkAdd(new Changes().add(itemType, count), braceletOpen);
    }

    public void add(ItemType itemType, int count, boolean braceletOpen) {
        requireNonNull(itemType);
        requirePositive(count);
        Changes changes = new Changes().add(itemType, count);
        state(checkAdd(changes, braceletOpen) == AddCheck.OK, () -> "소지품에 넣을 수 없습니다: " + itemType.getName());
        apply(changes);
    }

    /**
     * 물건 여럿을 그 순서대로 한 번에 넣는다(시작 소지품). 다 넣은 뒤의 칸으로 한 번만 검사하므로
     * 저물대가 뒤에 있어도 된다. 다 들어가지 않으면 {@code IllegalStateException} 이고 아무것도 넣지 않는다
     */
    public void addAll(SequencedMap<ItemType, Integer> items, boolean braceletOpen) {
        requireNonNull(items);
        Changes changes = new Changes();
        items.forEach((itemType, count) -> {
            requirePositive(requireNonNull(count));
            changes.add(itemType, count);
        });
        state(checkAdd(changes, braceletOpen) == AddCheck.OK, "소지품에 다 넣을 수 없습니다.");
        apply(changes);
    }

    /**
     * 가진 그 묶음에서 {@code count} 개를 뺄 수 있는지(버리기·주기). 가진 수보다 많으면 {@code IllegalStateException}.
     * 칸이 줄어 뺄 수 없으면 빼는 물건이 저물탁일 때 {@code BRACELET_IN_USE}, 저물대일 때 {@code BAG_IN_USE}.
     * 플레이어 문구는 게임 명령이 먼저 검사해 낸다
     */
    public RemoveCheck checkRemove(ItemType itemType, int count, boolean braceletOpen) {
        if (canShrink(removal(itemType, count), braceletOpen)) return RemoveCheck.OK;
        return itemType.isBracelet() ? RemoveCheck.BRACELET_IN_USE : RemoveCheck.BAG_IN_USE;
    }

    /** {@link #checkRemove} 가 OK일 때만 뺀다 */
    public void remove(ItemType itemType, int count, boolean braceletOpen) {
        RemoveCheck check = checkRemove(itemType, count, braceletOpen);
        state(check == RemoveCheck.OK, () -> "뺄 수 없습니다: " + check);
        apply(removal(itemType, count));
    }

    /**
     * 그 물건 {@code count} 개를 {@code price}(하품 기준)에 살 수 있는지. 순서: 영석 모자람 → 저물대 세 개 → 저물탁 하나 →
     * 결제한 영석을 뺀 뒤 칸 없음 → 거스름까지 넣으면 칸 없음 → 상품·극품 깨기 확인 → 살 수 있음
     */
    public PurchaseResult checkPurchase(ItemType itemType, int count, long price, ItemType lowStone, boolean braceletOpen) {
        requireNonNull(itemType);
        requirePositive(count);
        requireLowStone(lowStone);
        Optional<SpiritStonePayment> plan = SpiritStonePayment.plan(spiritStones(), price);
        if (plan.isEmpty()) return new PurchaseResult.NotEnough(spiritStoneValue());
        SpiritStonePayment payment = plan.get();

        Changes changes = new Changes();
        payment.paid().forEach((grade, paid) -> changes.add(heldStone(grade), -paid));
        changes.add(itemType, count);
        Usage withItem = after(changes, braceletOpen);
        if (withItem.bagCount() > Slots.MAX_BAGS) return new PurchaseResult.BagLimit();
        if (withItem.braceletCount() > Slots.MAX_BRACELETS) return new PurchaseResult.BraceletLimit();
        if (withItem.usedSlots() > withItem.capacity()) return new PurchaseResult.NoRoom();
        if (payment.change() > 0) {
            Usage withChange = after(changes.add(lowStone, payment.change()), braceletOpen);
            if (withChange.usedSlots() > withChange.capacity()) {
                int slotsShort = Math.toIntExact(withChange.usedSlots() - withChange.capacity());
                return new PurchaseResult.NoRoomForChange(payment.change(), slotsShort);
            }
        }
        return payment.breaksHighStones() ? new PurchaseResult.NeedsConfirm(payment) : new PurchaseResult.Bought(payment);
    }

    /**
     * 결제한다: 낸 영석을 빼고 산 물건과 거스름 하품을 넣는다. {@link #checkPurchase} 가 같은 결제로
     * 살 수 있음이나 확인 필요를 돌려주는 경우에만 바꾼다(낸 영석은 검사와 같은 가진 묶음에서 뺀다. {@code stones} 는 거스름 하품 종류를 꺼내는 데 쓴다)
     */
    public void purchase(ItemType itemType, int count, SpiritStonePayment payment, Map<SpiritStoneGrade, ItemType> stones,
                         boolean braceletOpen) {
        requireNonNull(payment);
        requireNonNull(stones);
        ItemType lowStone = requireNonNull(stones.get(SpiritStoneGrade.LOW), "하품 영석 종류가 없습니다.");
        PurchaseResult result = checkPurchase(itemType, count, payment.paidValue() - payment.change(), lowStone, braceletOpen);
        SpiritStonePayment planned = switch (result) {
            case PurchaseResult.Bought bought -> bought.payment();
            case PurchaseResult.NeedsConfirm needsConfirm -> needsConfirm.payment();
            default -> null;
        };
        state(payment.equals(planned), () -> "결제할 수 없습니다: " + result);

        Changes changes = new Changes();
        payment.paid().forEach((grade, paid) -> changes.add(heldStone(grade), -paid));
        changes.add(itemType, count);
        if (payment.change() > 0) changes.add(lowStone, payment.change());
        apply(changes);
    }

    /**
     * 가진 그 묶음 {@code count} 개를 팔고 하품 {@code payout} 개를 받을 수 있는지. 가진 수보다 많으면
     * {@code IllegalStateException}. 판 물건이 빠져 칸이 모자라면 저물탁은 {@code BRACELET_IN_USE}, 저물대는 {@code BAG_IN_USE},
     * 받을 영석까지 넣으면 칸이 모자라면 {@code NO_ROOM}
     */
    public SaleResult checkSale(ItemType itemType, int count, long payout, ItemType lowStone, boolean braceletOpen) {
        requireLowStone(lowStone);
        if (payout < 0) throw new IllegalArgumentException("받는 영석 수는 0 이상이어야 합니다: " + payout);
        Changes changes = removal(itemType, count);
        if (!canShrink(changes, braceletOpen)) {
            return itemType.isBracelet() ? SaleResult.BRACELET_IN_USE : SaleResult.BAG_IN_USE;
        }
        if (payout > 0 && !canShrink(changes.add(lowStone, payout), braceletOpen)) return SaleResult.NO_ROOM;
        return SaleResult.SOLD;
    }

    public void sell(ItemType itemType, int count, long payout, ItemType lowStone, boolean braceletOpen) {
        SaleResult result = checkSale(itemType, count, payout, lowStone, braceletOpen);
        state(result == SaleResult.SOLD, () -> "팔 수 없습니다: " + result);
        Changes changes = removal(itemType, count);
        if (payout > 0) changes.add(lowStone, payout);
        apply(changes);
    }

    private Changes removal(ItemType itemType, int count) {
        requireNonNull(itemType);
        requirePositive(count);
        state(held(itemType) >= count, () -> "가진 것보다 많이 뺄 수 없습니다: " + itemType.getName());
        return new Changes().add(itemType, -count);
    }

    /** 늘리는 변경: 한도를 넘지 않고 변경 뒤 쓰는 칸이 변경 뒤 칸 이하일 때만 */
    private AddCheck checkAdd(Changes changes, boolean braceletOpen) {
        Usage usage = after(changes, braceletOpen);
        if (usage.bagCount() > Slots.MAX_BAGS) return AddCheck.BAG_LIMIT;
        if (usage.braceletCount() > Slots.MAX_BRACELETS) return AddCheck.BRACELET_LIMIT;
        if (usage.usedSlots() > usage.capacity()) return AddCheck.NO_ROOM;
        return AddCheck.OK;
    }

    /** 줄이는 변경: 늘리는 변경과 같은 조건이거나, 쓰는 칸이 늘지 않고 칸도 줄지 않으면(칸을 넘은 소지품) */
    private boolean canShrink(Changes changes, boolean braceletOpen) {
        if (checkAdd(changes, braceletOpen) == AddCheck.OK) return true;
        Usage now = now(braceletOpen);
        Usage after = after(changes, braceletOpen);
        return after.usedSlots() <= now.usedSlots() && after.capacity() >= now.capacity();
    }

    private long held(ItemType itemType) {
        return findEntry(itemType).map(InventoryEntry::getCount).orElse(0);
    }

    private ItemType heldStone(SpiritStoneGrade grade) {
        return entries.stream()
                .map(InventoryEntry::getItemType)
                .filter(itemType -> itemType.findSpiritStoneGrade().orElse(null) == grade)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("가진 영석이 없습니다: " + grade.itemName()));
    }

    private Optional<InventoryEntry> findEntry(ItemType itemType) {
        return entries.stream().filter(entry -> entry.isFor(itemType)).findFirst();
    }

    private Usage now(boolean braceletOpen) {
        return after(new Changes(), braceletOpen);
    }

    /** 바꾼 뒤의 칸 사용. 바꾸지 않고 계산만 한다. 닫힌 저물탁은 칸을 더하지 않는다 */
    private Usage after(Changes changes, boolean braceletOpen) {
        Map<ItemType, Long> counts = new LinkedHashMap<>();
        for (InventoryEntry entry : entries) {
            counts.merge(entry.getItemType(), (long) entry.getCount(), Long::sum);
        }
        changes.deltas.forEach((key, delta) -> counts.merge(key, delta, Long::sum));

        long used = 0;
        long bagSlots = 0;
        long bags = 0;
        long bracelets = 0;
        for (Map.Entry<ItemType, Long> count : counts.entrySet()) {
            ItemType itemType = count.getKey();
            long n = count.getValue();
            state(n >= 0, () -> "묶음의 수는 0 아래로 내려갈 수 없습니다: " + itemType.getName());
            used += Slots.slotsFor(itemType, n);
            if (itemType.isBracelet()) {
                bracelets += n;
                if (braceletOpen) bagSlots += n * itemType.bagSlots();
            } else if (itemType.isBag()) {
                bags += n;
                bagSlots += n * itemType.bagSlots();
            }
        }
        return new Usage(used, Slots.BASE + bagSlots, bags, bracelets);
    }

    private void apply(Changes changes) {
        changes.deltas.forEach((itemType, delta) -> {
            InventoryEntry entry = findEntry(itemType).orElseGet(() -> {
                InventoryEntry created = InventoryEntry.create(this, itemType);
                entries.add(created);
                return created;
            });
            entry.changeCount(delta);
        });
        entries.removeIf(entry -> entry.getCount() == 0);
    }

    private static void requirePositive(long count) {
        if (count < 1) throw new IllegalArgumentException("수는 1 이상이어야 합니다: " + count);
    }

    private static void requireLowStone(ItemType lowStone) {
        requireNonNull(lowStone);
        if (lowStone.findSpiritStoneGrade().orElse(null) != SpiritStoneGrade.LOW) {
            throw new IllegalArgumentException("하품 영석 종류가 아닙니다: " + lowStone.getName());
        }
    }

    private record Usage(long usedSlots, long capacity, long bagCount, long braceletCount) {
    }

    /** 묶음마다 더하거나 뺄 수. 같은 묶음은 하나로 합친다 */
    private static final class Changes {

        private final Map<ItemType, Long> deltas = new LinkedHashMap<>();

        Changes add(ItemType itemType, long delta) {
            deltas.merge(requireNonNull(itemType), delta, Long::sum);
            return this;
        }
    }
}
