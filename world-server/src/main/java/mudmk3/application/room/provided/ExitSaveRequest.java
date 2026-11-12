package mudmk3.application.room.provided;

import jakarta.validation.constraints.NotNull;

/** 출구를 두는 요청. twoWay면 대상 방에 반대 방위 출구도 둔다 */
public record ExitSaveRequest(@NotNull Long targetRoomId, boolean twoWay) {
}
