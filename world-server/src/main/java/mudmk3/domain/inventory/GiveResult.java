package mudmk3.domain.inventory;

/** 주기 결과: 주었음, 받는 쪽 칸이 모자람, 받는 쪽 저물대가 이미 세 개, 주는 쪽이 저물대를 주면 칸이 모자람 */
public enum GiveResult {
    GIVEN,
    RECEIVER_NO_ROOM,
    RECEIVER_BAG_LIMIT,
    GIVER_BAG_IN_USE
}
