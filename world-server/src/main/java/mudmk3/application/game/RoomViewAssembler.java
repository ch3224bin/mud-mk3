package mudmk3.application.game;

import mudmk3.application.game.required.GameMessage.RoomMessage;
import mudmk3.application.game.required.GameMessage.RoomMessage.FloorItemView;
import mudmk3.application.game.required.GameMessage.RoomMessage.OccupantView;
import mudmk3.application.game.required.GameMessage.RoomMessage.OwnerView;
import mudmk3.application.game.required.GameMessage.RoomMessage.RoomView;
import mudmk3.application.game.required.GameMessage.RoomMessage.ZoneView;
import mudmk3.domain.faction.Faction;
import mudmk3.domain.floor.FloorStack;
import mudmk3.domain.game.OnlinePlayer;
import mudmk3.domain.npc.Npc;
import mudmk3.domain.room.Direction;
import mudmk3.domain.room.Exit;
import mudmk3.domain.room.Room;
import mudmk3.domain.zone.Zone;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/** 방과 그 방에 있는 사람·NPC·바닥 물건으로 방 보기 메시지를 만든다(트랜잭션 안에서 부른다) */
final class RoomViewAssembler {

    private RoomViewAssembler() {
    }

    /**
     * viewer 는 방 사람 목록에서 뺀다. occupants 는 들어온 순서, npcs 는 만든 순서(id 순)여야 한다.
     * floorStacks 는 방 보기 묶음 순서(D40)여야 한다. 사람 다음에 NPC를 놓고, 바닥 물건은 따로 맨 끝 목록이다
     */
    static RoomMessage assemble(
            Room room, boolean zoneEntered, List<OnlinePlayer> occupants, List<Npc> npcs, List<FloorStack> floorStacks,
            Long viewerCharacterId) {
        Zone zone = room.getZone();
        return new RoomMessage(
                new ZoneView(requireNonNull(zone.getId()), zone.getName()),
                zoneEntered,
                new RoomView(requireNonNull(room.getId()), room.getName(), room.getDescription()),
                zone.findOwner().map(RoomViewAssembler::ownerView).orElse(null),
                room.getExits().stream()
                        .map(Exit::getDirection)
                        .map(Direction::koreanName)
                        .toList(),
                Stream.concat(
                        occupants.stream()
                                .filter(player -> !player.getCharacterId().equals(viewerCharacterId))
                                .map(player -> OccupantView.player(player.getName())),
                        npcs.stream()
                                .map(npc -> OccupantView.npc(npc.getName()))
                ).toList(),
                floorStacks.stream()
                        .map(stack -> new FloorItemView(stack.itemType().getName(), stack.count()))
                        .toList());
    }

    private static OwnerView ownerView(Faction owner) {
        String kind = switch (owner.getKind()) {
            case CASTLE -> "castle";
            case SECT -> "sect";
        };
        return new OwnerView(kind, owner.getName());
    }
}
