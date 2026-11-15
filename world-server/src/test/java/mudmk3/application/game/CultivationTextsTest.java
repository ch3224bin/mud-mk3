package mudmk3.application.game;

import mudmk3.application.cultivation.provided.ReadResult;
import mudmk3.domain.character.PlayerCharacter;
import mudmk3.domain.character.SpiritRoot;
import mudmk3.domain.cultivation.CharacterTechnique;
import mudmk3.domain.cultivation.Insight;
import mudmk3.domain.cultivation.InsightKind;
import mudmk3.domain.cultivation.StatSheet;
import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Realm;
import mudmk3.domain.technique.AbilityKind;
import mudmk3.domain.technique.AbilitySaveInfo;
import mudmk3.domain.technique.AbilityTarget;
import mudmk3.domain.technique.CostStat;
import mudmk3.domain.technique.Technique;
import mudmk3.domain.technique.TechniqueFixture;
import mudmk3.domain.technique.TechniqueSaveInfo;
import mudmk3.domain.technique.TechniqueStage;
import mudmk3.domain.technique.UsableWhen;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static mudmk3.domain.character.CharacterFixture.createCharacter;
import static mudmk3.domain.cultivation.CharacterTechniqueFixture.learned;
import static mudmk3.domain.technique.TechniqueFixture.ability;
import static mudmk3.domain.technique.TechniqueFixture.changchun;
import static mudmk3.domain.technique.TechniqueFixture.fireBall;
import static mudmk3.domain.technique.TechniqueFixture.greenWood;
import static mudmk3.domain.technique.TechniqueFixture.ironBody;
import static mudmk3.domain.technique.TechniqueFixture.stage;
import static org.assertj.core.api.Assertions.assertThat;

class CultivationTextsTest {

    private final CultivationTexts texts = new CultivationTexts();

    @Test
    void statusQi1() {
        PlayerCharacter han = character("목 화", Realm.QI_1);
        StatSheet stats = StatSheet.of(Realm.QI_1, han.findSpiritRoot().orElseThrow(), List.of(learned(changchun())), 0, 0);

        assertThat(texts.status(han, stats, Insight.empty())).isEqualTo("""
                한백 · 연기 1층 · 목·화 진영근 (목 50, 화 50)
                기혈  60 / 60     법력  25 / 25
                의식  20 / 20     정혈  20 / 20
                육신  6   근력  3   영력  5 / 5
                수위  0 / 190
                깨달음  금 0 · 목 0 · 수 0 · 화 0 · 토 0 · 연체 0""");
    }

    @Test
    void statusMortal() {
        PlayerCharacter han = character("화", Realm.MORTAL);
        StatSheet stats = StatSheet.of(Realm.MORTAL, han.findSpiritRoot().orElseThrow(), List.of(), 0, 0);

        assertThat(texts.status(han, stats, Insight.empty())).isEqualTo("""
                한백 · 범인 · 화 천영근 (화 100)
                기혈  10 / 10     법력  0 / 0
                의식  10 / 10     정혈  10 / 10
                육신  1   근력  1   영력  0 / 0
                깨달음  금 0 · 목 0 · 수 0 · 화 0 · 토 0 · 연체 0""");
    }

    @Test
    void statusCommaNumbers() {
        PlayerCharacter han = character("목 화", Realm.QI_13);
        StatSheet stats = StatSheet.of(Realm.QI_13, han.findSpiritRoot().orElseThrow(), List.of(), 0, 0);

        assertThat(texts.status(han, stats, Insight.empty()).lines().toList()).contains("수위  0 / 11,000");
    }

    @Test
    void statusShowsAptitudeWithGainFloored() {
        PlayerCharacter han = character("금 목 화", Realm.QI_3);
        StatSheet stats = StatSheet.of(Realm.QI_3, han.findSpiritRoot().orElseThrow(),
                List.of(learned(changchun(), 5)), 0, 0);

        assertThat(texts.status(han, stats, Insight.empty()).lines().findFirst().orElseThrow())
                .isEqualTo("한백 · 연기 3층 · 금·목·화 진영근 (금 33, 목 37, 화 33)");
    }

