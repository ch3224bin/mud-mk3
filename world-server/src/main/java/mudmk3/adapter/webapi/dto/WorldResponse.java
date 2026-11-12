package mudmk3.adapter.webapi.dto;

import mudmk3.domain.room.Room;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/** 세계 설정. 시작 방이 없으면 세 값 모두 null */
public record WorldResponse(
        @Nullable Long startRoomId,
        @Nullable String startRoomName,
        @Nullable String startZoneName
) {

    public static WorldResponse of(Optional<Room> startRoom) {
        return startRoom
                .map(room -> new WorldResponse(room.getId(), room.getName(), room.getZone().getName()))
                .orElseGet(() -> new WorldResponse(null, null, null));
    }
}
