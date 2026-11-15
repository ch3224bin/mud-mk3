package mudmk3.application.mob;

import lombok.RequiredArgsConstructor;
import mudmk3.application.item.provided.ItemTypeFinder;
import mudmk3.application.mob.provided.MobTypeFinder;
import mudmk3.application.mob.provided.MobTypeView;
import mudmk3.application.mob.provided.MobTypeView.PlacementView;
import mudmk3.application.mob.required.MobPlacementRepository;
import mudmk3.application.mob.required.MobTypeRepository;
import mudmk3.application.room.provided.RoomFinder;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.mob.MobFixedDrop;
import mudmk3.domain.mob.MobPlacement;
import mudmk3.domain.mob.MobTierDrop;
import mudmk3.domain.mob.MobType;
import mudmk3.support.stereotype.ApplicationService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;

@ApplicationService
@RequiredArgsConstructor
public class MobTypeQueryService implements MobTypeFinder {

    private final MobTypeRepository mobTypeRepository;
    private final MobPlacementRepository mobPlacementRepository;
    private final ItemTypeFinder itemTypeFinder;
    private final RoomFinder roomFinder;

    @Override
    public MobType find(Long mobTypeId) {
        return mobTypeRepository.findById(mobTypeId)
                .orElseThrow(() -> new IllegalArgumentException("몹을 찾을 수 없습니다. id: " + mobTypeId));
    }

    @Override
    public MobTypeView view(Long mobTypeId) {
        MobType mobType = find(mobTypeId);
        return toView(mobType, itemNames(List.of(mobType)), placements(List.of(mobType)));
    }

    @Override
    public List<MobTypeView> viewAll() {
        List<MobType> mobTypes = mobTypeRepository.findAllByOrderByNameAsc();
        Map<Long, String> itemNames = itemNames(mobTypes);
        Map<Long, List<PlacementView>> placements = placements(mobTypes);
        return mobTypes.stream()
                .map(mobType -> toView(mobType, itemNames, placements))
                .toList();
    }

    private MobTypeView toView(MobType mobType, Map<Long, String> itemNames,
                               Map<Long, List<PlacementView>> placements) {
        return MobTypeView.of(mobType, itemNames, placements.getOrDefault(mobType.getId(), List.of()));
    }

    /** 몹 유형 id → 그 몹을 둔 배치(배치 id 순, 방 이름까지). 방 이름은 방마다 한 번 읽는다 */
    private Map<Long, List<PlacementView>> placements(List<MobType> mobTypes) {
        List<Long> mobTypeIds = mobTypes.stream().map(mobType -> requireNonNull(mobType.getId())).toList();
        Map<Long, String> roomNames = new HashMap<>();
        Map<Long, List<PlacementView>> placements = new HashMap<>();
        for (MobPlacement placement : mobPlacementRepository.findAllByMobTypeIdInOrderByIdAsc(mobTypeIds)) {
            String roomName = roomNames.computeIfAbsent(placement.getRoomId(),
                    roomId -> roomFinder.find(roomId).getName());
            placements.computeIfAbsent(requireNonNull(placement.getMobType().getId()), id -> new ArrayList<>())
                    .add(new PlacementView(requireNonNull(placement.getId()), placement.getRoomId(), roomName,
                            placement.getCount()));
        }
        return placements;
    }

    /** 몹들의 드롭 표 물건 이름을 한 번에 읽는다 */
    private Map<Long, String> itemNames(List<MobType> mobTypes) {
        Set<Long> ids = new LinkedHashSet<>();
        for (MobType mobType : mobTypes) {
            mobType.getFixedDrops().stream().map(MobFixedDrop::itemTypeId).forEach(ids::add);
            mobType.getTierDrops().stream().map(MobTierDrop::itemTypeId).forEach(ids::add);
        }
        Map<Long, String> names = new LinkedHashMap<>();
        for (Map.Entry<Long, ItemType> entry : itemTypeFinder.findAllById(ids).entrySet()) {
            names.put(entry.getKey(), entry.getValue().getName());
        }
        return names;
    }
}