    @Test
    void techniquesLine() {
        assertThat(texts.techniques(Realm.QI_1, List.of(learned(changchun())), Insight.empty()))
                .isEqualTo("장춘공  1/9성 · 목 · 오행  다음 2성: 목 깨달음 120 (지금 0)");
    }

    @Test
    void bodyRefiningLine() {
        assertThat(texts.techniques(Realm.QI_1, List.of(learned(ironBody())), Insight.empty()))
                .isEqualTo("강체결  1/9성 · 연체  다음 2성: 연체 깨달음 530 (지금 0)");
    }

    @Test
    void techniquesInLearningOrder() {
        assertThat(texts.techniques(Realm.QI_1, List.of(learned(changchun()), learned(ironBody())), Insight.empty()))
                .isEqualTo("""
                장춘공  1/9성 · 목 · 오행  다음 2성: 목 깨달음 120 (지금 0)
                강체결  1/9성 · 연체  다음 2성: 연체 깨달음 530 (지금 0)""");
    }

    @Test
    void multiElementSplitsInsight() {
        Technique fireEarth = Technique.create(new TechniqueSaveInfo("화토공", "", List.of(Element.EARTH, Element.FIRE),
                Realm.MORTAL, List.of(stage(0, 0, 0, 0, 0, 0), stage(400, 0, 0, 0, 0, 0)),
                List.of(TechniqueFixture.upToStage(fireBall(), 2))));

        assertThat(texts.techniques(Realm.QI_1, List.of(learned(fireEarth)), Insight.empty()))
                .isEqualTo("화토공  1/2성 · 화·토 · 오행  다음 2성: 화 깨달음 200 (지금 0) · 토 깨달음 200 (지금 0)");
    }

    @Test
    void lastStageLine() {
        Technique single = Technique.create(new TechniqueSaveInfo("목령공", "", List.of(Element.WOOD), Realm.MORTAL,
                List.of(stage(0, 0, 0, 0, 0, 0)), List.of(TechniqueFixture.upToStage(fireBall(), 1))));

        assertThat(texts.techniques(Realm.QI_1, List.of(learned(single)), Insight.empty()))
                .isEqualTo("목령공  1/1성 · 끝에 이름 · 목 · 오행");
    }

    @Test
    void realmTooLowSuffix() {
        assertThat(texts.techniques(Realm.QI_3, List.of(learned(greenWood())), Insight.empty()))
                .isEqualTo("청목결  1/8성 · 목 · 오행  다음 2성: 목 깨달음 1,800 (지금 0)  (연기 7층 필요)");
        assertThat(texts.techniques(Realm.QI_7, List.of(learned(greenWood())), Insight.empty()))
                .doesNotContain("필요");
    }

    @Test
    void statusInsightFloored() {
        PlayerCharacter han = character("목 화", Realm.QI_1);
        StatSheet stats = StatSheet.of(Realm.QI_1, han.findSpiritRoot().orElseThrow(), List.of(), 0, 0);
        Insight insight = new Insight(Map.of(InsightKind.FIRE, 230.9, InsightKind.WOOD, 112.2));

        assertThat(texts.status(han, stats, insight).lines().toList())
                .last().isEqualTo("깨달음  금 0 · 목 112 · 수 0 · 화 230 · 토 0 · 연체 0");
    }

    @Test
    void techniquesShowCurrentInsight() {
        Insight insight = new Insight(Map.of(InsightKind.WOOD, 2.5, InsightKind.BODY, 6.67));

        assertThat(texts.techniques(Realm.QI_1, List.of(learned(changchun()), learned(ironBody())), insight))
                .isEqualTo("""
                        장춘공  1/9성 · 목 · 오행  다음 2성: 목 깨달음 120 (지금 2)
                        강체결  1/9성 · 연체  다음 2성: 연체 깨달음 530 (지금 6)""");
    }

    @Test
    void multiElementShowsEachInsight() {
        Technique fireEarth = Technique.create(new TechniqueSaveInfo("화토공", "", List.of(Element.EARTH, Element.FIRE),
                Realm.MORTAL, List.of(stage(0, 0, 0, 0, 0, 0), stage(400, 0, 0, 0, 0, 0)),
                List.of(TechniqueFixture.upToStage(fireBall(), 2))));
        Insight insight = new Insight(Map.of(InsightKind.FIRE, 5.0, InsightKind.EARTH, 2.5));

        assertThat(texts.techniques(Realm.QI_1, List.of(learned(fireEarth)), insight))
                .isEqualTo("화토공  1/2성 · 화·토 · 오행  다음 2성: 화 깨달음 200 (지금 5) · 토 깨달음 200 (지금 2)");
    }

