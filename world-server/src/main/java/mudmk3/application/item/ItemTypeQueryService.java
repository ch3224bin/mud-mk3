package mudmk3.application.item;

import lombok.RequiredArgsConstructor;
import mudmk3.application.item.provided.ItemTypeFinder;
import mudmk3.application.item.required.ItemTypeRepository;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.item.SpiritStoneGrade;
import mudmk3.support.stereotype.ApplicationService;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationService
@RequiredArgsConstructor
public class ItemTypeQueryService implements ItemTypeFinder {

    /** 영석이 먼저(등급 순), 나머지는 이름순 */
    private static final Comparator<ItemType> LIST_ORDER = Comparator
            .comparing((ItemType itemType) -> itemType.findSpiritStoneGrade().map(Enum::ordinal).orElse(Integer.MAX_VALUE))
            .thenComparing(ItemType::getName);

    private final ItemTypeRepository itemTypeRepository;

    @Override
    public ItemType find(Long itemTypeId) {
        return itemTypeRepository.findById(itemTypeId)
                .orElseThrow(() -> new IllegalArgumentException("물건 종류를 찾을 수 없습니다. id: " + itemTypeId));
    }

    @Override
    public List<ItemType> findAll() {
        return itemTypeRepository.findAll().stream()
                .sorted(LIST_ORDER)
                .toList();
    }

    @Override
    public Map<Long, ItemType> findAllById(Collection<Long> itemTypeIds) {
        Map<Long, ItemType> result = new LinkedHashMap<>();
        if (itemTypeIds.isEmpty()) return result;
        for (ItemType itemType : itemTypeRepository.findAllByIdIn(itemTypeIds)) {
            result.put(itemType.getId(), itemType);
        }
        return result;
    }

    @Override
    public ItemType spiritStone(SpiritStoneGrade grade) {
        return itemTypeRepository.findBySpiritStoneGrade(grade)
                .orElseThrow(() -> new IllegalStateException("영석 종류가 없습니다: " + grade.itemName()));
    }
}
