package mudmk3.adapter.persistence.memory;

import mudmk3.application.game.required.AbilityTimersRepository;
import mudmk3.domain.combat.AbilityTimers;
import mudmk3.support.stereotype.Adapter;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** 신통 대기시간·시전을 캐릭터 id로 메모리에 둔다. 게임 락 안에서만 바뀐다 */
@Adapter
public class InMemoryAbilityTimersRepository implements AbilityTimersRepository {

    private final Map<Long, AbilityTimers> timers = new ConcurrentHashMap<>();

    @Override
    public void save(AbilityTimers abilityTimers) {
        timers.put(abilityTimers.getCharacterId(), abilityTimers);
    }

    @Override
    public Optional<AbilityTimers> find(Long characterId) {
        return Optional.ofNullable(timers.get(characterId));
    }

    @Override
    public List<AbilityTimers> findAll() {
        return timers.values().stream().sorted(Comparator.comparing(AbilityTimers::getCharacterId)).toList();
    }

    @Override
    public void deleteAll() {
        timers.clear();
    }
}
