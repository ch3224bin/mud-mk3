package mudmk3.domain.inventory;

/** 물건을 넣을 수 있는지: 넣을 수 있음, 칸이 모자람, 저물대가 이미 세 개 */
public enum AddCheck {
    OK,
    NO_ROOM,
    BAG_LIMIT
}
