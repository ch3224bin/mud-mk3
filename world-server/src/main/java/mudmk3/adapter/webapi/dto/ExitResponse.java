package mudmk3.adapter.webapi.dto;

import mudmk3.domain.room.Direction;
import mudmk3.domain.room.Exit;
import mudmk3.domain.room.Room;

public record ExitResponse(
        Direction direction,
        String directionName,
        Long targetRoomId,
        String targetRoomName,
        String targetZoneName
) {

    public static ExitResponse of(Exit exit) {
        Room target = exit.getTarget();
        return new ExitResponse(exit.getDirection(), exit.getDirection().koreanName(),
                target.getId(), target.getName(), target.getZone().getName());
    }
}
