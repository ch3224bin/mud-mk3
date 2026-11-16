package mudmk3.application.game.required;

import mudmk3.domain.combat.AbilityTimers;
import mudmk3.domain.combat.Casting;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** 신통 대기시간·시전을 메모리에 둔다 */
public interface AbilityTimersRepository {

    /** 넣거나 바꾼다(캐릭터 id 기준) */
    void save(AbilityTimers timers);

    Optional<AbilityTimers> find(Long characterId);

    /** 모든 대기시간·시전(캐릭터 id 순) */
    List<AbilityTimers> findAll();

    void deleteAll();

    /** 그 캐릭터의 시전이 {@code which} 에 맞으면 지우고 저장한다(대기시간은 남음). 지웠으면 true */
    default boolean clearCasting(Long characterId, Predicate<Casting> which) {
        Optional<AbilityTimers> timers = find(characterId).filter(t -> t.casting().filter(which).isPresent());
        timers.ifPresent(t -> {
            t.clearCasting();
            save(t);
        });
        return timers.isPresent();
    }
}
