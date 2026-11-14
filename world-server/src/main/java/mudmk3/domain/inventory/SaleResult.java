package mudmk3.domain.inventory;

/** 팔기 결과: 팔았음, 저물대를 팔면 칸이 모자람, 받을 하품 영석이 들어갈 칸이 없음 */
public enum SaleResult {
    SOLD,
    BAG_IN_USE,
    NO_ROOM
}
