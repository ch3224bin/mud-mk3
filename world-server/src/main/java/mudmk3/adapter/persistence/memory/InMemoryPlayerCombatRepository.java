package mudmk3.adapter.persistence.memory;

import mudmk3.application.game.required.PlayerCombatRepository;
import mudmk3.domain.combat.PlayerCombat;
import mudmk3.support.stereotype.Adapter;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** 플레이어의 교전 상태를 캐릭터 id로 메모리에 둔다. 게임 락 안에서만 바뀐다 */
@Adapter
public class InMemoryPlayerCombatRepository implements PlayerCombatRepository {

    private final Map<Long, PlayerCombat> combats = new ConcurrentHashMap<>();

    @Override
    public void save(PlayerCombat combat) {
        combats.put(combat.getCharacterId(), combat);
    }

    @Override
    public Optional<PlayerCombat> find(Long characterId) {
        return Optional.ofNullable(combats.get(characterId));
    }

    @Override
    public List<PlayerCombat> findAll() {
        return combats.values().stream().sorted(Comparator.comparing(PlayerCombat::getCharacterId)).toList();
    }

    @Override
    public void delete(Long characterId) {
        combats.remove(characterId);
    }

    @Override
    public void deleteAll() {
        combats.clear();
    }
}
