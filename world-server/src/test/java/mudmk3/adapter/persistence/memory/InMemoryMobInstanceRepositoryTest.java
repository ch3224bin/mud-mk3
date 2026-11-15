package mudmk3.adapter.persistence.memory;

import mudmk3.domain.combat.MobInstance;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static mudmk3.domain.combat.CombatFixture.wolfProfile;
import static org.assertj.core.api.Assertions.assertThat;

class InMemoryMobInstanceRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-11-14T00:00:00Z");

    private final InMemoryMobInstanceRepository repository = new InMemoryMobInstanceRepository();

    @Test
    void findAllInRoom() {
        repository.save(mob(1, 200L, 1, 10L));
        repository.save(mob(2, 100L, 1, 10L));
        repository.save(mob(3, 200L, 0, 10L));
        repository.save(mob(4, 100L, 0, 10L));
        repository.save(mob(5, 300L, 0, 20L));
        MobInstance dead = mob(6, 100L, 2, 10L);
        dead.engage(7L, NOW);
        dead.receive(7L, 1_000, NOW);
        repository.save(dead);

        assertThat(repository.findAllInRoom(10L)).extracting(MobInstance::getId).containsExactly(4L, 2L, 3L, 1L);
        assertThat(repository.findAll()).extracting(MobInstance::getId).containsExactly(4L, 2L, 6L, 3L, 1L, 5L);
    }

    @Test
    void saveFindDelete() {
        MobInstance mob = mob(1, 100L, 0, 10L);
        repository.save(mob);
        repository.save(mob(2, 100L, 1, 10L));

        assertThat(repository.findById(1)).containsSame(mob);
        repository.delete(1);
        assertThat(repository.findById(1)).isEmpty();
        repository.deleteAll();
        assertThat(repository.findAll()).isEmpty();
    }

    private static MobInstance mob(long id, Long placementId, int slot, Long roomId) {
        return MobInstance.spawn(id, placementId, slot, roomId, 3, wolfProfile(), NOW);
    }
}
