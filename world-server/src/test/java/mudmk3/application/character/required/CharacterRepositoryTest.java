package mudmk3.application.character.required;

import jakarta.persistence.EntityManager;
import mudmk3.domain.character.CharacterName;
import mudmk3.domain.character.PlayerCharacter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static mudmk3.domain.character.CharacterFixture.createCharacter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
record CharacterRepositoryTest(CharacterRepository characterRepository, EntityManager entityManager) {

    @Test
    void save() {
        PlayerCharacter character = characterRepository.save(createCharacter(1L, "한백", 10L));
        flushAndClear();

        PlayerCharacter found = characterRepository.findById(character.getId()).orElseThrow();
        assertThat(found.getAccountId()).isEqualTo(1L);
        assertThat(found.getName()).isEqualTo(new CharacterName("한백"));
        assertThat(found.getRoomId()).isEqualTo(10L);
    }

    @Test
    void findByAccountId() {
        characterRepository.save(createCharacter(1L, "한백", 10L));
        characterRepository.save(createCharacter(2L, "한립", 10L));
        flushAndClear();

        assertThat(characterRepository.findByAccountId(2L))
                .map(character -> character.getName().value())
                .contains("한립");
        assertThat(characterRepository.findByAccountId(3L)).isEmpty();
    }

    @Test
    void existsByName() {
        characterRepository.save(createCharacter(1L, "한백", 10L));
        flushAndClear();

        assertThat(characterRepository.existsByName(new CharacterName("한백"))).isTrue();
        assertThat(characterRepository.existsByName(new CharacterName("한립"))).isFalse();
    }

    @Test
    void findAllByRoomId() {
        characterRepository.save(createCharacter(1L, "한백", 10L));
        characterRepository.save(createCharacter(2L, "한립", 20L));
        characterRepository.save(createCharacter(3L, "남궁완", 10L));
        flushAndClear();

        assertThat(characterRepository.findAllByRoomId(10L))
                .extracting(character -> character.getName().value())
                .containsExactlyInAnyOrder("한백", "남궁완");
        assertThat(characterRepository.findAllByRoomId(30L)).isEmpty();
    }

    @Test
    void duplicateAccountIdFail() {
        characterRepository.save(createCharacter(1L, "한백", 10L));

        assertThatThrownBy(() -> {
            characterRepository.save(createCharacter(1L, "한립", 10L));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicateNameFail() {
        characterRepository.save(createCharacter(1L, "한백", 10L));

        assertThatThrownBy(() -> {
            characterRepository.save(createCharacter(2L, "한백", 10L));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
