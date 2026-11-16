package mudmk3.application.game;

import mudmk3.application.game.required.GameMessage.RoomMessage;
import mudmk3.application.game.required.GameMessage.RoomMessage.CorpseView;
import mudmk3.application.game.required.GameMessage.RoomMessage.FloorItemView;
import mudmk3.application.game.required.GameMessage.RoomMessage.MobView;
import mudmk3.application.game.required.GameMessage.RoomMessage.OccupantView;
import mudmk3.application.game.required.GameMessage.RoomMessage.OwnerView;
import mudmk3.application.game.required.GameMessage.RoomMessage.RoomView;
import mudmk3.application.game.required.GameMessage.RoomMessage.ZoneView;
import mudmk3.domain.combat.Corpse;
import mudmk3.domain.combat.MobInstance;
import mudmk3.domain.faction.Faction;
import mudmk3.domain.floor.FloorStack;
import mudmk3.domain.game.OnlinePlayer;
import mudmk3.domain.npc.Npc;
import mudmk3.domain.room.Direction;
import mudmk3.domain.room.Exit;
import mudmk3.domain.room.Room;
import mudmk3.domain.shared.Realm;
import mudmk3.domain.zone.Zone;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/** 방과 그 방에 있는 사람·NPC·몹·시체·바닥 물건으로 방 보기 메시지를 만든다(트랜잭션 안에서 부른다) */
final class RoomViewAssembler {

    private RoomViewAssembler() {
    }

    /**
     * viewer 는 방 사람 목록에서 뺀다. occupants 는 들어온 순서, npcs 는 만든 순서(id 순)여야 한다.
     * floorStacks 는 방 보기 묶음 순서(D40)여야 한다. 사람 다음에 NPC를 놓고, 몹·시체·바닥 물건은 따로 목록이다.
     * mobs 는 그 방의 살아 있는 몹(방 보기 순서, {@link CombatEngine#mobsInRoom}), corpses 는 시체(쓰러진 순)다.
     * 몹은 묶음 키({@link MobGroupKey})가 같으면 한 줄로 묶고, 급·경지는 보는 사람의 경지로 정한다(D89)
     */
    static RoomMessage assemble(
            Room room, boolean zoneEntered, List<OnlinePlayer> occupants, List<Npc> npcs, List<FloorStack> floorStacks,
            Long viewerCharacterId, List<MobInstance> mobs, List<Corpse> corpses, Realm viewerRealm, Instant now) {
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
                mobViews(mobs, viewerRealm),
                corpses.stream()
                        .map(corpse -> new CorpseView(corpse.name(), corpse.collectibleBy(viewerCharacterId, now)))
                        .toList(),
                floorStacks.stream()
                        .map(stack -> new FloorItemView(stack.itemType().getName(), stack.count()))
                        .toList());
    }

    private static List<MobView> mobViews(List<MobInstance> mobs, Realm viewerRealm) {
        Map<MobGroupKey, List<MobInstance>> groups = new LinkedHashMap<>();
        for (MobInstance mob : mobs) {
            groups.computeIfAbsent(MobGroupKey.of(mob), key -> new ArrayList<>()).add(mob);
        }
        return groups.entrySet().stream()
                .map(group -> {
                    MobInstance first = group.getValue().getFirst();
                    return new MobView(first.getProfile().name(), CombatTexts.rank(first.getProfile(), viewerRealm),
                            first.getProfile().tier().badge(), group.getValue().size(), group.getKey().fighting());
                })
                .toList();
    }

    private static OwnerView ownerView(Faction owner) {
        String kind = switch (owner.getKind()) {
            case CASTLE -> "castle";
            case SECT -> "sect";
        };
        return new OwnerView(kind, owner.getName());
    }
}
