package mudmk3.application.inventory.required;

import mudmk3.domain.inventory.Inventory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

import java.util.Optional;

/** 캐릭터 소지품을 저장하고 찾는다 */
public interface InventoryRepository extends Repository<Inventory, Long> {

    Inventory save(Inventory inventory);

    /** 묶음과 그 물건 종류까지 읽는다(트랜잭션 밖에서 표시해도 되게) */
    @EntityGraph(attributePaths = {"entries", "entries.itemType"})
    Optional<Inventory> findByCharacterId(Long characterId);
}
