package mudmk3.application.technique.provided;

import mudmk3.ApplicationServiceTest;
import mudmk3.domain.shared.Element;
import mudmk3.domain.technique.AbilityKind;
import mudmk3.domain.technique.AbilityTarget;
import mudmk3.domain.technique.CostStat;
import mudmk3.domain.technique.TechniqueFixture;
import mudmk3.domain.technique.UsableWhen;
import org.junit.jupiter.api.Test;

import java.util.List;

import static mudmk3.application.technique.provided.TechniqueRequestFixture.changchunRequest;
import static mudmk3.application.technique.provided.TechniqueRequestFixture.requestOf;
import static org.assertj.core.api.Assertions.assertThat;

@ApplicationServiceTest
record TechniqueFinderTest(TechniqueEditor techniqueEditor, TechniqueFinder techniqueFinder) {

    @Test
    void findAbilityName() {
        techniqueEditor.create(changchunRequest("장춘공"));
        techniqueEditor.create(requestOf(TechniqueFixture.ironBodyInfo()));

        assertThat(techniqueFinder.findAbilityName("철권")).contains("철권");
        assertThat(techniqueFinder.findAbilityName("어풍결")).contains("어풍결");
        assertThat(techniqueFinder.findAbilityName("화탄")).isEmpty();
        assertThat(techniqueFinder.findAbilityName("뇌격")).isEmpty();
    }

    @Test
    void findAbilityNameIgnoresCase() {
        techniqueEditor.create(requestOf(TechniqueFixture.changchunWith(List.of(
                TechniqueFixture.ability("Bolt", 1, AbilityKind.ATTACK, List.of(Element.FIRE), AbilityTarget.SINGLE,
                        CostStat.MANA, "5", "0", "3", UsableWhen.COMBAT_ONLY)))));

        assertThat(techniqueFinder.findAbilityName("bolt")).contains("Bolt");
    }

    /** 같은 이름이 여러 공법에 있으면 먼저 정의된 것(F12) */
    @Test
    void findAbilityNameFirstDefined() {
        techniqueEditor.create(changchunRequest("장춘공"));
        techniqueEditor.create(changchunRequest("청운공"));

        assertThat(techniqueFinder.findAbilityName("화탄술")).contains("화탄술");
    }
}
