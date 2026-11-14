package mudmk3.application.floor.required;

import mudmk3.domain.floor.DroppedItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

import java.time.Instant;
import java.util.List;

/** 버린 물건을 저장하고 찾는다. 물건 종류는 함께 읽는다 */
public interface DroppedItemRepository extends Repository<DroppedItem, Long> {

    DroppedItem save(DroppedItem droppedItem);

    /** 그 방의 버린 물건(버린 순서 = id 순) */
    @EntityGraph(attributePaths = "itemType")
    List<DroppedItem> findAllByRoomIdOrderByIdAsc(Long roomId);

    void delete(DroppedItem droppedItem);

    void deleteByRoomId(Long roomId);

    /** 그 방에서 그 시각에 사라진 버린 물건을 지운다(사라지는 시각 포함) */
    void deleteByRoomIdAndExpiresAtLessThanEqual(Long roomId, Instant now);
}
