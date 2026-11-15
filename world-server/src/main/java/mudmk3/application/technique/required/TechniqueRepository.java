package mudmk3.application.technique.required;

import mudmk3.domain.technique.Technique;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
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

    /** 이 공법이 담긴 아이템 종류(이름순) */
    @Query("select new mudmk3.application.technique.required.ScrollItem(i.id, i.name) from ItemType i "
            + "where i.technique.id = :techniqueId order by i.name")
    List<ScrollItem> findScrollItems(Long techniqueId);

    /** 이 공법을 익힌 캐릭터 수 */
    @Query("select count(c) from CharacterTechnique c where c.technique.id = :id")
    long countLearners(Long id);

    /** 모든 공법의 신통 가운데 영문 대소문자를 가리지 않고 이름이 같은 것의 저장된 이름(신통 id 순) */
    @Query("select a.name from Ability a where lower(a.name) = lower(:name) order by a.id")
    List<String> findAbilityNames(String name);

    /** 이 공법을 익힌 캐릭터의 가장 높은 단계. 익힌 캐릭터가 없으면 비어 있다 */
    @Query("select max(c.stage) from CharacterTechnique c where c.technique.id = :id")
    Optional<Integer> maxLearnedStage(Long id);
}
