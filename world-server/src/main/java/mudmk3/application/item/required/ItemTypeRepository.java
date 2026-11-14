package mudmk3.application.item.required;

import mudmk3.domain.item.ItemType;
import mudmk3.domain.item.SpiritStoneGrade;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 아이템 종류를 저장하고 찾는다. 쓰이는지는 쓰는 슬라이스에 기대지 않으려고 JPQL로 센다(쓰는 슬라이스가 생길 때 더한다) */
public interface ItemTypeRepository extends Repository<ItemType, Long> {

    ItemType save(ItemType itemType);

    Optional<ItemType> findById(Long id);

    List<ItemType> findAll();

    List<ItemType> findAllByIdIn(Collection<Long> ids);

    Optional<ItemType> findBySpiritStoneGrade(SpiritStoneGrade spiritStoneGrade);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    void delete(ItemType itemType);

    /** 그 종류가 든 소지품 묶음 수(수가 0인 묶음은 도메인이 빼므로 남지 않는다) */
    @Query("select count(e) from InventoryEntry e where e.itemType.id = :itemTypeId")
    long countInInventories(Long itemTypeId);
}
