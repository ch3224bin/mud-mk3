package mudmk3.application.cultivation.required;

import mudmk3.domain.cultivation.CharacterInsight;
import mudmk3.domain.cultivation.InsightKind;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/** 캐릭터의 깨달음(갈래마다 한 행)을 저장하고 찾는다 */
public interface CharacterInsightRepository extends Repository<CharacterInsight, Long> {

    CharacterInsight save(CharacterInsight insight);

    List<CharacterInsight> findAllByCharacterId(Long characterId);

    Optional<CharacterInsight> findByCharacterIdAndKind(Long characterId, InsightKind kind);
}
