package mudmk3.application.cultivation.required;

import mudmk3.domain.cultivation.CharacterTechnique;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;

/** 캐릭터가 익힌 공법을 저장하고 찾는다 */
public interface CharacterTechniqueRepository extends Repository<CharacterTechnique, Long> {

    CharacterTechnique save(CharacterTechnique characterTechnique);

    /** 그 캐릭터가 익힌 공법을 모두 지운다(읽어서 하나씩 지움 — 영속성 컨텍스트와 맞음) */
    void deleteAllByCharacterId(Long characterId);

    /** 그 캐릭터가 익힌 공법(익힌 순서). 공법은 함께 읽는다 */
    @EntityGraph(attributePaths = "technique")
    List<CharacterTechnique> findAllByCharacterIdOrderByIdAsc(Long characterId);

    /** 공법을 하나 이상 익힌 캐릭터마다 익힌 공법 수 */
    @Query("select new mudmk3.application.cultivation.required.LearnedCount(c.character.id, count(c)) "
            + "from CharacterTechnique c group by c.character.id")
    List<LearnedCount> countByCharacter();
}
