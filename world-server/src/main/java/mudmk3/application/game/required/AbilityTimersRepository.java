package mudmk3.application.game.required;

import mudmk3.domain.combat.AbilityTimers;

import java.util.List;
import java.util.Optional;

/** 신통 대기시간·시전을 메모리에 둔다 */
public interface AbilityTimersRepository {

    /** 넣거나 바꾼다(캐릭터 id 기준) */
    void save(AbilityTimers timers);

    Optional<AbilityTimers> find(Long characterId);

    /** 모든 대기시간·시전(캐릭터 id 순) */
    List<AbilityTimers> findAll();

    void deleteAll();
}
