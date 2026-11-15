package mudmk3.application.mob;

import lombok.RequiredArgsConstructor;
import mudmk3.application.item.provided.ItemTypeFinder;
import mudmk3.application.mob.provided.MobTypeFinder;
import mudmk3.application.mob.provided.MobTypeView;
import mudmk3.application.mob.required.MobTypeRepository;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.mob.MobFixedDrop;
import mudmk3.domain.mob.MobTierDrop;
import mudmk3.domain.mob.MobType;
import mudmk3.support.stereotype.ApplicationService;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationService
@RequiredArgsConstructor
public class MobTypeQueryService implements MobTypeFinder {

    private final MobTypeRepository mobTypeRepository;
    private final ItemTypeFinder itemTypeFinder;

    @Override
    public MobType find(Long mobTypeId) {
        return mobTypeRepository.findById(mobTypeId)
                .orElseThrow(() -> new IllegalArgumentException("몹을 찾을 수 없습니다. id: " + mobTypeId));
    }

    @Override
    public MobTypeView view(Long mobTypeId) {
        MobType mobType = find(mobTypeId);
        return toView(mobType, itemNames(List.of(mobType)));
    }

    @Override
    public List<MobTypeView> viewAll() {
        List<MobType> mobTypes = mobTypeRepository.findAllByOrderByNameAsc();
        Map<Long, String> itemNames = itemNames(mobTypes);
        return mobTypes.stream()
                .map(mobType -> toView(mobType, itemNames))
                .toList();
    }

    /** 배치된 방은 몹 배치가 생기는 태스크가 채운다 */
    private MobTypeView toView(MobType mobType, Map<Long, String> itemNames) {
        return MobTypeView.of(mobType, itemNames, List.of());
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
