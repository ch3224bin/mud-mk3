package mudmk3.application.room;

import lombok.RequiredArgsConstructor;
import mudmk3.application.room.provided.RoomFinder;
import mudmk3.application.room.required.RoomRepository;
import mudmk3.application.zone.provided.ZoneFinder;
import mudmk3.domain.room.Room;
import mudmk3.support.stereotype.ApplicationService;

import java.util.List;
import java.util.Optional;

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
    public List<Room> findAllInZone(Long zoneId) {
        return roomRepository.findAllByZoneOrderByIdAsc(zoneFinder.find(zoneId));
    }
}
