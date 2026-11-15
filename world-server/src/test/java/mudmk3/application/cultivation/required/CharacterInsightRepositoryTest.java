package mudmk3.application.cultivation.required;

import jakarta.persistence.EntityManager;
import mudmk3.domain.cultivation.CharacterInsight;
import mudmk3.domain.cultivation.InsightKind;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
record CharacterInsightRepositoryTest(CharacterInsightRepository characterInsightRepository,
                                      EntityManager entityManager) {

    /** 찾은 행의 값을 고치면 저장된다(JPQL 일괄 갱신 없이 dirty checking) */
    @Test
    void addAmountUpdatesExisting() {
        characterInsightRepository.save(CharacterInsight.create(1L, InsightKind.FIRE, 5.0));
        characterInsightRepository.save(CharacterInsight.create(2L, InsightKind.FIRE, 1.0));
        flushAndClear();

        characterInsightRepository.findByCharacterIdAndKind(1L, InsightKind.FIRE).orElseThrow().add(2.5);
        flushAndClear();

        assertThat(characterInsightRepository.findAllByCharacterId(1L))
                .extracting(CharacterInsight::getKind, CharacterInsight::getAmount)
                .containsExactly(tuple(InsightKind.FIRE, 7.5));
        assertThat(characterInsightRepository.findByCharacterIdAndKind(2L, InsightKind.FIRE).orElseThrow().getAmount())
                .isEqualTo(1.0);
        assertThat(characterInsightRepository.findByCharacterIdAndKind(1L, InsightKind.WOOD)).isEmpty();
    }

    @Test
    void uniqueCharacterKind() {
        characterInsightRepository.save(CharacterInsight.create(1L, InsightKind.FIRE, 5.0));
        characterInsightRepository.save(CharacterInsight.create(1L, InsightKind.WOOD, 5.0));
        entityManager.flush();

        assertThatThrownBy(() -> {
            characterInsightRepository.save(CharacterInsight.create(1L, InsightKind.FIRE, 1.0));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
