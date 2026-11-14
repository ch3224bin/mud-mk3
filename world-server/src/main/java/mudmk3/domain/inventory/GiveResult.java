package mudmk3.domain.inventory;

/**
 * 주기 결과: 주었음, 받는 쪽 칸이 모자람, 받는 쪽 저물대가 이미 세 개, 받는 쪽 저물탁이 이미 하나,
 * 주는 쪽이 저물대를 주면 칸이 모자람, 주는 쪽이 저물탁을 주면 칸이 모자람
 */
public enum GiveResult {
    GIVEN,
    RECEIVER_NO_ROOM,
    RECEIVER_BAG_LIMIT,
    RECEIVER_BRACELET_LIMIT,
    GIVER_BAG_IN_USE,
    GIVER_BRACELET_IN_USE
}
