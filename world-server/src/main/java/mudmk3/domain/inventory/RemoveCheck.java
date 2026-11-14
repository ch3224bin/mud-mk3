package mudmk3.domain.inventory;

/** 물건을 뺄 수 있는지: 뺄 수 있음, 저물대를 빼면 쓰는 칸보다 칸이 모자람 */
public enum RemoveCheck {
    OK,
    BAG_IN_USE
}
