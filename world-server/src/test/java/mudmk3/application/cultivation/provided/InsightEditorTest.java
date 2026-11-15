package mudmk3.application.cultivation.provided;

import jakarta.persistence.EntityManager;
import mudmk3.ApplicationServiceTest;
import mudmk3.domain.character.PlayerCharacter;
import mudmk3.domain.character.SpiritRoot;
import mudmk3.domain.cultivation.InsightKind;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Objects.requireNonNull;
import static mudmk3.domain.character.CharacterFixture.createCharacter;
import static org.assertj.core.api.Assertions.assertThat;

@ApplicationServiceTest
record InsightEditorTest(InsightEditor insightEditor, CultivationFinder cultivationFinder,
                         EntityManager entityManager) {

    @Test
    void addCreatesThenAccumulates() {
        Long id = character();

        insightEditor.add(id, Map.of(InsightKind.FIRE, 5.0, InsightKind.WOOD, 2.5));
        flushAndClear();
        insightEditor.add(id, Map.of(InsightKind.FIRE, 5.0, InsightKind.WOOD, 2.5));
        flushAndClear();

        assertThat(cultivationFinder.insight(id).amounts()).containsExactly(
                Map.entry(InsightKind.METAL, 0.0), Map.entry(InsightKind.WOOD, 5.0),
                Map.entry(InsightKind.WATER, 0.0), Map.entry(InsightKind.FIRE, 10.0),
                Map.entry(InsightKind.EARTH, 0.0), Map.entry(InsightKind.BODY, 0.0));
    }

    /** 한 트랜잭션 안에서 두 번 더해도(아직 flush 전) 한 행에 쌓인다 */
    @Test
    void addTwiceInOneTransaction() {
        Long id = character();

        insightEditor.add(id, Map.of(InsightKind.WOOD, 2.5));
        insightEditor.add(id, Map.of(InsightKind.WOOD, 2.5));
        flushAndClear();

        assertThat(cultivationFinder.insight(id).amount(InsightKind.WOOD)).isEqualTo(5.0);
    }

    @Test
    void skipsZero() {
        Long id = character();

        insightEditor.add(id, Map.of(InsightKind.FIRE, 0.0, InsightKind.WOOD, -1.0));
        flushAndClear();

        assertThat(entityManager.createQuery("select count(i) from CharacterInsight i", Long.class)
                .getSingleResult()).isZero();
    }

    private Long character() {
        PlayerCharacter han = createCharacter(1L, "한백", 10L, SpiritRoot.parse("목 화").orElseThrow());
        entityManager.persist(han);
        return requireNonNull(han.getId());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
