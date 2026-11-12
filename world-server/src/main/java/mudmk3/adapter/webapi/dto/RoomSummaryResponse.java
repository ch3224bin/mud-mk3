package mudmk3.adapter.webapi.dto;

import mudmk3.domain.room.Room;

public record RoomSummaryResponse(Long id, String name, int exitCount) {

    public static RoomSummaryResponse of(Room room) {
        return new RoomSummaryResponse(room.getId(), room.getName(), room.getExits().size());
    }
}
