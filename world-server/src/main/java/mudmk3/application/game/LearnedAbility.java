package mudmk3.application.game;

import mudmk3.domain.combat.AbilityKey;
import mudmk3.domain.technique.Ability;
import mudmk3.domain.technique.Technique;

import static java.util.Objects.requireNonNull;

/**
 * 캐릭터가 배운 신통 하나: 그 신통이 속한 공법, 신통 정의, 그 공법의 지금 단계. 엔티티를 들고 있으므로 트랜잭션 안에서만 쓰고
 * 메모리 상태에 두지 않는다(메모리에는 {@link #key()} 만 둔다)
 */
record LearnedAbility(Technique technique, Ability ability, int stage) {

    LearnedAbility {
        requireNonNull(technique);
        requireNonNull(ability);
    }

    /** 재사용 대기시간·시전의 키: (공법 id, 신통 이름) */
    AbilityKey key() {
        return new AbilityKey(requireNonNull(technique.getId(), "저장되지 않은 공법입니다"), ability.getName());
    }
}
