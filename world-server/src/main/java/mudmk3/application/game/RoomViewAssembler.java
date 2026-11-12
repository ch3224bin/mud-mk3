package mudmk3.application.game;

import mudmk3.application.game.required.GameMessage.RoomMessage;
import mudmk3.application.game.required.GameMessage.RoomMessage.OccupantView;
import mudmk3.application.game.required.GameMessage.RoomMessage.RoomView;
import mudmk3.application.game.required.GameMessage.RoomMessage.ZoneView;
import mudmk3.domain.game.OnlinePlayer;
import mudmk3.domain.room.Direction;
import mudmk3.domain.room.Exit;
import mudmk3.domain.room.Room;
import mudmk3.domain.zone.Zone;

import java.util.List;

import static java.util.Objects.requireNonNull;

/** 방과 그 방에 있는 사람으로 방 보기 메시지를 만든다(트랜잭션 안에서 부른다) */
final class RoomViewAssembler {

    private RoomViewAssembler() {
    }

    /** viewer 는 방 사람 목록에서 뺀다. occupants 는 들어온 순서여야 한다 */
    static RoomMessage assemble(Room room, boolean zoneEntered, List<OnlinePlayer> occupants, Long viewerCharacterId) {
        Zone zone = room.getZone();
        return new RoomMessage(
                new ZoneView(requireNonNull(zone.getId()), zone.getName()),
                zoneEntered,
                new RoomView(requireNonNull(room.getId()), room.getName(), room.getDescription()),
                room.getExits().stream()
                        .map(Exit::getDirection)
                        .map(Direction::koreanName)
                        .toList(),
                occupants.stream()
                        .filter(player -> !player.getCharacterId().equals(viewerCharacterId))
                        .map(player -> OccupantView.player(player.getName()))
                        .toList());
    }
}
