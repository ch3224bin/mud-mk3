package mudmk3.adapter.persistence.memory;

import mudmk3.application.game.required.CorpseRepository;
import mudmk3.domain.combat.Corpse;
import mudmk3.support.stereotype.Adapter;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 시체를 시체 id로 메모리에 둔다. 게임 락 안에서만 바뀐다 */
@Adapter
public class InMemoryCorpseRepository implements CorpseRepository {

    private static final Comparator<Corpse> DEATH_ORDER =
            Comparator.comparing(Corpse::getDiedAt).thenComparingLong(Corpse::getId);

    private final Map<Long, Corpse> corpses = new ConcurrentHashMap<>();

    @Override
    public void save(Corpse corpse) {
        corpses.put(corpse.getId(), corpse);
    }

    @Override
    public List<Corpse> findAllInRoom(Long roomId) {
        return corpses.values().stream()
                .filter(corpse -> corpse.getRoomId().equals(roomId))
                .sorted(DEATH_ORDER)
                .toList();
    }

    @Override
    public List<Corpse> findAll() {
        return corpses.values().stream().sorted(DEATH_ORDER).toList();
    }

    @Override
    public void delete(long id) {
        corpses.remove(id);
    }

    @Override
    public void deleteAll() {
        corpses.clear();
    }
}
