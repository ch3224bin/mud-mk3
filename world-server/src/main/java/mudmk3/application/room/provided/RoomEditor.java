package mudmk3.application.room.provided;

import jakarta.validation.Valid;
import mudmk3.domain.room.Direction;
import mudmk3.domain.room.Room;

/** 방과 출구를 만들고 고치고 지우는 기능을 제공한다 */
public interface RoomEditor {

    Room create(@Valid RoomSaveRequest request);

    Room update(Long roomId, @Valid RoomSaveRequest request);

    /** 방과 그 방으로 오는 출구를 지우고 {@link RoomDeletedEvent} 를 낸다. 시작 방은 지울 수 없다 */
    void delete(Long roomId);

    /** 그 방위에 출구를 두거나 대상을 바꾼다. twoWay면 대상 방에 반대 방위 출구도 둔다 */
    Room putExit(Long roomId, Direction direction, @Valid ExitSaveRequest request);

    /** 그 방위 출구를 지운다. twoWay면 대상 방의 반대 출구가 이 방을 가리킬 때 그것도 지운다 */
    void deleteExit(Long roomId, Direction direction, boolean twoWay);
}