    @Test
    void noTechniques() {
        assertThat(texts.techniques(Realm.QI_1, List.of(), Insight.empty())).isEqualTo("익힌 공법이 없습니다.");
    }

    @Test
    void abilitiesBlock() {
        assertThat(texts.abilities(List.of(learned(changchun()), learned(ironBody())))).isEqualTo("""
                [장춘공 1성]
                  화탄술  공격·화  법력 5  대기 3초
                [강체결 1성]
                  철권  공격·연체  법력 없음  대기 4초""");
    }

    @Test
    void abilitiesUpToStage() {
        assertThat(texts.abilities(List.of(learned(changchun(), 2)))).isEqualTo("""
                [장춘공 2성]
                  화탄술  공격·화  법력 5  대기 3초
                  천안술  탐지·목  법력 3  대기 10초""");
    }

    @Test
    void castTimeShown() {
        assertThat(abilityLine(ability("화탄술", 1, AbilityKind.ATTACK, List.of(Element.FIRE), AbilityTarget.SINGLE,
                CostStat.MANA, "5", "2", "3", UsableWhen.COMBAT_ONLY)))
                .isEqualTo("  화탄술  공격·화  법력 5  시전 2초  대기 3초");
    }

    @Test
    void perTickCost() {
        assertThat(abilityLine(new AbilitySaveInfo("호신술", 1, AbilityKind.DEFENSE, List.of(Element.FIRE, Element.EARTH),
                false, AbilityTarget.SELF, CostStat.SENSE, new BigDecimal("1"), true, BigDecimal.ZERO,
                new BigDecimal("90"), UsableWhen.ANYTIME, List.of("호신술을 두른다."), List.of())))
                .isEqualTo("  호신술  방어·화·토  의식 1 (틱마다)  대기 1분 30초");
    }

    @Test
    void noElementAbility() {
        assertThat(abilityLine(ability("정심술", 1, AbilityKind.BUFF, List.of(), AbilityTarget.SELF,
                CostStat.MANA, "1.5", "0", "10", UsableWhen.ANYTIME)))
                .isEqualTo("  정심술  보조  법력 1.5  대기 10초");
    }

    @Test
    void noAbilities() {
        assertThat(texts.abilities(List.of())).isEqualTo("배운 신통이 없습니다.");
    }

    // ---- 읽기

    @Test
    void readSuccessInitiates() {
        ReadResult.Learned learned = new ReadResult.Learned("장춘공", List.of(Element.WOOD), changchun().stage(1),
                List.of("화탄술"), true);

        assertThat(texts.readSuccess(learned)).isEqualTo("""
                옥간을 이마에 대자 구결이 머릿속으로 흘러든다. 장춘공 1성에 이르렀습니다!
                단전에 따뜻한 기운이 맺힌다. 연기 1층이 되었습니다. 새 신통: 화탄술""");
    }

    @Test
    void readSuccessNonMortal() {
        ReadResult.Learned learned = new ReadResult.Learned("청목결", List.of(Element.WOOD), greenWood().stage(1),
                List.of("청목전"), false);

        assertThat(texts.readSuccess(learned)).isEqualTo("""
                옥간을 이마에 대자 구결이 머릿속으로 흘러든다. 청목결 1성에 이르렀습니다!
                새 신통을 깨쳤습니다: 청목전""");
    }

    @Test
    void readSuccessManyAbilities() {
        ReadResult.Learned learned = new ReadResult.Learned("장춘공", List.of(Element.WOOD), changchun().stage(1),
                List.of("화탄술", "천안술"), false);

        assertThat(texts.readSuccess(learned).lines().toList().getLast()).isEqualTo("새 신통을 깨쳤습니다: 화탄술, 천안술");
    }

