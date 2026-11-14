package mudmk3.domain.inventory;

/**
 * 사기 검사 결과. 검사 순서: 영석 모자람 → 저물대 세 개 → 결제한 영석을 뺀 뒤에도 산 물건이 들어갈 칸 없음
 * → 거스름까지 넣으면 칸 없음 → 상품·극품을 깨야 해 확인이 필요함 → 살 수 있음
 */
public sealed interface PurchaseResult {

    /** 살 수 있음(또는 샀음) */
    record Bought(SpiritStonePayment payment) implements PurchaseResult {
    }

    /** 상품·극품을 깨야 해 확인을 받아야 함 */
    record NeedsConfirm(SpiritStonePayment payment) implements PurchaseResult {
    }

    /** 영석이 모자람. {@code have} 는 가진 영석의 하품 기준 합 */
    record NotEnough(long have) implements PurchaseResult {
    }

    /** 저물대는 세 개까지만 찰 수 있음 */
    record BagLimit() implements PurchaseResult {
    }

    /** 결제한 영석을 뺀 뒤에도 산 물건이 들어갈 칸이 없음 */
    record NoRoom() implements PurchaseResult {
    }

    /** 거스름 하품을 받을 칸이 없음. {@code slotsNeeded} 는 거스름을 넣으면 늘어나는 칸 */
    record NoRoomForChange(long change, int slotsNeeded) implements PurchaseResult {
    }
}
