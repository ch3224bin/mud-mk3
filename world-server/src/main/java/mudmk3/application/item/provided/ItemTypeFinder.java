package mudmk3.application.item.provided;

import mudmk3.domain.item.ItemType;
import mudmk3.domain.item.SpiritStoneGrade;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** 아이템 종류를 찾는다 */
public interface ItemTypeFinder {

    /** 없으면 {@code IllegalArgumentException("물건 종류를 찾을 수 없습니다. id: " + id)} */
    ItemType find(Long itemTypeId);

    /** 영석(하품 → 극품) 다음 나머지를 이름순으로 */
    List<ItemType> findAll();

    /** 있는 것만 id로 묶는다(없는 id는 빠진다) */
    Map<Long, ItemType> findAllById(Collection<Long> itemTypeIds);

    /** 그 등급의 영석 종류. 서버가 뜰 때 심으므로 없으면 {@code IllegalStateException} */
    ItemType spiritStone(SpiritStoneGrade grade);
}
