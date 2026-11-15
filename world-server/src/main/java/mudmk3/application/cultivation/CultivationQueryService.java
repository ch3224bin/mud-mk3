package mudmk3.application.cultivation;

import lombok.RequiredArgsConstructor;
import mudmk3.application.character.provided.CharacterFinder;
import mudmk3.application.character.provided.CharacterVitalsEditor;
import mudmk3.application.cultivation.provided.CharacterSummary;
import mudmk3.application.cultivation.provided.CultivationFinder;
import mudmk3.application.cultivation.provided.CultivationProfile;
import mudmk3.application.cultivation.provided.LearnedTechniqueView;
import mudmk3.application.cultivation.required.CharacterTechniqueRepository;
import mudmk3.application.cultivation.required.LearnedCount;
import mudmk3.domain.character.PlayerCharacter;
import mudmk3.domain.character.SpiritRoot;
import mudmk3.domain.cultivation.CharacterTechnique;
import mudmk3.domain.cultivation.StatSheet;
import mudmk3.domain.technique.Technique;
import mudmk3.support.stereotype.ApplicationService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@ApplicationService
@RequiredArgsConstructor
public class CultivationQueryService implements CultivationFinder {

    private final CharacterFinder characterFinder;
    private final CharacterVitalsEditor characterVitalsEditor;
    private final CharacterTechniqueRepository characterTechniqueRepository;

    @Override
    public StatSheet stats(Long characterId) {
        return stats(characterFinder.find(characterId), findLearned(characterId));
    }

    @Override
    public List<CharacterTechnique> findLearned(Long characterId) {
        return characterTechniqueRepository.findAllByCharacterIdOrderByIdAsc(characterId);
    }

    @Override
    public CultivationProfile profile(Long characterId) {
        PlayerCharacter character = characterFinder.find(characterId);
        List<CharacterTechnique> learned = findLearned(characterId);
        return new CultivationProfile(
                characterId,
                character.getName().value(),
                character.getRealm(),
                character.findSpiritRoot().orElse(null),
                stats(character, learned),
                learned.stream().map(CultivationQueryService::toView).toList());
    }

    @Override
    public List<CharacterSummary> summaries() {
        Map<Long, Long> counts = characterTechniqueRepository.countByCharacter().stream()
                .collect(Collectors.toMap(LearnedCount::characterId, LearnedCount::count));
        return characterFinder.findAllByName().stream()
                .map(character -> {
                    Long id = requireNonNull(character.getId());
                    return new CharacterSummary(id, character.getName().value(), character.getRealm(),
                            character.findSpiritRoot().map(SpiritRoot::name).orElse(null),
                            Math.toIntExact(counts.getOrDefault(id, 0L)));
                })
                .toList();
    }

    /** 깎인 기혈은 엔티티 값이 아니라 전용 쿼리로 읽는다 */
    private StatSheet stats(PlayerCharacter character, List<CharacterTechnique> learned) {
        double qiBloodLost = characterVitalsEditor.qiBloodLost(requireNonNull(character.getId()));
        return StatSheet.of(character.getRealm(), character.findSpiritRoot().orElse(null), learned, qiBloodLost);
    }

    private static LearnedTechniqueView toView(CharacterTechnique learned) {
        Technique technique = learned.getTechnique();
        return new LearnedTechniqueView(requireNonNull(technique.getId()), technique.getName(), learned.getStage(),
                technique.stageCount());
    }
}