    @Test
    void readWithStageBonus() {
        ReadResult.Learned learned = new ReadResult.Learned("장춘공", List.of(Element.WOOD), stage(0, 0, 2, 0, 0, 1),
                List.of("화탄술"), false);

        assertThat(texts.readSuccess(learned)).isEqualTo("""
                옥간을 이마에 대자 구결이 머릿속으로 흘러든다. 장춘공 1성에 이르렀습니다!
                법력 +2 · 목영근 자질 +1
                새 신통을 깨쳤습니다: 화탄술""");
    }

    @Test
    void readWithEveryBonusInOrder() {
        ReadResult.Learned learned = new ReadResult.Learned("시험공", List.of(Element.FIRE, Element.EARTH),
                new TechniqueStage(0, 0, 1, 2, 3, 4, 5, 6, 1), List.of("화탄술"), false);

        assertThat(texts.readSuccess(learned).lines().skip(1).findFirst().orElseThrow())
                .isEqualTo("법력 +1 · 의식 +2 · 영력 +3 · 정혈 +4 · 육신 +5 · 근력 +6 · 화영근 자질 +1 · 토영근 자질 +1");
    }

    @Test
    void readFailures() {
        assertThat(texts.readFailure(new ReadResult.NotScroll())).isEqualTo("읽을 수 있는 물건이 아닙니다.");
        assertThat(texts.readFailure(new ReadResult.NoTechnique())).isEqualTo("아무것도 적혀 있지 않습니다.");
        assertThat(texts.readFailure(new ReadResult.AlreadyLearned("장춘공", 1, 9)))
                .isEqualTo("이미 깨친 내용입니다. (장춘공 1/9성)");
        assertThat(texts.readFailure(new ReadResult.RealmTooLow(Realm.QI_7)))
                .isEqualTo("지금은 이 공법을 익힐 수 없습니다. (연기 7층 필요)");
        assertThat(texts.readFailure(new ReadResult.MissingRoot(Element.FIRE)))
                .isEqualTo("지금은 이 공법을 익힐 수 없습니다. (화영근 필요)");
        assertThat(texts.readFailure(new ReadResult.LowAptitude(Element.FIRE, 40, 33)))
                .isEqualTo("지금은 이 공법을 익힐 수 없습니다. (화영근 자질 40 필요, 지금 33)");
    }

    // ---- 경지 알림

    @Test
    void realmChanged() {
        assertThat(texts.realmChanged(Realm.QI_1, Realm.QI_7))
                .isEqualTo("경지가 연기 7층으로 바뀌었습니다. (육신 +6, 근력 +3, 영력 +6, 의식 +12, 정혈 +12)");
        assertThat(texts.realmChanged(Realm.QI_7, Realm.FOUNDATION_EARLY))
                .isEqualTo("경지가 축기 초기로 바뀌었습니다. (육신 +24, 근력 +12, 영력 +23, 의식 +56, 정혈 +56)");
        assertThat(texts.realmChanged(Realm.QI_7, Realm.QI_1))
                .isEqualTo("경지가 연기 1층으로 바뀌었습니다. (육신 -6, 근력 -3, 영력 -6, 의식 -12, 정혈 -12)");
        assertThat(texts.realmChanged(Realm.QI_1, Realm.MORTAL))
                .isEqualTo("경지가 범인으로 바뀌었습니다. (육신 -5, 근력 -2, 영력 -5, 의식 -10, 정혈 -10)");
    }

    @Test
    void realmChangedWithoutDifferenceDropsParentheses() {
        assertThat(texts.realmChanged(Realm.QI_1, Realm.QI_1)).isEqualTo("경지가 연기 1층으로 바뀌었습니다.");
    }

    /** 그 신통 하나를 1성에 둔 공법을 익혔을 때 신통 줄 */
    private String abilityLine(AbilitySaveInfo info) {
        Technique technique = Technique.create(new TechniqueSaveInfo("시험공", "", List.of(Element.FIRE), Realm.MORTAL,
                List.of(stage(0, 0, 0, 0, 0, 0)), List.of(TechniqueFixture.upToStage(info, 1))));
        CharacterTechnique learned = learned(technique);
        return texts.abilities(List.of(learned)).lines().skip(1).findFirst().orElseThrow();
    }

    private static PlayerCharacter character(String roots, Realm realm) {
        PlayerCharacter character = createCharacter(1L, "한백", 10L, SpiritRoot.parse(roots).orElseThrow());
        character.changeRealm(realm);
        return character;
    }
}
