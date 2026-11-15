package mudmk3.application.game;

import lombok.RequiredArgsConstructor;
import mudmk3.application.cultivation.provided.CultivationFinder;
import mudmk3.application.technique.provided.TechniqueFinder;
import mudmk3.domain.combat.AbilityKey;
import mudmk3.domain.cultivation.CharacterTechnique;
import mudmk3.domain.technique.Ability;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.BiPredicate;

/**
 * 신통 이름을 배운 신통으로 해석한다(command.md {@code ### 신통 쓰기} 이름 해석 2·3). 명령마다 DB에서 익힌 공법과 정의를 읽는다.
 * 트랜잭션 안에서만 불린다
 */
@Component
@RequiredArgsConstructor
class AbilityResolver {

    private final CultivationFinder cultivationFinder;
    private final TechniqueFinder techniqueFinder;

    /**
     * 익힌 공법마다(익힌 순서) 지금 단계까지 배운 신통 가운데 이름 전체가 같은 것(영문 대소문자 무시). 다른 공법에 같은 이름이
     * 있으면 먼저 익힌 공법의 것
     */
    Optional<LearnedAbility> findLearned(Long characterId, String name) {
        return find(characterId, (technique, ability) -> ability.getName().equalsIgnoreCase(name));
    }

    /** 키(공법 id, 신통 이름)의 신통을 지금도 배운 상태면 다시 읽는다(시전 발동 때) */
    Optional<LearnedAbility> findLearned(Long characterId, AbilityKey key) {
        return find(characterId, (technique, ability) -> key.techniqueId().equals(technique.getTechnique().getId())
                && ability.getName().equals(key.abilityName()));
    }

    /** 어느 공법에든 그 이름의 신통이 정의되어 있으면 그 저장된 이름(이름 해석 3) */
    Optional<String> definedName(String name) {
        return techniqueFinder.findAbilityName(name);
    }

    private Optional<LearnedAbility> find(Long characterId, BiPredicate<CharacterTechnique, Ability> matches) {
        for (CharacterTechnique learned : cultivationFinder.findLearned(characterId)) {
            Optional<Ability> found = learned.getTechnique().abilitiesUpTo(learned.getStage()).stream()
                    .filter(ability -> matches.test(learned, ability))
                    .findFirst();
            if (found.isPresent()) {
                return Optional.of(new LearnedAbility(learned.getTechnique(), found.get(), learned.getStage()));
            }
        }
        return Optional.empty();
    }
}
