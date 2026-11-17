package mudmk3.application.technique.required;

import mudmk3.application.technique.provided.ScrollItem;
import mudmk3.domain.technique.Technique;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 공법(단계 표·신통 포함)을 저장하고 찾는다. 공법은 속성과 함께 읽고, 단계·신통과 신통의 속성·이펙트 문장·단계별 값은
 * 트랜잭션 안에서 지연 로딩한다. 컬렉션 여럿을 한 쿼리로 읽으면 행이 곱해지므로
 * {@code hibernate.default_batch_fetch_size} 로 여러 공법(신통)의 같은 컬렉션을 한 쿼리에 묶어 읽는다
 */
public interface TechniqueRepository extends Repository<Technique, Long> {

    Technique save(Technique technique);

    @EntityGraph(attributePaths = "elements")
    Optional<Technique> findById(Long id);

    @EntityGraph(attributePaths = "elements")
    List<Technique> findAllByOrderByNameAsc();

    /** 영문 대소문자를 가리지 않고 같은 이름이 있는지 */
    boolean existsByNameIgnoreCase(String name);

    /** 영문 대소문자를 가리지 않고 다른 공법이 같은 이름을 쓰는지 */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    void delete(Technique technique);

    /** 이 공법이 담긴 아이템 종류(이름순) */
    @Query("select new mudmk3.application.technique.provided.ScrollItem(i.id, i.name) from ItemType i "
            + "where i.technique.id = :techniqueId order by i.name")
    List<ScrollItem> findScrollItems(Long techniqueId);

    /** 그 공법들이 담긴 아이템 종류(이름순) */
    @Query("select new mudmk3.application.technique.required.ScrollItemRow(i.technique.id, i.id, i.name) "
            + "from ItemType i where i.technique.id in :techniqueIds order by i.name")
    List<ScrollItemRow> findScrollItemsIn(Collection<Long> techniqueIds);

    /** 공법마다 익힌 캐릭터 수(익힌 캐릭터가 없는 공법은 줄이 없다) */
    @Query("select new mudmk3.application.technique.required.LearnerCount(c.technique.id, count(c)) "
            + "from CharacterTechnique c group by c.technique.id")
    List<LearnerCount> countLearnersByTechnique();

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
