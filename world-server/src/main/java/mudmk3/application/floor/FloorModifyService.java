package mudmk3.application.floor;

import lombok.RequiredArgsConstructor;
import mudmk3.application.floor.provided.FloorPlacementEditor;
import mudmk3.application.floor.provided.FloorPlacementFinder;
import mudmk3.application.floor.provided.FloorPlacementSaveRequest;
import mudmk3.application.floor.provided.RoomFloor;
import mudmk3.application.floor.required.DroppedItemRepository;
import mudmk3.application.floor.required.FloorPlacementRepository;
import mudmk3.application.item.provided.ItemTypeFinder;
import mudmk3.application.room.provided.RoomDeletedEvent;
import mudmk3.application.room.provided.RoomFinder;
import mudmk3.domain.floor.DroppedItem;
import mudmk3.domain.floor.FloorPlacement;
import mudmk3.domain.floor.FloorStack;
import mudmk3.domain.floor.FloorStacks;
import mudmk3.domain.item.ItemType;
import mudmk3.support.stereotype.ValidatedApplicationService;
import org.springframework.context.event.EventListener;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@ValidatedApplicationService
@RequiredArgsConstructor
public class FloorModifyService implements FloorPlacementEditor, RoomFloor {

    private final FloorPlacementFinder floorPlacementFinder;
    private final FloorPlacementRepository floorPlacementRepository;
    private final DroppedItemRepository droppedItemRepository;
    private final RoomFinder roomFinder;
    private final ItemTypeFinder itemTypeFinder;
    private final Clock clock;

    @Override
    public FloorPlacement create(Long roomId, FloorPlacementSaveRequest request) {
        roomFinder.find(roomId);
        ItemType itemType = itemTypeFinder.find(request.itemTypeId());
        return floorPlacementRepository.save(FloorPlacement.create(roomId, itemType, request.toInfo()));
    }

    @Override
    public FloorPlacement update(Long floorPlacementId, FloorPlacementSaveRequest request) {
        FloorPlacement placement = floorPlacementFinder.find(floorPlacementId);
        ItemType itemType = itemTypeFinder.find(request.itemTypeId());
        placement.update(itemType, request.toInfo());
        return floorPlacementRepository.save(placement);
    }

    @Override
    public void delete(Long floorPlacementId) {
        floorPlacementRepository.delete(floorPlacementFinder.find(floorPlacementId));
    }

    @Override
    public List<FloorStack> look(Long roomId) {
        Instant now = clock.instant();
        Floor floor = read(roomId, now);
        return FloorStacks.of(floor.placements(), floor.droppedItems(), now);
    }

    @Override
    public int take(Long roomId, Long itemTypeId, int count) {
        if (count < 1) throw new IllegalArgumentException("수는 1 이상이어야 합니다: " + count);
        Instant now = clock.instant();
        Floor floor = read(roomId, now);
        int left = count;
        for (DroppedItem dropped : floor.droppedItems()) {
            if (left == 0) break;
            if (!itemTypeId.equals(dropped.getItemType().getId())) continue;
            int taken = Math.min(left, dropped.getCount());
            dropped.take(taken);
            if (dropped.isEmpty()) droppedItemRepository.delete(dropped);
            left -= taken;
        }
        for (FloorPlacement placement : floor.placements()) {
            if (left == 0) break;
            if (!itemTypeId.equals(placement.getItemType().getId())) continue;
            int taken = Math.min(left, placement.availableAt(now));
            if (taken == 0) continue;
            placement.take(taken, now);
            left -= taken;
        }
        return count - left;
    }

    @Override
    public void drop(Long roomId, Long itemTypeId, int count) {
        ItemType itemType = itemTypeFinder.find(itemTypeId);
        droppedItemRepository.save(DroppedItem.drop(roomId, itemType, count, clock.instant()));
    }

    /** 방이 지워지면 그 방의 배치·버린 물건을 지운다(방을 지우는 트랜잭션 안에서) */
    @EventListener
    public void onRoomDeleted(RoomDeletedEvent event) {
        floorPlacementRepository.deleteByRoomId(event.roomId());
        droppedItemRepository.deleteByRoomId(event.roomId());
    }

    /** 바닥을 읽을 때 시간을 잰다: 사라진 버린 물건을 지우고 재등장할 배치를 채운다(서버 틱 없음, D39) */
    private Floor read(Long roomId, Instant now) {
        droppedItemRepository.deleteByRoomIdAndExpiresAtLessThanEqual(roomId, now);
        List<FloorPlacement> placements = floorPlacementRepository.findAllByRoomIdOrderByIdAsc(roomId);
        placements.forEach(placement -> placement.refresh(now));
        return new Floor(placements, droppedItemRepository.findAllByRoomIdOrderByIdAsc(roomId));
    }

    private record Floor(List<FloorPlacement> placements, List<DroppedItem> droppedItems) {
    }
}
