package mudmk3.application.world;

import lombok.RequiredArgsConstructor;
import mudmk3.application.item.provided.ItemTypeFinder;
import mudmk3.application.room.provided.RoomFinder;
import mudmk3.application.world.provided.StartingItemRequest;
import mudmk3.application.world.provided.WorldSettingsEditor;
import mudmk3.application.world.required.WorldSettingsRepository;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.item.Slots;
import mudmk3.domain.room.Room;
import mudmk3.domain.shared.Numbers;
import mudmk3.domain.world.StartingItem;
import mudmk3.domain.world.WorldSettings;
import mudmk3.support.stereotype.ValidatedApplicationService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ValidatedApplicationService
@RequiredArgsConstructor
public class WorldSettingsModifyService implements WorldSettingsEditor {

    private final WorldSettingsRepository worldSettingsRepository;
    private final RoomFinder roomFinder;
    private final ItemTypeFinder itemTypeFinder;

    @Override
    public WorldSettings designateStartRoom(Long roomId) {
        Room room = roomFinder.find(roomId);
        WorldSettings settings = findOrCreate();
        settings.designateStartRoom(room);
        return worldSettingsRepository.save(settings);
    }

    @Override
    public List<StartingItem> replaceStartingItems(List<StartingItemRequest> items) {
        List<StartingItem> startingItems = items.stream().map(StartingItemRequest::toStartingItem).toList();
        ensureFitsNewInventory(startingItems);
        WorldSettings settings = findOrCreate();
        settings.replaceStartingItems(startingItems);
        // 요소 컬렉션은 LAZY라 트랜잭션 안에서 복사해 돌려준다
        return List.copyOf(worldSettingsRepository.save(settings).getStartingItems());
    }

    private WorldSettings findOrCreate() {
        return worldSettingsRepository.findFirstByOrderByIdAsc().orElseGet(WorldSettings::create);
    }

    /**
     * 빈 소지품(기본 2칸)에 다 넣을 수 있는지 줄 순서대로 본다: 없는 물건·같은 물건 두 번 → 저물대 네 개 이상 → 칸 넘침.
     * 칸은 소지품과 같은 규칙({@link Slots})으로 센다
     */
    private void ensureFitsNewInventory(List<StartingItem> startingItems) {
        Set<Long> seen = new HashSet<>();
        long usedSlots = 0;
        long bags = 0;
        long bagSlots = 0;
        for (StartingItem startingItem : startingItems) {
            ItemType itemType = itemTypeFinder.find(startingItem.itemTypeId());
            if (!seen.add(startingItem.itemTypeId())) {
                throw new IllegalArgumentException("같은 물건이 두 번 있습니다: " + itemType.getName());
            }
            usedSlots += Slots.slotsFor(itemType, startingItem.count());
            if (itemType.isBag()) {
                bags += startingItem.count();
                bagSlots += (long) startingItem.count() * itemType.bagSlots();
            }
        }
        if (bags > Slots.MAX_BAGS) {
            throw new IllegalArgumentException("저물대는 세 개까지만 찰 수 있습니다.");
        }
        int capacity = Slots.capacity(Math.toIntExact(bagSlots));
        if (usedSlots > capacity) {
            throw new IllegalArgumentException("시작 소지품이 칸을 넘습니다. (필요 %s칸, 칸 %s)"
                    .formatted(Numbers.count(usedSlots), Numbers.count(capacity)));
        }
    }
}
