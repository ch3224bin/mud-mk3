package mudmk3.application.mob.required;

import mudmk3.domain.mob.MobType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/** 몹 유형(드롭 표 포함)을 저장하고 찾는다. 드롭 표 둘은 순서 열이 있는 리스트라 함께 읽어도 여러 bag 문제가 없다 */
public interface MobTypeRepository extends Repository<MobType, Long> {

    MobType save(MobType mobType);

    @EntityGraph(attributePaths = {"fixedDrops", "tierDrops"})
    Optional<MobType> findById(Long id);

    @EntityGraph(attributePaths = {"fixedDrops", "tierDrops"})
    List<MobType> findAllByOrderByNameAsc();

    /** 영문 대소문자를 가리지 않고 같은 이름이 있는지 */
    boolean existsByNameIgnoreCase(String name);

    /** 영문 대소문자를 가리지 않고 다른 몹이 같은 이름을 쓰는지 */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    void delete(MobType mobType);
}
