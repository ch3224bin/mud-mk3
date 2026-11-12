package mudmk3.application.room;

import lombok.RequiredArgsConstructor;
import mudmk3.application.room.provided.ExitConflictException;
import mudmk3.application.room.provided.ExitSaveRequest;
import mudmk3.application.room.provided.RoomDeletedEvent;
import mudmk3.application.room.provided.RoomEditor;
import mudmk3.application.room.provided.RoomFinder;
import mudmk3.application.room.provided.RoomSaveRequest;
import mudmk3.application.room.provided.StartRoomDeletionException;
import mudmk3.application.room.required.RoomRepository;
import mudmk3.application.room.required.StartRoomChecker;
import mudmk3.application.zone.provided.ZoneFinder;
import mudmk3.domain.room.Direction;
import mudmk3.domain.room.Exit;
import mudmk3.domain.room.Room;
import mudmk3.domain.zone.Zone;
import mudmk3.support.stereotype.ValidatedApplicationService;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

@ValidatedApplicationService
@RequiredArgsConstructor
public class RoomModifyService implements RoomEditor {

    private final RoomFinder roomFinder;
    private final RoomRepository roomRepository;
    private final ZoneFinder zoneFinder;
    private final StartRoomChecker startRoomChecker;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Room create(RoomSaveRequest request) {
        Zone zone = zoneFinder.find(request.zoneId());
        return roomRepository.save(Room.create(zone, request.toInfo()));
    }

    @Override
    public Room update(Long roomId, RoomSaveRequest request) {
        Room room = roomFinder.find(roomId);
        Zone zone = zoneFinder.find(request.zoneId());
        room.update(zone, request.toInfo());
        return roomRepository.save(room);
    }

    @Override
    public void delete(Long roomId) {
        if (startRoomChecker.isStartRoom(roomId)) {
            throw new StartRoomDeletionException("시작 방은 지울 수 없습니다");
        }
        Room room = roomFinder.find(roomId);
        for (Room source : roomRepository.findAllWithExitTo(room)) {
            source.getExits().stream()
                    .filter(exit -> exit.getTarget().equals(room))
                    .map(Exit::getDirection)
                    .forEach(source::removeExit);
        }
        // 들어오는 출구(외래 키)를 먼저 지워야 방을 지울 수 있다
        roomRepository.flush();
        roomRepository.delete(room);
        eventPublisher.publishEvent(new RoomDeletedEvent(roomId));
    }

    @Override
    public Room putExit(Long roomId, Direction direction, ExitSaveRequest request) {
        Room room = roomFinder.find(roomId);
        Room target = roomFinder.findOptional(request.targetRoomId())
                .orElseThrow(() -> new IllegalArgumentException("대상 방을 찾을 수 없습니다. id: " + request.targetRoomId()));
        Direction opposite = direction.opposite();
        // 자기 자신으로 가는 출구는 아래 setExit 가 막는다(그 경우 반대 출구 충돌은 따지지 않는다)
        boolean twoWay = request.twoWay() && !target.equals(room);
        if (twoWay) {
            ensureNoBackExitConflict(room, target, opposite);
        }

        room.setExit(direction, target);
        if (twoWay && target.findExit(opposite).isEmpty()) {
            target.setExit(opposite, room);
        }
        return room;
    }

    @Override
    public void deleteExit(Long roomId, Direction direction, boolean twoWay) {
        Room room = roomFinder.find(roomId);
        Optional<Room> target = room.findExit(direction).map(Exit::getTarget);
        room.removeExit(direction);
        if (!twoWay || target.isEmpty()) return;

        Direction opposite = direction.opposite();
        boolean backExitToThisRoom = target.get().findExit(opposite)
                .filter(exit -> exit.getTarget().equals(room))
                .isPresent();
        if (backExitToThisRoom) {
            target.get().removeExit(opposite);
        }
    }

    private static void ensureNoBackExitConflict(Room room, Room target, Direction opposite) {
        target.findExit(opposite)
                .filter(exit -> !exit.getTarget().equals(room))
                .ifPresent(exit -> {
                    throw new ExitConflictException(
                            "반대 방향 출구가 이미 다른 방으로 이어져 있습니다: " + target.getName() + " " + opposite.koreanName());
                });
    }
}
