package mudmk3.domain.technique;

import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Realm;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static mudmk3.domain.technique.TechniqueFixture.bodyStage;
import static mudmk3.domain.technique.TechniqueFixture.changchunInfo;
import static mudmk3.domain.technique.TechniqueFixture.changchunStages;
import static mudmk3.domain.technique.TechniqueFixture.changchunWith;
import static mudmk3.domain.technique.TechniqueFixture.changchunWithStages;
import static mudmk3.domain.technique.TechniqueFixture.fireBall;
import static mudmk3.domain.technique.TechniqueFixture.ironFist;
import static mudmk3.domain.technique.TechniqueFixture.stage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TechniqueTest {

    @Test
    void createChangchun() {
        Technique technique = TechniqueFixture.changchun();

        assertThat(technique.getName()).isEqualTo("장춘공");
        assertThat(technique.getElements()).containsExactly(Element.WOOD);
        assertThat(technique.elementNames()).contains("목");
        assertThat(technique.isBodyRefining()).isFalse();
        assertThat(technique.lineName()).isEqualTo("오행");
        assertThat(technique.getStartRealm()).isEqualTo(Realm.MORTAL);
        assertThat(technique.stageCount()).isEqualTo(9);
        assertThat(technique.stage(1)).isEqualTo(new TechniqueStage(0, 0, 0, 0, 0, 0, 0, 0, 0));
        assertThat(technique.stage(9)).isEqualTo(new TechniqueStage(1_540, 0, 2, 0, 1, 0, 0, 0, 1));
        assertThat(technique.getStages().stream().mapToInt(TechniqueStage::manaBonus).sum()).isEqualTo(16);
        assertThat(technique.getStages().stream().mapToInt(TechniqueStage::senseBonus).sum()).isEqualTo(4);
        assertThat(technique.getStages().stream().mapToInt(TechniqueStage::spiritBonus).sum()).isEqualTo(2);
        assertThat(technique.getStages().stream().mapToInt(TechniqueStage::aptitudeGain).sum()).isEqualTo(8);
        assertThat(technique.getAbilities()).extracting(Ability::getName).containsExactly("화탄술", "천안술", "어풍결");
        Ability fireBall = technique.getAbilities().getFirst();
        assertThat(fireBall.getLearnStage()).isEqualTo(1);
        assertThat(fireBall.getKind()).isEqualTo(AbilityKind.ATTACK);
        assertThat(fireBall.getElements()).containsExactly(Element.FIRE);
        assertThat(fireBall.getTarget()).isEqualTo(AbilityTarget.SINGLE);
        assertThat(fireBall.getCostStat()).isEqualTo(CostStat.MANA);
        assertThat(fireBall.getCostAmount()).isEqualByComparingTo("5");
        assertThat(fireBall.getCooldownSeconds()).isEqualByComparingTo("3");
        assertThat(fireBall.getUsableWhen()).isEqualTo(UsableWhen.COMBAT_ONLY);
    }

    @Test
    void createIronBody() {
        Technique technique = TechniqueFixture.ironBody();

        assertThat(technique.getElements()).isEmpty();
        assertThat(technique.elementNames()).isEmpty();
        assertThat(technique.isBodyRefining()).isTrue();
        assertThat(technique.lineName()).isEqualTo("연체술");
        assertThat(technique.stageCount()).isEqualTo(9);
        assertThat(technique.getStages().stream().mapToInt(TechniqueStage::bodyBonus).sum()).isEqualTo(4);
        assertThat(technique.getStages().stream().mapToInt(TechniqueStage::strengthBonus).sum()).isEqualTo(4);
        assertThat(technique.getAbilities()).singleElement().satisfies(ability -> {
            assertThat(ability.getName()).isEqualTo("철권");
            assertThat(ability.isBodyRefining()).isTrue();
            assertThat(ability.getCostStat()).isEqualTo(CostStat.NONE);
        });
    }

    @Test
    void createGreenWood() {
        Technique technique = TechniqueFixture.greenWood();

        assertThat(technique.getStartRealm()).isEqualTo(Realm.QI_7);
        assertThat(technique.stageCount()).isEqualTo(8);
        assertThat(technique.stage(1).aptitudeRequired()).isEqualTo(15);
        assertThat(technique.stage(5).aptitudeRequired()).isEqualTo(20);
        assertThat(technique.abilitiesUpTo(3)).extracting(Ability::getName).containsExactly("청목전");
        assertThat(technique.abilitiesUpTo(4)).extracting(Ability::getName).containsExactly("청목전", "목령갑");
    }

    @Test
    void abilitiesUpTo() {
        Technique technique = TechniqueFixture.changchun();

        assertThat(technique.abilitiesUpTo(1)).extracting(Ability::getName).containsExactly("화탄술");
        assertThat(technique.abilitiesUpTo(9)).hasSize(3);
    }

    @Test
    void elementsSortedAndNamesJoined() {
        Technique technique = Technique.create(new TechniqueSaveInfo("화토공", "", List.of(Element.EARTH, Element.FIRE),
                Realm.MORTAL, List.of(stage(0, 0, 0, 0, 0, 0)), List.of(TechniqueFixture.upToStage(fireBall(), 1))));

        assertThat(technique.getElements()).containsExactly(Element.FIRE, Element.EARTH);
        assertThat(technique.elementNames()).contains("화·토");
    }

    @Test
    void nameAndDescriptionStripped() {
        Technique technique = Technique.create(new TechniqueSaveInfo("  장춘공 ", "  묘사 ", List.of(Element.WOOD),
                Realm.MORTAL, changchunStages(), List.of(fireBall())));

        assertThat(technique.getName()).isEqualTo("장춘공");
        assertThat(technique.getDescription()).isEqualTo("묘사");
    }

    @Test
    void update() {
        Technique technique = TechniqueFixture.changchun();

        technique.update(new TechniqueSaveInfo("청목결", "새 묘사", List.of(Element.WOOD, Element.FIRE), Realm.QI_7,
                List.of(stage(0, 15, 0, 0, 0, 0), stage(1_800, 15, 3, 1, 0, 0)),
                List.of(TechniqueFixture.upToStage(fireBall(), 2))));

        assertThat(technique.getName()).isEqualTo("청목결");
        assertThat(technique.getDescription()).isEqualTo("새 묘사");
        assertThat(technique.getElements()).containsExactly(Element.WOOD, Element.FIRE);
        assertThat(technique.getStartRealm()).isEqualTo(Realm.QI_7);
        assertThat(technique.stageCount()).isEqualTo(2);
        assertThat(technique.getAbilities()).extracting(Ability::getName).containsExactly("화탄술");
    }

    @Test
    void updateFailKeepsValues() {
        Technique technique = TechniqueFixture.changchun();

        assertThatThrownBy(() -> technique.update(changchunInfo("장 춘공")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(technique.getName()).isEqualTo("장춘공");
        assertThat(technique.getAbilities()).hasSize(3);
    }

    @Test
    void nameWithSpaceFail() {
        assertFail(changchunInfo("장 춘공"), "공법 이름은 띄어쓰기 없이 1~12자로 정하십시오.");
        assertFail(changchunInfo("   "), "공법 이름은 띄어쓰기 없이 1~12자로 정하십시오.");
    }

    @Test
    void nameTooLongFail() {
        assertFail(changchunInfo("가나다라마바사아자차카타파"), "공법 이름은 띄어쓰기 없이 1~12자로 정하십시오.");
        assertThat(Technique.create(changchunInfo("가나다라마바사아자차카타")).getName()).hasSize(12);
    }

    @Test
    void descriptionTooLongFail() {
        TechniqueSaveInfo info = new TechniqueSaveInfo("장춘공", "가".repeat(501), List.of(Element.WOOD), Realm.MORTAL,
                changchunStages(), List.of(fireBall()));

        assertFail(info, "공법 묘사는 500자 이내로 적으십시오.");
        assertThat(Technique.create(new TechniqueSaveInfo("장춘공", "가".repeat(500), List.of(Element.WOOD),
                Realm.MORTAL, changchunStages(), List.of(fireBall()))).getDescription()).hasSize(500);
    }

    @Test
    void duplicateElementFail() {
        TechniqueSaveInfo info = new TechniqueSaveInfo("장춘공", "", List.of(Element.WOOD, Element.FIRE, Element.WOOD),
                Realm.MORTAL, changchunStages(), List.of(fireBall()));

        assertFail(info, "같은 속성이 두 번 있습니다: 목");
    }

    @Test
    void stageCountFail() {
        assertFail(changchunWithStages(List.of()), "단계 수는 1~12로 정하십시오.");
        List<TechniqueStage> thirteen = new ArrayList<>(List.of(stage(0, 0, 0, 0, 0, 0)));
        thirteen.addAll(Collections.nCopies(12, stage(100, 0, 0, 0, 0, 0)));
        assertFail(changchunWithStages(thirteen), "단계 수는 1~12로 정하십시오.");
        assertThat(Technique.create(changchunWithStages(thirteen.subList(0, 12))).stageCount()).isEqualTo(12);
    }

    @Test
    void firstStageInsightFail() {
        assertFail(changchunWithStages(List.of(stage(10, 0, 0, 0, 0, 0))),
                "1성은 읽으면 이르므로 필요 깨달음을 둘 수 없습니다.");
    }

    @Test
    void insightRangeFail() {
        assertFail(changchunWithStages(List.of(stage(0, 0, 0, 0, 0, 0), stage(120, 0, 0, 0, 0, 0),
                        stage(0, 0, 0, 0, 0, 0))),
                "필요 깨달음은 1~100,000,000으로 정하십시오. (3성)");
        assertFail(changchunWithStages(List.of(stage(0, 0, 0, 0, 0, 0), stage(100_000_001, 0, 0, 0, 0, 0))),
                "필요 깨달음은 1~100,000,000으로 정하십시오. (2성)");
    }

    @Test
    void insightNotDivisibleFail() {
        TechniqueSaveInfo info = new TechniqueSaveInfo("화토공", "", List.of(Element.FIRE, Element.EARTH), Realm.MORTAL,
                List.of(stage(0, 0, 0, 0, 0, 0), stage(200, 0, 0, 0, 0, 0), stage(201, 0, 0, 0, 0, 0)),
                List.of(fireBall()));

        assertFail(info, "필요 깨달음은 속성 수(2)로 나누어떨어져야 합니다. (3성)");
    }

    @Test
    void bodyRefiningAptitudeFail() {
        TechniqueSaveInfo condition = new TechniqueSaveInfo("강체결", "", List.of(), Realm.MORTAL,
                List.of(new TechniqueStage(0, 10, 0, 0, 0, 0, 0, 0, 0)), List.of(ironFist()));
        TechniqueSaveInfo gain = new TechniqueSaveInfo("강체결", "", List.of(), Realm.MORTAL,
                List.of(bodyStage(0, 0, 0), new TechniqueStage(530, 0, 0, 0, 0, 0, 0, 0, 1)), List.of(ironFist()));

        assertFail(condition, "연체술 공법에는 영근 자질 조건·증가를 둘 수 없습니다.");
        assertFail(gain, "연체술 공법에는 영근 자질 조건·증가를 둘 수 없습니다.");
    }

    @Test
    void fiveElementBodyFail() {
        assertFail(changchunWithStages(List.of(new TechniqueStage(0, 0, 0, 0, 0, 0, 1, 0, 0))),
                "육신·근력은 연체술 공법만 올립니다.");
        assertFail(changchunWithStages(List.of(new TechniqueStage(0, 0, 0, 0, 0, 0, 0, 1, 0))),
                "육신·근력은 연체술 공법만 올립니다.");
    }

    @Test
    void stageValueRangeFail() {
        assertFail(changchunWithStages(List.of(stage(0, 0, 0, 0, 0, 0), stage(120, 0, 0, 0, 0, 0),
                        stage(190, 0, 10_001, 0, 0, 0))),
                "단계 값이 범위를 벗어났습니다. (3성 법력)");
        assertFail(changchunWithStages(List.of(stage(0, 1_001, 0, 0, 0, 0))),
                "단계 값이 범위를 벗어났습니다. (1성 자질 조건)");
        assertFail(changchunWithStages(List.of(stage(0, 0, 0, -1, 0, 0))),
                "단계 값이 범위를 벗어났습니다. (1성 의식)");
        assertFail(changchunWithStages(List.of(stage(0, 0, 0, 0, 0, 101))),
                "단계 값이 범위를 벗어났습니다. (1성 영근 자질)");
        assertFail(changchunWithStages(List.of(new TechniqueStage(0, 0, 0, 0, 0, 10_001, 0, 0, 0))),
                "단계 값이 범위를 벗어났습니다. (1성 정혈)");
        TechniqueSaveInfo body = new TechniqueSaveInfo("강체결", "", List.of(), Realm.MORTAL,
                List.of(bodyStage(0, 10_000, 10_001)), List.of(ironFist()));
        assertFail(body, "단계 값이 범위를 벗어났습니다. (1성 근력)");
    }

    @Test
    void checkOrderFirstFailureOnly() {
        // 이름과 단계 수가 모두 틀리면 이름 문구만
        TechniqueSaveInfo info = new TechniqueSaveInfo("장 춘공", "", List.of(Element.WOOD), Realm.MORTAL,
                List.of(), List.of());
        assertFail(info, "공법 이름은 띄어쓰기 없이 1~12자로 정하십시오.");
        // 3성 범위 위반보다 2성 범위 위반이 먼저
        assertFail(changchunWithStages(List.of(stage(0, 0, 0, 0, 0, 0), stage(120, 0, -1, 0, 0, 0),
                        stage(0, 0, 0, 0, 0, 0))),
                "단계 값이 범위를 벗어났습니다. (2성 법력)");
        // 단계 위반이 신통 수 위반보다 먼저
        assertFail(new TechniqueSaveInfo("장춘공", "", List.of(Element.WOOD), Realm.MORTAL,
                        List.of(stage(1, 0, 0, 0, 0, 0)), List.of()),
                "1성은 읽으면 이르므로 필요 깨달음을 둘 수 없습니다.");
    }

    @Test
    void abilityCountFail() {
        assertFail(changchunWith(List.of()), "신통은 1~30개로 정하십시오.");
        List<AbilitySaveInfo> many = new ArrayList<>();
        for (int i = 0; i < 31; i++) {
            many.add(TechniqueFixture.ability("화탄술" + i, 1, AbilityKind.ATTACK, List.of(Element.FIRE),
                    AbilityTarget.SINGLE, CostStat.MANA, "5", "0", "3", UsableWhen.COMBAT_ONLY));
        }
        assertFail(changchunWith(many), "신통은 1~30개로 정하십시오.");
        assertThat(Technique.create(changchunWith(many.subList(0, 30))).getAbilities()).hasSize(30);
    }

    @Test
    void noFirstStageAbilityFail() {
        assertFail(changchunWith(List.of(TechniqueFixture.heavenEye(), TechniqueFixture.windRiding())),
                "1성에 배우는 신통이 하나 이상 있어야 합니다.");
    }

    @Test
    void stageOutOfRangeFail() {
        Technique technique = TechniqueFixture.changchun();

        assertThatThrownBy(() -> technique.stage(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> technique.stage(10)).isInstanceOf(IllegalArgumentException.class);
    }

    static void assertFail(TechniqueSaveInfo info, String message) {
        assertThatThrownBy(() -> Technique.create(info))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(message);
    }
}
