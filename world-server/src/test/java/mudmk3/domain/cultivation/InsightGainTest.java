package mudmk3.domain.cultivation;

import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Realm;
import mudmk3.domain.technique.Ability;
import mudmk3.domain.technique.AbilityKind;
import mudmk3.domain.technique.AbilitySaveInfo;
import mudmk3.domain.technique.AbilityTarget;
import mudmk3.domain.technique.CostStat;
import mudmk3.domain.technique.Technique;
import mudmk3.domain.technique.TechniqueFixture;
import mudmk3.domain.technique.TechniqueSaveInfo;
import mudmk3.domain.technique.UsableWhen;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static mudmk3.domain.technique.TechniqueFixture.ability;
import static mudmk3.domain.technique.TechniqueFixture.changchunStages;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** 룰북 깨달음 절·command.md {@code ### 깨달음 쌓기} 의 예 */
class InsightGainTest {

    @Test
    void baseAmountIsManaCost() {
        assertThat(InsightGain.baseAmount(fireBall(), Realm.MORTAL)).isEqualTo(5.0);
    }

    @Test
    void baseAmountForNoCost() {
        // 철권: 강체결은 범인부터 → 기준 경지 연기 1층, 25 ÷ 5 × 4/3
        Technique ironBody = TechniqueFixture.ironBody();
        Ability ironFist = abilityOf(ironBody, "철권");

        assertThat(InsightGain.baseAmount(ironFist, ironBody.getStartRealm())).isCloseTo(6.6667, within(1e-4));
    }

    @Test
    void situationFactor() {
        assertThat(InsightGain.situationFactor(Realm.QI_1, Realm.QI_2)).isEqualTo(1.0);
        assertThat(InsightGain.situationFactor(Realm.QI_7, Realm.QI_4)).isEqualTo(0.5);
        assertThat(InsightGain.situationFactor(Realm.QI_7, Realm.QI_5)).isEqualTo(1.0);
        assertThat(InsightGain.situationFactor(Realm.FOUNDATION_EARLY, Realm.QI_13)).isEqualTo(0.1);
        assertThat(InsightGain.situationFactor(Realm.QI_1, Realm.FOUNDATION_EARLY)).isEqualTo(1.0);
        assertThat(InsightGain.situationFactor(Realm.FOUNDATION_PEAK, Realm.FOUNDATION_EARLY)).isEqualTo(0.5);
    }

    @Test
    void splitFireballInChangchun() {
        Map<InsightKind, Double> split = InsightGain.split(5, fireBall(), List.of(Element.WOOD));

        assertThat(split).containsOnly(Map.entry(InsightKind.FIRE, 5.0), Map.entry(InsightKind.WOOD, 2.5));
    }

    @Test
    void splitWoodAbilityInWood() {
        Ability heavenEye = abilityOf(TechniqueFixture.changchun(), "천안술");

        assertThat(InsightGain.split(3, heavenEye, List.of(Element.WOOD)))
                .containsOnly(Map.entry(InsightKind.WOOD, 3.0));
    }

    @Test
    void splitMultiElement() {
        Ability fireEarth = attack("화토결", List.of(Element.FIRE, Element.EARTH), List.of(Element.FIRE, Element.EARTH));
        Ability fire = attack("화염결", List.of(Element.FIRE), List.of(Element.FIRE, Element.EARTH));

        assertThat(InsightGain.split(6, fireEarth, List.of(Element.FIRE, Element.EARTH)))
                .containsOnly(Map.entry(InsightKind.FIRE, 3.0), Map.entry(InsightKind.EARTH, 3.0));
        assertThat(InsightGain.split(4, fire, List.of(Element.FIRE, Element.EARTH)))
                .containsOnly(Map.entry(InsightKind.FIRE, 4.0), Map.entry(InsightKind.EARTH, 2.0));
    }

    @Test
    void splitBodyRefining() {
        Technique ironBody = TechniqueFixture.ironBody();
        Ability ironFist = abilityOf(ironBody, "철권");
        double g = InsightGain.baseAmount(ironFist, ironBody.getStartRealm());

        Map<InsightKind, Double> split = InsightGain.split(g, ironFist, ironBody.getElements());

        assertThat(split).containsOnlyKeys(InsightKind.BODY);
        assertThat(split.get(InsightKind.BODY)).isCloseTo(6.67, within(0.01));
    }

    @Test
    void splitNoElementInElementalTechnique() {
        Ability plain = attack("기공탄", List.of(), List.of(Element.WOOD));

        assertThat(InsightGain.split(4, plain, List.of(Element.WOOD)))
                .containsOnly(Map.entry(InsightKind.WOOD, 2.0));
    }

    @Test
    void cappedScales() {
        Map<InsightKind, Double> capped = InsightGain.capped(
                Map.of(InsightKind.FIRE, 5.0, InsightKind.WOOD, 2.5), 4);

        assertThat(capped).containsOnlyKeys(InsightKind.FIRE, InsightKind.WOOD);
        assertThat(capped.get(InsightKind.FIRE)).isCloseTo(2.667, within(1e-3));
        assertThat(capped.get(InsightKind.WOOD)).isCloseTo(1.333, within(1e-3));
    }

    @Test
    void cappedUnderRemainingKeepsGains() {
        Map<InsightKind, Double> gains = Map.of(InsightKind.FIRE, 5.0, InsightKind.WOOD, 2.5);

        assertThat(InsightGain.capped(gains, 100)).isEqualTo(gains);
    }

    @Test
    void cappedZero() {
        assertThat(InsightGain.capped(Map.of(InsightKind.FIRE, 5.0), 0)).isEmpty();
        assertThat(InsightGain.capped(Map.of(InsightKind.FIRE, 5.0), -1)).isEmpty();
    }

    private static Ability fireBall() {
        return abilityOf(TechniqueFixture.changchun(), "화탄술");
    }

    /** {@code techniqueElements} 공법(범인부터)에 든 {@code abilityElements} 속성 공격 신통 */
    private static Ability attack(String name, List<Element> abilityElements, List<Element> techniqueElements) {
        AbilitySaveInfo info = ability(name, 1, AbilityKind.ATTACK, abilityElements, AbilityTarget.SINGLE,
                CostStat.MANA, "5", "0", "3", UsableWhen.COMBAT_ONLY);
        Technique technique = Technique.create(new TechniqueSaveInfo("시험공", "", techniqueElements, Realm.MORTAL,
                changchunStages(), List.of(info)));
        return abilityOf(technique, name);
    }

    private static Ability abilityOf(Technique technique, String name) {
        return technique.getAbilities().stream()
                .filter(each -> each.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
