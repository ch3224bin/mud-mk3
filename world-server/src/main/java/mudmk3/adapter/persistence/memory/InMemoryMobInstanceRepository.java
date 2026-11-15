package mudmk3.adapter.persistence.memory;

import mudmk3.application.game.required.MobInstanceRepository;
import mudmk3.domain.combat.MobInstance;
import mudmk3.support.stereotype.Adapter;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** 세계의 몹을 몹 id로 메모리에 둔다. 게임 락 안에서만 바뀌므로 조회는 단순 순회로 한다 */
@Adapter
public class InMemoryMobInstanceRepository implements MobInstanceRepository {

    private static final Comparator<MobInstance> PLACEMENT_ORDER =
            Comparator.comparing(MobInstance::getPlacementId).thenComparingInt(MobInstance::getSlot);

    private final Map<Long, MobInstance> mobs = new ConcurrentHashMap<>();

    @Override
    public void save(MobInstance mob) {
        mobs.put(mob.getId(), mob);
    }

    @Override
    public Optional<MobInstance> findById(long id) {
        return Optional.ofNullable(mobs.get(id));
    }

    @Override
    public List<MobInstance> findAllInRoom(Long roomId) {
        return mobs.values().stream()
                .filter(mob -> mob.isAlive() && mob.getRoomId().equals(roomId))
                .sorted(PLACEMENT_ORDER)
                .toList();
    }

    @Override
    public List<MobInstance> findAll() {
        return mobs.values().stream().sorted(PLACEMENT_ORDER).toList();
    }

    @Override
    public void delete(long id) {
        mobs.remove(id);
    }

    @Override
    public void deleteAll() {
        mobs.clear();
    }
}
