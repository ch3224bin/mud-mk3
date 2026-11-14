package mudmk3.application.technique.required;

import mudmk3.domain.technique.Technique;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 공법(단계 표·신통 포함)을 저장하고 찾는다. 속성·단계·신통·신통 속성은 함께 읽는다
 * (리스트는 단계·신통 둘 다 순서 열이 있고 속성은 집합이라 여러 bag 문제가 없다)
 */
public interface TechniqueRepository extends Repository<Technique, Long> {

    Technique save(Technique technique);

    @EntityGraph(attributePaths = {"elements", "stages", "abilities", "abilities.elements"})
    Optional<Technique> findById(Long id);

    @EntityGraph(attributePaths = {"elements", "stages", "abilities", "abilities.elements"})
    List<Technique> findAllByOrderByNameAsc();

    /** 영문 대소문자를 가리지 않고 같은 이름이 있는지 */
    boolean existsByNameIgnoreCase(String name);

    /** 영문 대소문자를 가리지 않고 다른 공법이 같은 이름을 쓰는지 */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    void delete(Technique technique);
}
