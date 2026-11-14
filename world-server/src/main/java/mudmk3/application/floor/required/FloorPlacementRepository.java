package mudmk3.application.floor.required;

import mudmk3.domain.floor.FloorPlacement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/** 관리자가 둔 바닥 물건을 저장하고 찾는다. 물건 종류는 트랜잭션 밖에서도 읽을 수 있게 함께 읽는다 */
public interface FloorPlacementRepository extends Repository<FloorPlacement, Long> {

    FloorPlacement save(FloorPlacement placement);

    @EntityGraph(attributePaths = "itemType")
    Optional<FloorPlacement> findById(Long id);

    /** 그 방의 배치(id 순) */
    @EntityGraph(attributePaths = "itemType")
    List<FloorPlacement> findAllByRoomIdOrderByIdAsc(Long roomId);

    void delete(FloorPlacement placement);

    void deleteByRoomId(Long roomId);
}
