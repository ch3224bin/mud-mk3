package mudmk3.application.floor.provided;

import mudmk3.domain.floor.FloorStack;

import java.util.List;

/** 게임에서 방 바닥 물건을 보고 줍고 버린다 */
public interface RoomFloor {

    /** 그 방의 사라진 버린 물건을 지우고 재등장할 배치를 채운 뒤, 방 보기 묶음(D40 순서)을 돌려준다 */
    List<FloorStack> look(Long roomId);

    /**
     * 그 방 바닥에서 그 종류를 최대 {@code count}(1 이상) 개 덜어 내고 덜어 낸 수를 돌려준다.
     * 버린 것(오래된 것부터)에서 먼저, 다음 배치(id 순)에서 덜어 낸다
     */
    int take(Long roomId, Long itemTypeId, int count);

    /** 그 방 바닥에 버린다(지금부터 5분) */
    void drop(Long roomId, Long itemTypeId, int count);
}
