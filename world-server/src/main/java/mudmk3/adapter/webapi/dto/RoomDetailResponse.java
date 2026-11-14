package mudmk3.adapter.webapi.dto;

import mudmk3.domain.room.Room;

import java.util.List;

/** 방 하나의 자세한 정보. exits는 방위 순서, npcs는 그 방에 배치된 NPC(id 순) */
public record RoomDetailResponse(
        Long id,
        Long zoneId,
        String zoneName,
        String name,
        String description,
        boolean start,
        List<ExitResponse> exits,
        List<RoomNpcResponse> npcs
) {

    public static RoomDetailResponse of(Room room, boolean start, List<RoomNpcResponse> npcs) {
        return new RoomDetailResponse(room.getId(), room.getZone().getId(), room.getZone().getName(),
                room.getName(), room.getDescription(), start,
                room.getExits().stream().map(ExitResponse::of).toList(), npcs);
    }
}
