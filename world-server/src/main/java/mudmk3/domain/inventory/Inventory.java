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
 * 소지품: 캐릭터마다 하나. 칸 = 기본 2칸 + 가진 저물대 칸의 합, 저물대는 세 개까지이고 칸을 차지하지 않는다. 그 밖의 물건은
 * (종류, 귀속) 묶음마다 올림(수 ÷ 겹침) 칸을 차지한다(룰북 item.md 저물대 · 저물탁). 영석도 칸을 차지하는 물건이다.
 * 캐릭터는 id로만 갖는다(FK 없음). 같은 (종류, 귀속) 묶음은 하나뿐이고, 이미 있으면(수가 0이라도) 그 묶음을 쓴다.
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
        return Math.toIntExact(after(new Changes()).usedSlots());
    }

    public int capacity() {
        return Math.toIntExact(after(new Changes()).capacity());
    }

    public int bagCount() {
        return Math.toIntExact(after(new Changes()).bagCount());
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

    /** 귀속 안 된 그 물건을 더 넣을 수 있는 최대 수. 저물대는 세 개 한도까지 */
    public int roomFor(ItemType itemType) {
        requireNonNull(itemType);
        Usage now = after(new Changes());
        if (itemType.isBag()) {
            return Math.toIntExact(Math.max(0, Slots.MAX_BAGS - now.bagCount()));
        }
        long stack = itemType.stackSize();
        long held = held(itemType, false);
        long slack = Math.ceilDiv(held, stack) * stack - held;
        long free = Math.max(0, now.capacity() - now.usedSlots());
        return Math.toIntExact(free * stack + slack);
    }

    /** 귀속 안 된 그 물건 {@code count} 개를 넣을 수 있는지. 어떤 소지품에도 들어가지 않을 만큼 큰 수는 계산하지 않고 거절한다 */
    public AddCheck checkAdd(ItemType itemType, long count) {
        requireNonNull(itemType);
        requirePositive(count);
        if (count > Integer.MAX_VALUE) return itemType.isBag() ? AddCheck.BAG_LIMIT : AddCheck.NO_ROOM;
        return checkAdd(new Changes().add(itemType, false, count));
    }

    public void add(ItemType itemType, int count, boolean bound) {
        requireNonNull(itemType);
        requirePositive(count);
        state(!bound || !itemType.isSpiritStone(), "영석은 귀속할 수 없습니다.");
        Changes changes = new Changes().add(itemType, bound, count);
        state(checkAdd(changes) == AddCheck.OK, () -> "소지품에 넣을 수 없습니다: " + itemType.getName());
        apply(changes);
    }

    /**
     * 귀속 안 된 물건 여럿을 그 순서대로 한 번에 넣는다(시작 소지품). 다 넣은 뒤의 칸으로 한 번만 검사하므로
     * 저물대가 뒤에 있어도 된다. 다 들어가지 않으면 {@code IllegalStateException} 이고 아무것도 넣지 않는다
     */
    public void addAll(SequencedMap<ItemType, Integer> items) {
        requireNonNull(items);
        Changes changes = new Changes();
        items.forEach((itemType, count) -> {
            requirePositive(requireNonNull(count));
            changes.add(itemType, false, count);
        });
        state(checkAdd(changes) == AddCheck.OK, "소지품에 다 넣을 수 없습니다.");
        apply(changes);
    }

    /**
     * 가진 그 묶음에서 {@code count} 개를 뺄 수 있는지(버리기·주기). 가진 수보다 많거나 귀속된 묶음이면
     * {@code IllegalStateException} — 귀속된 물건은 버리거나 줄 수 없고(룰북 item.md 귀속), 3차에 귀속 물건을 빼는 다른 경로는 없다.
     * 플레이어 문구는 게임 명령이 먼저 검사해 낸다
     */
    public RemoveCheck checkRemove(ItemType itemType, boolean bound, int count) {
        return fits(unboundRemoval(itemType, bound, count)) ? RemoveCheck.OK : RemoveCheck.BAG_IN_USE;
    }

    /** {@link #checkRemove} 가 OK일 때만 뺀다 */
    public void remove(ItemType itemType, boolean bound, int count) {
        state(checkRemove(itemType, bound, count) == RemoveCheck.OK, "저물대에 든 물건을 먼저 비우십시오.");
        apply(unboundRemoval(itemType, bound, count));
    }

    /**
     * 귀속 안 된 그 물건 {@code count} 개를 {@code price}(하품 기준)에 살 수 있는지. 순서: 영석 모자람 → 저물대 세 개 →
     * 결제한 영석을 뺀 뒤 칸 없음 → 거스름까지 넣으면 칸 없음 → 상품·극품 깨기 확인 → 살 수 있음
     */
    public PurchaseResult checkPurchase(ItemType itemType, int count, long price, ItemType lowStone) {
        requireNonNull(itemType);
        requirePositive(count);
        requireLowStone(lowStone);
        Optional<SpiritStonePayment> plan = SpiritStonePayment.plan(spiritStones(), price);
        if (plan.isEmpty()) return new PurchaseResult.NotEnough(spiritStoneValue());
        SpiritStonePayment payment = plan.get();

        Changes changes = new Changes();
        payment.paid().forEach((grade, paid) -> changes.add(heldStone(grade), false, -paid));
        changes.add(itemType, false, count);
        Usage withItem = after(changes);
        if (withItem.bagCount() > Slots.MAX_BAGS) return new PurchaseResult.BagLimit();
        if (withItem.usedSlots() > withItem.capacity()) return new PurchaseResult.NoRoom();
        if (payment.change() > 0) {
            Usage withChange = after(changes.add(lowStone, false, payment.change()));
            if (withChange.usedSlots() > withChange.capacity()) {
                int slotsNeeded = Math.toIntExact(withChange.usedSlots() - withItem.usedSlots());
                return new PurchaseResult.NoRoomForChange(payment.change(), slotsNeeded);
            }
        }
        return payment.breaksHighStones() ? new PurchaseResult.NeedsConfirm(payment) : new PurchaseResult.Bought(payment);
    }

    /**
     * 결제한다: 낸 영석을 빼고 산 물건(귀속 안 됨)과 거스름 하품을 넣는다. {@link #checkPurchase} 가 같은 결제로
     * 살 수 있음이나 확인 필요를 돌려주는 경우에만 바꾼다(낸 영석은 검사와 같은 가진 묶음에서 뺀다. {@code stones} 는 거스름 하품 종류를 꺼내는 데 쓴다)
     */
    public void purchase(ItemType itemType, int count, SpiritStonePayment payment, Map<SpiritStoneGrade, ItemType> stones) {
        requireNonNull(payment);
        requireNonNull(stones);
        ItemType lowStone = requireNonNull(stones.get(SpiritStoneGrade.LOW), "하품 영석 종류가 없습니다.");
        PurchaseResult result = checkPurchase(itemType, count, payment.paidValue() - payment.change(), lowStone);
        SpiritStonePayment planned = switch (result) {
            case PurchaseResult.Bought bought -> bought.payment();
            case PurchaseResult.NeedsConfirm needsConfirm -> needsConfirm.payment();
            default -> null;
        };
        state(payment.equals(planned), () -> "결제할 수 없습니다: " + result);

        Changes changes = new Changes();
        payment.paid().forEach((grade, paid) -> changes.add(heldStone(grade), false, -paid));
        changes.add(itemType, false, count);
        if (payment.change() > 0) changes.add(lowStone, false, payment.change());
        apply(changes);
    }

    /**
     * 가진 그 묶음 {@code count} 개를 팔고 하품 {@code payout} 개를 받을 수 있는지. 가진 수보다 많거나 귀속된 묶음이면
     * {@code IllegalStateException}(귀속된 물건은 팔 수 없다, 룰북 item.md 귀속)
     */
    public SaleResult checkSale(ItemType itemType, boolean bound, int count, long payout, ItemType lowStone) {
        requireLowStone(lowStone);
        if (payout < 0) throw new IllegalArgumentException("받는 영석 수는 0 이상이어야 합니다: " + payout);
        Changes changes = unboundRemoval(itemType, bound, count);
        if (!fits(changes)) return SaleResult.BAG_IN_USE;
        if (payout > 0 && !fits(changes.add(lowStone, false, payout))) return SaleResult.NO_ROOM;
        return SaleResult.SOLD;
    }

    public void sell(ItemType itemType, boolean bound, int count, long payout, ItemType lowStone) {
        SaleResult result = checkSale(itemType, bound, count, payout, lowStone);
        state(result == SaleResult.SOLD, () -> "팔 수 없습니다: " + result);
        Changes changes = unboundRemoval(itemType, bound, count);
        if (payout > 0) changes.add(lowStone, false, payout);
        apply(changes);
    }

    /** 귀속 안 된 묶음에서 하나를 귀속된 묶음으로 옮긴다 */
    public void bind(ItemType itemType) {
        requireNonNull(itemType);
        state(itemType.isBindable(), () -> itemType.getName() + "은(는) 귀속할 수 없는 종류입니다.");
        moveOne(itemType, false);
    }

    /** 귀속된 묶음에서 하나를 귀속 안 된 묶음으로 옮긴다 */
    public void unbind(ItemType itemType) {
        requireNonNull(itemType);
        moveOne(itemType, true);
    }

    private void moveOne(ItemType itemType, boolean fromBound) {
        state(held(itemType, fromBound) >= 1, () -> (fromBound ? "귀속된 " : "귀속 안 된 ") + itemType.getName() + "이(가) 없습니다.");
        Changes changes = new Changes().add(itemType, fromBound, -1).add(itemType, !fromBound, 1);
        state(fits(changes), "소지품 칸이 모자랍니다.");
        apply(changes);
    }

    private Changes unboundRemoval(ItemType itemType, boolean bound, int count) {
        requireNonNull(itemType);
        requirePositive(count);
        state(!bound, () -> "귀속된 물건은 버리거나 팔거나 줄 수 없습니다: " + itemType.getName());
        state(held(itemType, bound) >= count, () -> "가진 것보다 많이 뺄 수 없습니다: " + itemType.getName());
        return new Changes().add(itemType, bound, -count);
    }

    private AddCheck checkAdd(Changes changes) {
        Usage usage = after(changes);
        if (usage.bagCount() > Slots.MAX_BAGS) return AddCheck.BAG_LIMIT;
        if (usage.usedSlots() > usage.capacity()) return AddCheck.NO_ROOM;
        return AddCheck.OK;
    }

    private boolean fits(Changes changes) {
        return checkAdd(changes) == AddCheck.OK;
    }

    private long held(ItemType itemType, boolean bound) {
        return findEntry(itemType, bound).map(InventoryEntry::getCount).orElse(0);
    }

    private ItemType heldStone(SpiritStoneGrade grade) {
        return entries.stream()
                .map(InventoryEntry::getItemType)
                .filter(itemType -> itemType.findSpiritStoneGrade().orElse(null) == grade)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("가진 영석이 없습니다: " + grade.itemName()));
    }

    private Optional<InventoryEntry> findEntry(ItemType itemType, boolean bound) {
        return entries.stream().filter(entry -> entry.isFor(itemType, bound)).findFirst();
    }

    /** 바꾼 뒤의 칸 사용. 바꾸지 않고 계산만 한다 */
    private Usage after(Changes changes) {
        Map<Key, Long> counts = new LinkedHashMap<>();
        for (InventoryEntry entry : entries) {
            counts.merge(new Key(entry.getItemType(), entry.isBound()), (long) entry.getCount(), Long::sum);
        }
        changes.deltas.forEach((key, delta) -> counts.merge(key, delta, Long::sum));

        long used = 0;
        long bagSlots = 0;
        long bags = 0;
        for (Map.Entry<Key, Long> count : counts.entrySet()) {
            ItemType itemType = count.getKey().itemType();
            long n = count.getValue();
            state(n >= 0, () -> "묶음의 수는 0 아래로 내려갈 수 없습니다: " + itemType.getName());
            used += Slots.slotsFor(itemType, n);
            if (itemType.isBag()) {
                bags += n;
                bagSlots += n * itemType.bagSlots();
            }
        }
        return new Usage(used, Slots.BASE + bagSlots, bags);
    }

    private void apply(Changes changes) {
        changes.deltas.forEach((key, delta) -> {
            InventoryEntry entry = findEntry(key.itemType(), key.bound()).orElseGet(() -> {
                InventoryEntry created = InventoryEntry.create(this, key.itemType(), key.bound());
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

    private record Key(ItemType itemType, boolean bound) {
    }

    private record Usage(long usedSlots, long capacity, long bagCount) {
    }

    /** 묶음마다 더하거나 뺄 수. 같은 묶음은 하나로 합친다 */
    private static final class Changes {

        private final Map<Key, Long> deltas = new LinkedHashMap<>();

        Changes add(ItemType itemType, boolean bound, long delta) {
            deltas.merge(new Key(requireNonNull(itemType), bound), delta, Long::sum);
            return this;
        }
    }
}
