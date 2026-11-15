package mudmk3.adapter.persistence.memory;

import mudmk3.domain.combat.AbilityTimers;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryAbilityTimersRepositoryTest {

    private final InMemoryAbilityTimersRepository repository = new InMemoryAbilityTimersRepository();

    @Test
    void saveFindDeleteAll() {
        AbilityTimers first = new AbilityTimers(7L);
        AbilityTimers second = new AbilityTimers(3L);
        repository.save(first);
        repository.save(second);

        assertThat(repository.find(7L)).containsSame(first);
        assertThat(repository.find(9L)).isEmpty();
        assertThat(repository.findAll()).containsExactly(second, first);

        AbilityTimers replaced = new AbilityTimers(7L);
        repository.save(replaced);
        assertThat(repository.find(7L)).containsSame(replaced);

        repository.deleteAll();
        assertThat(repository.findAll()).isEmpty();
        assertThat(repository.find(3L)).isEmpty();
    }
}
