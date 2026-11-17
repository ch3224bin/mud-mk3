package mudmk3.application.room;

import lombok.RequiredArgsConstructor;
import mudmk3.application.room.provided.RoomFinder;
import mudmk3.application.room.required.RoomRepository;
import mudmk3.application.zone.provided.ZoneFinder;
import mudmk3.domain.room.Room;
import mudmk3.support.stereotype.ApplicationService;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@ApplicationService
@RequiredArgsConstructor
public class RoomQueryService implements RoomFinder {

    private final RoomRepository roomRepository;
    private final ZoneFinder zoneFinder;

    @Override
    public Room find(Long roomId) {
        return findOptional(roomId)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다. id: " + roomId));
    }

    @Override
    public Optional<Room> findOptional(Long roomId) {
        return roomRepository.findById(roomId);
    }

    @Override
    public Map<Long, Room> findAllById(Collection<Long> roomIds) {
        Map<Long, Room> rooms = new HashMap<>();
        if (roomIds.isEmpty()) return rooms;
        for (Room room : roomRepository.findWithZoneByIdIn(roomIds)) {
            rooms.put(requireNonNull(room.getId()), room);
        }
        return rooms;
    }

    @Override
    public Map<Long, String> findNames(Collection<Long> roomIds) {
        Map<Long, String> names = new HashMap<>();
        if (roomIds.isEmpty()) return names;
        for (Room room : roomRepository.findAllByIdIn(roomIds)) {
            names.put(requireNonNull(room.getId()), room.getName());
        }
        return names;
    }

    @Override
    public List<Room> findAllInZone(Long zoneId) {
        return roomRepository.findAllByZoneOrderByIdAsc(zoneFinder.find(zoneId));
    }
}
