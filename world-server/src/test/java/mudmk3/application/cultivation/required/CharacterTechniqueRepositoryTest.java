package mudmk3.application.cultivation.required;

import jakarta.persistence.EntityManager;
import mudmk3.domain.character.PlayerCharacter;
import mudmk3.domain.cultivation.CharacterTechnique;
import mudmk3.domain.technique.Technique;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static mudmk3.domain.character.CharacterFixture.createCharacter;
import static mudmk3.domain.character.CharacterFixture.spiritRoot;
import static mudmk3.domain.technique.TechniqueFixture.changchun;
import static mudmk3.domain.technique.TechniqueFixture.greenWood;
import static mudmk3.domain.technique.TechniqueFixture.ironBody;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
record CharacterTechniqueRepositoryTest(CharacterTechniqueRepository characterTechniqueRepository,
                                        EntityManager entityManager) {

    @Test
    void learnAndFindInOrder() {
        PlayerCharacter han = persist(createCharacter(1L, "한백", 10L, spiritRoot()));
        PlayerCharacter yeo = persist(createCharacter(2L, "여림", 10L, spiritRoot()));
        Technique changchun = persist(changchun());
        Technique ironBody = persist(ironBody());
        Technique greenWood = persist(greenWood());
        characterTechniqueRepository.save(CharacterTechnique.learn(han, ironBody));
        characterTechniqueRepository.save(CharacterTechnique.learn(yeo, changchun));
        characterTechniqueRepository.save(CharacterTechnique.learn(han, greenWood));
        characterTechniqueRepository.save(CharacterTechnique.learn(han, changchun));
        entityManager.flush();
        entityManager.clear();

        assertThat(characterTechniqueRepository.findAllByCharacterIdOrderByIdAsc(han.getId()))
                .extracting(learned -> learned.getTechnique().getName(), CharacterTechnique::getStage)
                .containsExactly(tuple("강체결", 1), tuple("청목결", 1), tuple("장춘공", 1));
        assertThat(characterTechniqueRepository.existsByCharacterIdAndTechniqueId(yeo.getId(), changchun.getId()))
                .isTrue();
        assertThat(characterTechniqueRepository.existsByCharacterIdAndTechniqueId(yeo.getId(), ironBody.getId()))
                .isFalse();
        assertThat(characterTechniqueRepository.countByCharacter()).containsExactlyInAnyOrder(
                new LearnedCount(han.getId(), 3), new LearnedCount(yeo.getId(), 1));
    }

    @Test
    void duplicateFail() {
        PlayerCharacter han = persist(createCharacter(1L, "한백", 10L, spiritRoot()));
        Technique changchun = persist(changchun());
        characterTechniqueRepository.save(CharacterTechnique.learn(han, changchun));
        entityManager.flush();

        assertThatThrownBy(() -> {
            characterTechniqueRepository.save(CharacterTechnique.learn(han, changchun));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }
}
