package mudmk3.application.item.required;

import mudmk3.domain.item.ItemType;
import mudmk3.domain.item.SpiritStoneGrade;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.time.Instant;
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

    /** 그 종류가 든 시작 소지품 줄 수 */
    @Query("select count(w) from WorldSettings w join w.startingItems s where s.itemTypeId = :itemTypeId")
    long countInStartingItems(Long itemTypeId);

    /** 그 종류를 둔 바닥 배치 수(남은 수가 0이어도 센다) */
    @Query("select count(p) from FloorPlacement p where p.itemType.id = :itemTypeId")
    long countInFloorPlacements(Long itemTypeId);

    /** 그 시각에 아직 사라지지 않은 그 종류의 버린 물건 수 */
    @Query("select count(d) from DroppedItem d where d.itemType.id = :itemTypeId and d.expiresAt > :now")
    long countInDroppedItems(Long itemTypeId, Instant now);

    /**
     * 그 시각에 사라진 그 종류의 버린 물건 행을 지운다. 종류를 지우기 전에 부른다(남은 행이 외래 키로 막지 않게).
     * 지운 행이 영속성 컨텍스트에 남아 flush를 막지 않도록 먼저 flush하고 끝나면 컨텍스트를 비운다
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from DroppedItem d where d.itemType.id = :itemTypeId and d.expiresAt <= :now")
    void deleteExpiredDroppedItems(Long itemTypeId, Instant now);
}
