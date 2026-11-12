package mudmk3.domain.room;

import mudmk3.application.room.provided.RoomSaveRequest;
import mudmk3.domain.zone.Zone;

public class RoomFixture {

    public static RoomSaveRequest createRoomSaveRequest(Long zoneId, String name) {
        return new RoomSaveRequest(zoneId, name, name + "의 묘사다.");
    }

    public static RoomSaveInfo createRoomSaveInfo(String name) {
        return new RoomSaveInfo(name, name + "의 묘사다.");
    }

    public static Room createRoom(Zone zone, String name) {
        return Room.create(zone, createRoomSaveInfo(name));
    }
}
