package mudmk3.domain.item;

/** 소지품 칸 계산(룰북 item.md 저물대 · 저물탁) */
public final class Slots {

    /** 저물대가 없어도 가진 기본 칸 */
    public static final int BASE = 2;

    /** 함께 찰 수 있는 저물대 수 */
    public static final int MAX_BAGS = 3;

    private Slots() {
    }

    /** 한 묶음이 차지하는 칸: 저물대는 0, 나머지는 올림(수 ÷ 겹침) */
    public static int slotsFor(ItemType itemType, long count) {
        if (itemType.isBag()) return 0;
        return Math.toIntExact(Math.ceilDiv(count, (long) itemType.stackSize()));
    }

    /** 전체 칸: 기본 칸 + 가진 저물대 칸의 합 */
    public static int capacity(int bagSlotSum) {
        return BASE + bagSlotSum;
    }
}
