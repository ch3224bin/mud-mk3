package mudmk3.application.mob.required;

import mudmk3.domain.mob.MobPlacement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 몹 배치를 저장하고 찾는다. 몹 유형은 트랜잭션 밖에서도 읽을 수 있게 함께 읽는다 */
public interface MobPlacementRepository extends Repository<MobPlacement, Long> {

    MobPlacement save(MobPlacement placement);

    @EntityGraph(attributePaths = "mobType")
    Optional<MobPlacement> findById(Long id);

    /** 모든 배치(id 순). 드롭 표 둘은 순서 열이 있는 리스트라 함께 읽어도 여러 bag 문제가 없다 */
    @EntityGraph(attributePaths = {"mobType", "mobType.fixedDrops", "mobType.tierDrops"})
    List<MobPlacement> findAllByOrderByIdAsc();

    /** 그 방의 배치(id 순) */
    @EntityGraph(attributePaths = "mobType")
    List<MobPlacement> findAllByRoomIdOrderByIdAsc(Long roomId);

    /** 그 몹 유형들을 둔 배치(id 순) */
    @EntityGraph(attributePaths = "mobType")
    List<MobPlacement> findAllByMobTypeIdInOrderByIdAsc(Collection<Long> mobTypeIds);

    long countByMobTypeId(Long mobTypeId);

    void delete(MobPlacement placement);

    void deleteByRoomId(Long roomId);

    /** 그 몹 유형을 둔 배치를 모두 지우고 지운 수를 돌려준다 */
    long deleteByMobTypeId(Long mobTypeId);
}
