package mudmk3.application.cultivation;

import lombok.RequiredArgsConstructor;
import mudmk3.application.character.provided.CharacterFinder;
import mudmk3.application.character.provided.CharacterRealmEditor;
import mudmk3.application.cultivation.provided.CultivationFinder;
import mudmk3.application.cultivation.provided.InsightEditor;
import mudmk3.application.cultivation.provided.ReadResult;
import mudmk3.application.cultivation.provided.TechniqueReader;
import mudmk3.application.cultivation.required.CharacterInsightRepository;
import mudmk3.application.cultivation.required.CharacterTechniqueRepository;
import mudmk3.application.inventory.provided.InventoryEditor;
import mudmk3.application.item.provided.ItemTypeFinder;
import mudmk3.application.technique.provided.TechniqueFinder;
import mudmk3.domain.character.PlayerCharacter;
import mudmk3.domain.character.SpiritRoot;
import mudmk3.domain.cultivation.CharacterInsight;
import mudmk3.domain.cultivation.CharacterTechnique;
import mudmk3.domain.cultivation.InsightKind;
import mudmk3.domain.inventory.RemoveCheck;
import mudmk3.domain.item.ItemCategory;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Realm;
import mudmk3.domain.technique.Ability;
import mudmk3.domain.technique.Technique;
import mudmk3.domain.technique.TechniqueStage;
import mudmk3.support.stereotype.ApplicationService;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static org.springframework.util.Assert.state;

@ApplicationService
@RequiredArgsConstructor
public class CultivationModifyService implements TechniqueReader, InsightEditor {

    private final CharacterFinder characterFinder;
    private final CharacterRealmEditor characterRealmEditor;
    private final CultivationFinder cultivationFinder;
    private final CharacterTechniqueRepository characterTechniqueRepository;
    private final CharacterInsightRepository characterInsightRepository;
    private final ItemTypeFinder itemTypeFinder;
    private final TechniqueFinder techniqueFinder;
    private final InventoryEditor inventoryEditor;

    @Override
    public ReadResult read(Long characterId, Long itemTypeId) {
        ItemType itemType = itemTypeFinder.find(itemTypeId);
        if (itemType.getCategory() != ItemCategory.SCROLL) {
            return new ReadResult.NotScroll();
        }
        Optional<Technique> contained = itemType.findTechnique();
        if (contained.isEmpty()) {
            return new ReadResult.NoTechnique();
        }
        // 담긴 공법의 단계·신통은 지연 로딩이라 정의 전체를 읽어 온다
        Technique technique = techniqueFinder.find(requireNonNull(contained.get().getId()));
        PlayerCharacter character = characterFinder.find(characterId);

        Optional<CharacterTechnique> alreadyLearned = cultivationFinder.findLearned(characterId).stream()
                .filter(learned -> Objects.equals(learned.getTechnique().getId(), technique.getId()))
                .findFirst();
        if (alreadyLearned.isPresent()) {
            return new ReadResult.AlreadyLearned(technique.getName(), alreadyLearned.get().getStage(),
                    technique.stageCount());
        }
        Realm realm = character.getRealm();
        if (realm.isBelow(technique.getStartRealm())) {
            return new ReadResult.RealmTooLow(technique.getStartRealm());
        }
        TechniqueStage firstStage = technique.stage(1);
        if (!technique.isBodyRefining()) {
            Optional<ReadResult> rootRejection = checkRoot(character.findSpiritRoot(), technique, firstStage);
            if (rootRejection.isPresent()) {
                return rootRejection.get();
            }
        }

        RemoveCheck removed = inventoryEditor.remove(characterId, itemTypeId, 1, realm.opensBracelet());
        state(removed == RemoveCheck.OK, () -> "옥간·서책을 뺄 수 없습니다: " + removed);
        characterTechniqueRepository.save(CharacterTechnique.learn(character, technique));
        boolean initiated = realm == Realm.MORTAL && technique.getStartRealm() == Realm.MORTAL;
        if (initiated) {
            characterRealmEditor.initiate(characterId);
        }
        return new ReadResult.Learned(technique.getName(), technique.getElements(), firstStage,
                technique.abilitiesUpTo(1).stream().map(Ability::getName).toList(), initiated);
    }

    /** 게임 락 안에서만 불러 같은 행을 동시에 만들 일이 없다. 있는 행은 값을 고쳐 dirty checking 으로 저장한다 */
    @Override
    public void add(Long characterId, Map<InsightKind, Double> gains) {
        gains.forEach((kind, gain) -> {
            if (gain <= 0) return;
            characterInsightRepository.findByCharacterIdAndKind(characterId, kind)
                    .ifPresentOrElse(insight -> insight.add(gain),
                            () -> characterInsightRepository.save(CharacterInsight.create(characterId, kind, gain)));
        });
    }

    /** 공법 속성마다(오행 순) 영근이 있는지, 다음으로 1성 자질 조건을 채우는지(소수 그대로 비교) */
    private static Optional<ReadResult> checkRoot(Optional<SpiritRoot> spiritRoot, Technique technique,
                                                  TechniqueStage firstStage) {
        for (Element element : technique.getElements()) {
            if (spiritRoot.isEmpty() || !spiritRoot.get().has(element)) {
                return Optional.of(new ReadResult.MissingRoot(element));
            }
        }
        SpiritRoot root = spiritRoot.orElseThrow();
        for (Element element : technique.getElements()) {
            double aptitude = root.aptitude(element);
            if (aptitude < firstStage.aptitudeRequired()) {
                return Optional.of(new ReadResult.LowAptitude(element, firstStage.aptitudeRequired(),
                        (int) Math.floor(aptitude)));
            }
        }
        return Optional.empty();
    }
}
