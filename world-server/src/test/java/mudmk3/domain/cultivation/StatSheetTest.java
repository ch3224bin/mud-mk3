package mudmk3.domain.cultivation;

import mudmk3.domain.character.SpiritRoot;
import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Realm;
import mudmk3.domain.technique.Technique;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static mudmk3.domain.character.CharacterFixture.createCharacter;
import static mudmk3.domain.cultivation.CharacterTechniqueFixture.learned;
import static mudmk3.domain.technique.TechniqueFixture.changchun;
import static mudmk3.domain.technique.TechniqueFixture.ironBody;
import static org.assertj.core.api.Assertions.assertThat;

class StatSheetTest {

    private static SpiritRoot root(String input) {
        return SpiritRoot.parse(input).orElseThrow();
    }

    @Test
    void mortal() {
        StatSheet stats = StatSheet.of(Realm.MORTAL, root("화"), List.of(), 0, 0);

        assertThat(stats).isEqualTo(new StatSheet(10, 10.0, 0, 0.0, 10, 10, 0, 1, 1, Map.of(Element.FIRE, 100.0)));
        assertThat(stats.aptitudes()).containsExactly(Map.entry(Element.FIRE, 100.0));
    }

    @Test
    void qi1WithChangchunFirstStage() {
        StatSheet stats = StatSheet.of(Realm.QI_1, root("목 화"), List.of(learned(changchun())), 0, 0);

        assertThat(stats.qiBlood()).isEqualTo(60);
        assertThat(stats.mana()).isEqualTo(25);
        assertThat(stats.sense()).isEqualTo(20);
        assertThat(stats.essence()).isEqualTo(20);
        assertThat(stats.spirit()).isEqualTo(5);
        assertThat(stats.body()).isEqualTo(6);
        assertThat(stats.strength()).isEqualTo(3);
        assertThat(stats.aptitude(Element.WOOD)).isEqualTo(50.0);
    }

    @Test
    void changchunThirdStageSumsStages() {
        StatSheet stats = StatSheet.of(Realm.QI_1, root("목 화"), List.of(learned(changchun(), 3)), 0, 0);

        assertThat(stats.mana()).isEqualTo(25 + 4);
        assertThat(stats.sense()).isEqualTo(21);
        assertThat(stats.aptitude(Element.WOOD)).isEqualTo(50.0 + 2);
        assertThat(stats.aptitudes()).containsExactly(Map.entry(Element.WOOD, 52.0), Map.entry(Element.FIRE, 50.0));
    }

    @Test
    void qi7() {
        StatSheet stats = StatSheet.of(Realm.QI_7, root("목"), List.of(), 0, 0);

        assertThat(stats.qiBlood()).isEqualTo(120);
        assertThat(stats.mana()).isEqualTo(55);
        assertThat(stats.sense()).isEqualTo(32);
    }

    @Test
    void ironBodyFifthStage() {
        StatSheet base = StatSheet.of(Realm.QI_1, root("화"), List.of(), 0, 0);
        StatSheet stats = StatSheet.of(Realm.QI_1, root("화"), List.of(learned(ironBody(), 5)), 0, 0);

        assertThat(stats.body()).isEqualTo(base.body() + 2);
        assertThat(stats.strength()).isEqualTo(base.strength() + 2);
        assertThat(stats.qiBlood()).isEqualTo(base.qiBlood() + 20);
        assertThat(stats.aptitudes()).isEqualTo(base.aptitudes());
    }

    @Test
    void manaZeroWhenSpiritZero() {
        StatSheet stats = StatSheet.of(Realm.MORTAL, root("목"), List.of(learned(changchun(), 3)), 0, 0);

        assertThat(stats.spirit()).isZero();
        assertThat(stats.mana()).isZero();
    }

    @Test
    void aptitudeOnlyForTechniqueElements() {
        StatSheet stats = StatSheet.of(Realm.QI_1, root("목 화"), List.of(learned(changchun(), 9)), 0, 0);

        assertThat(stats.aptitude(Element.WOOD)).isEqualTo(50.0 + 8);
        assertThat(stats.aptitude(Element.FIRE)).isEqualTo(50.0);
    }

    @Test
    void techniquesAddUp() {
        Technique changchun = changchun();
        StatSheet stats = StatSheet.of(Realm.QI_1, root("목"),
                List.of(learned(changchun, 3), learned(ironBody(), 5)), 0, 0);

        assertThat(stats.mana()).isEqualTo(29);
        assertThat(stats.body()).isEqualTo(8);
        assertThat(stats.qiBlood()).isEqualTo(80);
    }

    @Test
    void foundationPeak() {
        StatSheet stats = StatSheet.of(Realm.FOUNDATION_PEAK, root("목"), List.of(), 0, 0);

        assertThat(stats.qiBlood()).isEqualTo(540);
        assertThat(stats.mana()).isEqualTo(255);
    }

    @Test
    void noSpiritRootHasNoAptitudes() {
        StatSheet stats = StatSheet.of(Realm.QI_1, null, List.of(learned(changchun(), 3)), 0, 0);

        assertThat(stats.aptitudes()).isEmpty();
        assertThat(stats.aptitude(Element.WOOD)).isZero();
        assertThat(stats.mana()).isEqualTo(29);
    }

    @Test
    void qiBloodCurrentFromLost() {
        StatSheet stats = StatSheet.of(Realm.QI_1, root("목"), List.of(), 12.5, 0);

        assertThat(stats.qiBlood()).isEqualTo(60);
        assertThat(stats.qiBloodCurrent()).isEqualTo(47.5);
    }

    @Test
    void qiBloodCurrentNotBelowZero() {
        StatSheet stats = StatSheet.of(Realm.QI_1, root("목"), List.of(), 80, 0);

        assertThat(stats.qiBloodCurrent()).isZero();
    }

    @Test
    void manaCurrentFromLost() {
        StatSheet stats = StatSheet.of(Realm.QI_1, root("목"), List.of(), 0, 5.5);

        assertThat(stats.mana()).isEqualTo(25);
        assertThat(stats.manaCurrent()).isEqualTo(19.5);
    }

    @Test
    void manaCurrentNotBelowZero() {
        StatSheet stats = StatSheet.of(Realm.QI_1, root("목"), List.of(), 0, 40);

        assertThat(stats.manaCurrent()).isZero();
    }

    @Test
    void bonusSumOfLearnedStages() {
        assertThat(learned(changchun()).bonusSum()).isEqualTo(StageBonusSum.ZERO);
        assertThat(learned(changchun(), 3).bonusSum()).isEqualTo(new StageBonusSum(4, 1, 0, 0, 0, 0, 2));
        assertThat(CharacterTechnique.learn(createCharacter(), changchun())
                .getStage()).isEqualTo(1);
    }
}
