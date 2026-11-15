package mudmk3.domain.technique;

import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Realm;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static mudmk3.domain.technique.TechniqueFixture.ability;
import static mudmk3.domain.technique.TechniqueFixture.bodyStage;
import static mudmk3.domain.technique.TechniqueFixture.changchunWith;
import static mudmk3.domain.technique.TechniqueFixture.fireBall;
import static mudmk3.domain.technique.TechniqueFixture.fireBallStagePowers;
import static mudmk3.domain.technique.TechniqueFixture.ironFist;
import static mudmk3.domain.technique.TechniqueFixture.stagePowers;
import static mudmk3.domain.technique.TechniqueFixture.windRiding;
import static mudmk3.domain.technique.TechniqueFixture.withCost;
import static mudmk3.domain.technique.TechniqueFixture.withEffectLines;
import static mudmk3.domain.technique.TechniqueFixture.withStagePowers;
import static mudmk3.domain.technique.TechniqueTest.assertFail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbilityTest {

    @Test
    void nameRuleFail() {
        assertFail(changchunWith(List.of(fireBallNamed("화 탄술"))), "신통 이름은 띄어쓰기 없이 1~12자로 정하십시오.");
        assertFail(changchunWith(List.of(fireBallNamed(""))), "신통 이름은 띄어쓰기 없이 1~12자로 정하십시오.");
        assertFail(changchunWith(List.of(fireBallNamed("가나다라마바사아자차카타파"))),
                "신통 이름은 띄어쓰기 없이 1~12자로 정하십시오.");
    }

    @Test
    void reservedNameFail() {
        assertFail(changchunWith(List.of(fireBall(), fireBallNamed("상태"))), "명령어와 같은 이름은 쓸 수 없습니다: 상태");
        assertFail(changchunWith(List.of(fireBall(), fireBallNamed("E"))), "명령어와 같은 이름은 쓸 수 없습니다: E");
    }

    @Test
    void duplicateNameFail() {
        assertFail(changchunWith(List.of(fireBall(), fireBall())), "같은 신통 이름이 두 번 있습니다: 화탄술");
        assertFail(changchunWith(List.of(fireBallNamed("Fire"), fireBallNamed("fire"))),
                "같은 신통 이름이 두 번 있습니다: fire");
    }

    @Test
    void sameNameInOtherTechnique() {
        Technique other = Technique.create(new TechniqueSaveInfo("적염결", "", List.of(Element.FIRE), Realm.QI_7,
                List.of(TechniqueFixture.stage(0, 15, 0, 0, 0, 0)),
                List.of(withStagePowers(fireBall(), fireBallStagePowers(1)))));
        assertThat(other.getAbilities()).extracting(Ability::getName).containsExactly("화탄술");
    }

    @Test
    void learnStageFail() {
        AbilitySaveInfo tenth = ability("어풍결", 10, AbilityKind.ESCAPE, List.of(Element.WOOD), AbilityTarget.SELF,
                CostStat.MANA, "10", "0", "10", UsableWhen.ANYTIME);
        AbilitySaveInfo zeroth = ability("어풍결", 0, AbilityKind.ESCAPE, List.of(Element.WOOD), AbilityTarget.SELF,
                CostStat.MANA, "10", "0", "10", UsableWhen.ANYTIME);

        assertFail(changchunWith(List.of(fireBall(), tenth)), "배우는 단계는 1성~9성 가운데로 정하십시오: 어풍결");
        assertFail(changchunWith(List.of(fireBall(), zeroth)), "배우는 단계는 1성~9성 가운데로 정하십시오: 어풍결");
    }

    @Test
    void duplicateElementFail() {
        AbilitySaveInfo info = ability("화탄술", 1, AbilityKind.ATTACK, List.of(Element.FIRE, Element.FIRE),
                AbilityTarget.SINGLE, CostStat.MANA, "5", "0", "3", UsableWhen.COMBAT_ONLY);

        assertFail(changchunWith(List.of(info)), "같은 속성이 두 번 있습니다: 화");
    }

    @Test
    void bodyRefiningWithElementFail() {
        AbilitySaveInfo info = new AbilitySaveInfo("철권", 1, AbilityKind.ATTACK, List.of(Element.METAL), true,
                AbilityTarget.SINGLE, CostStat.NONE, BigDecimal.ZERO, false, BigDecimal.ZERO, new BigDecimal("4"),
                UsableWhen.COMBAT_ONLY, List.of("주먹을 내지른다."), List.of());

        assertFail(ironBodyWith(info), "연체 신통에는 오행 속성을 함께 둘 수 없습니다: 철권");
    }

    @Test
    void manaOnBodyRefiningFail() {
        AbilitySaveInfo info = new AbilitySaveInfo("철권", 1, AbilityKind.ATTACK, List.of(), true,
                AbilityTarget.SINGLE, CostStat.MANA, new BigDecimal("5"), false, BigDecimal.ZERO, new BigDecimal("4"),
                UsableWhen.COMBAT_ONLY, List.of("주먹을 내지른다."), List.of());

        assertFail(ironBodyWith(info), "연체술 공법의 신통은 법력을 쓰지 않습니다: 철권");
        // 연체술 공법이라도 정혈 소모는 된다
        AbilitySaveInfo essence = new AbilitySaveInfo("철권", 1, AbilityKind.ATTACK, List.of(), true,
                AbilityTarget.SINGLE, CostStat.ESSENCE, new BigDecimal("1.5"), true, BigDecimal.ZERO,
                new BigDecimal("4"), UsableWhen.COMBAT_ONLY, List.of("주먹을 내지른다."),
                stagePowers(1, 1, "5", "0", "2.2"));
        assertThat(Technique.create(ironBodyWith(essence)).getAbilities().getFirst().isCostPerTick()).isTrue();
    }

    @Test
    void costScaleFail() {
        assertFail(changchunWith(List.of(fireBallWith("5.25", "0", "3"))),
                "소모 양·시간은 소수 첫째 자리까지 적으십시오: 화탄술");
        assertFail(changchunWith(List.of(fireBallWith("5", "0.05", "3"))),
                "소모 양·시간은 소수 첫째 자리까지 적으십시오: 화탄술");
        assertFail(changchunWith(List.of(fireBallWith("5", "0", "3.01"))),
                "소모 양·시간은 소수 첫째 자리까지 적으십시오: 화탄술");
        // 끝의 0은 자리로 세지 않는다
        assertThat(Technique.create(changchunWith(List.of(fireBallWith("5.50", "0.00", "3.0")))).getAbilities())
                .hasSize(1);
    }

    @Test
    void noneWithAmountFail() {
        AbilitySaveInfo info = ability("화탄술", 1, AbilityKind.ATTACK, List.of(Element.FIRE), AbilityTarget.SINGLE,
                CostStat.NONE, "5", "0", "3", UsableWhen.COMBAT_ONLY);

        assertFail(changchunWith(List.of(info)), "소모가 없으면 양을 0으로 두십시오: 화탄술");
    }

    @Test
    void costAmountFail() {
        assertFail(changchunWith(List.of(fireBallWith("0", "0", "3"))), "소모가 있으면 양을 0보다 크게 정하십시오: 화탄술");
        assertFail(changchunWith(List.of(fireBallWith("-1", "0", "3"))), "소모가 있으면 양을 0보다 크게 정하십시오: 화탄술");
        assertFail(changchunWith(List.of(fireBallWith("10000.1", "0", "3"))), "소모 양은 0.1~10,000으로 정하십시오: 화탄술");
        assertThat(Technique.create(changchunWith(List.of(fireBallWith("10000", "0", "3")))).getAbilities()
                .getFirst().getCostAmount()).isEqualByComparingTo("10000");
        assertThat(Technique.create(changchunWith(List.of(fireBallWith("0.1", "0", "3")))).getAbilities()).hasSize(1);
    }

    @Test
    void castRangeFail() {
        assertFail(changchunWith(List.of(fireBallWith("5", "61", "3"))), "시전 시간은 0~60초로 정하십시오: 화탄술");
        assertFail(changchunWith(List.of(fireBallWith("5", "-0.1", "3"))), "시전 시간은 0~60초로 정하십시오: 화탄술");
        assertThat(Technique.create(changchunWith(List.of(fireBallWith("5", "60", "3")))).getAbilities()).hasSize(1);
    }

    @Test
    void cooldownRangeFail() {
        assertFail(changchunWith(List.of(fireBallWith("5", "0", "0"))), "재사용 대기시간은 0.1~3,600초로 정하십시오: 화탄술");
        assertFail(changchunWith(List.of(fireBallWith("5", "0", "3600.1"))),
                "재사용 대기시간은 0.1~3,600초로 정하십시오: 화탄술");
        assertThat(Technique.create(changchunWith(List.of(fireBallWith("5", "0", "0.1")))).getAbilities()).hasSize(1);
        assertThat(Technique.create(changchunWith(List.of(fireBallWith("5", "0", "3600")))).getAbilities()).hasSize(1);
    }

    @Test
    void checkOrderFirstFailureOnly() {
        // 예약어가 배우는 단계보다 먼저
        AbilitySaveInfo reserved = ability("상태", 10, AbilityKind.ATTACK, List.of(Element.FIRE), AbilityTarget.SINGLE,
                CostStat.MANA, "5", "0", "3", UsableWhen.COMBAT_ONLY);
        assertFail(changchunWith(List.of(fireBall(), reserved)), "명령어와 같은 이름은 쓸 수 없습니다: 상태");
        // 소수 자리가 대기시간 범위보다 먼저
        assertFail(changchunWith(List.of(fireBallWith("5.25", "0", "0"))),
                "소모 양·시간은 소수 첫째 자리까지 적으십시오: 화탄술");
        // 신통마다 정의 순서로: 앞 신통의 위반이 먼저
        assertFail(changchunWith(List.of(fireBallWith("5", "61", "3"), fireBallNamed("상태"))),
                "시전 시간은 0~60초로 정하십시오: 화탄술");
    }

    // --- 6차: 공격 신통의 단계별 값 ---

    @Test
    void stagePowersOnNonAttackFail() {
        AbilitySaveInfo wind = withStagePowers(windRiding(), stagePowers(3, 9, "2", "0.2", "1.2"));

        assertFail(changchunWith(List.of(fireBall(), wind)), "단계별 값은 공격 신통에만 둡니다: 어풍결");
    }

    @Test
    void stagePowersMissingFail() {
        assertFail(changchunWith(List.of(withStagePowers(fireBall(), fireBallStagePowers(8)))), STAGE_ROWS);
        assertFail(changchunWith(List.of(withStagePowers(fireBall(), List.of()))), STAGE_ROWS);
    }

    @Test
    void stagePowersExtraFail() {
        assertFail(changchunWith(List.of(withStagePowers(fireBall(), fireBallStagePowers(10)))), STAGE_ROWS);
    }

    @Test
    void stagePowersOrderFail() {
        List<AbilityStagePower> swapped = new ArrayList<>(fireBallStagePowers(9));
        swapped.set(0, fireBallStagePowers(9).get(1));
        swapped.set(1, fireBallStagePowers(9).get(0));

        assertFail(changchunWith(List.of(withStagePowers(fireBall(), swapped))), STAGE_ROWS);
    }

    @Test
    void stagePowersFromLearnStage() {
        AbilitySaveInfo third = withStagePowers(fireBallNamed("화룡술"), stagePowers(3, 9, "2", "0.2", "1.2"));
        AbilitySaveInfo thirdLearned = new AbilitySaveInfo(third.name(), 3, third.kind(), third.elements(),
                third.bodyRefining(), third.target(), third.costStat(), third.costAmount(), third.costPerTick(),
                third.castSeconds(), third.cooldownSeconds(), third.usableWhen(), third.effectLines(),
                third.stagePowers());

        Ability ability = Technique.create(changchunWith(List.of(fireBall(), thirdLearned))).getAbilities().get(1);

        assertThat(ability.getStagePowers()).extracting(AbilityStagePower::stage)
                .containsExactly(3, 4, 5, 6, 7, 8, 9);
        assertThat(ability.stagePower(3)).isPresent();
        assertThat(ability.stagePower(2)).isEmpty();
    }

    @Test
    void basePowerScaleFail() {
        assertFail(fireBallWithThirdStage("2.255", "1.2"), SCALE);
    }

    @Test
    void coefficientScaleFail() {
        assertFail(fireBallWithThirdStage("2.4", "1.2245"), SCALE);
    }

    @Test
    void basePowerRangeFail() {
        assertFail(fireBallWithThirdStage("10000.01", "1.2"), "기본 위력은 0~10,000으로 정하십시오: 화탄술 (3성)");
        assertFail(fireBallWithThirdStage("-1", "1.2"), "기본 위력은 0~10,000으로 정하십시오: 화탄술 (3성)");
        assertThat(Technique.create(fireBallWithThirdStage("10000", "100")).getAbilities()).hasSize(1);
        assertThat(Technique.create(fireBallWithThirdStage("0", "0")).getAbilities()).hasSize(1);
    }

    @Test
    void coefficientRangeFail() {
        assertFail(fireBallWithThirdStage("2.4", "100.001"), "신통 계수는 0~100으로 정하십시오: 화탄술 (3성)");
        assertFail(fireBallWithThirdStage("2.4", "-0.001"), "신통 계수는 0~100으로 정하십시오: 화탄술 (3성)");
    }

    @Test
    void lowerStageReportedFirst() {
        List<AbilityStagePower> powers = new ArrayList<>(fireBallStagePowers(9));
        powers.set(1, new AbilityStagePower(2, new BigDecimal("-1"), new BigDecimal("1.2")));
        powers.set(2, new AbilityStagePower(3, new BigDecimal("2.255"), new BigDecimal("1.2")));

        assertFail(changchunWith(List.of(withStagePowers(fireBall(), powers))),
                "기본 위력은 0~10,000으로 정하십시오: 화탄술 (2성)");
    }

    @Test
    void stagePowerScaleStored() {
        List<AbilityStagePower> powers = new ArrayList<>(fireBallStagePowers(9));
        powers.set(0, new AbilityStagePower(1, new BigDecimal("2"), new BigDecimal("1.2")));

        Ability ability = Technique.create(changchunWith(List.of(withStagePowers(fireBall(), powers))))
                .getAbilities().getFirst();

        AbilityStagePower first = ability.stagePower(1).orElseThrow();
        assertThat(first.basePower()).hasToString("2.00");
        assertThat(first.coefficient()).hasToString("1.200");
        assertThat(ability.stagePower(9).orElseThrow().basePower()).hasToString("3.60");
    }

    // --- 6차: 이펙트 문장 ---

    @Test
    void effectLinesZeroFail() {
        assertFail(fireBallLines(List.of()), "이펙트 문장은 1~5개로 정하십시오: 화탄술");
    }

    @Test
    void effectLinesSixFail() {
        assertFail(fireBallLines(List.of("가", "나", "다", "라", "마", "바")), "이펙트 문장은 1~5개로 정하십시오: 화탄술");
        assertThat(Technique.create(fireBallLines(List.of("가", "나", "다", "라", "마"))).getAbilities()).hasSize(1);
    }

    @Test
    void effectLineBlankFail() {
        assertFail(fireBallLines(List.of("   ")), LENGTH);
    }

    @Test
    void effectLineTooLongFail() {
        assertFail(fireBallLines(List.of("가".repeat(101))), LENGTH);
    }

    @Test
    void effectLineNewlineFail() {
        assertFail(fireBallLines(List.of("불\n꽃")), LENGTH);
        assertFail(fireBallLines(List.of("불\r꽃")), LENGTH);
    }

    @Test
    void effectLineHundredCharsOk() {
        assertThat(Technique.create(fireBallLines(List.of("가".repeat(100)))).getAbilities().getFirst()
                .getEffectLines()).containsExactly("가".repeat(100));
    }

    @Test
    void effectLineStripped() {
        Ability ability = Technique.create(fireBallLines(List.of("  불꽃이 튄다!  "))).getAbilities().getFirst();

        assertThat(ability.getEffectLines()).containsExactly("불꽃이 튄다!");
    }

    @Test
    void unknownPlaceholderFail() {
        assertFail(fireBallLines(List.of("{적}에게 불꽃이 튄다!")), "쓸 수 없는 자리표시가 있습니다: 화탄술 ({적})");
    }

    @Test
    void unclosedBraceFail() {
        assertFail(fireBallLines(List.of("{대상에게 불꽃이 튄다!")), "쓸 수 없는 자리표시가 있습니다: 화탄술 ({)");
    }

    @Test
    void strayCloseBraceFail() {
        assertFail(fireBallLines(List.of("불꽃}")), "쓸 수 없는 자리표시가 있습니다: 화탄술 (})");
    }

    @Test
    void targetPlaceholderOnSelfFail() {
        AbilitySaveInfo wind = withEffectLines(windRiding(), List.of("{대상}을 태운다"));

        assertFail(changchunWith(List.of(fireBall(), wind)), "{대상}은 한 대상에게 쓰는 신통에만 쓸 수 있습니다: 어풍결");
    }

    @Test
    void duplicateEffectLineFail() {
        assertFail(fireBallLines(List.of("불꽃이 튄다!", " 불꽃이 튄다! ")), "같은 이펙트 문장이 두 번 있습니다: 화탄술");
    }

    @Test
    void noPlaceholderOk() {
        assertThat(Technique.create(fireBallLines(List.of("불꽃이 튄다!"))).getAbilities()).hasSize(1);
    }

    @Test
    void casterPlaceholderOnSelfOk() {
        AbilitySaveInfo wind = withEffectLines(windRiding(), List.of("{시전자}의 몸이 바람을 탄다."));

        Ability ability = Technique.create(changchunWith(List.of(fireBall(), wind))).getAbilities().get(1);

        assertThat(ability.getEffectLines()).containsExactly("{시전자}의 몸이 바람을 탄다.");
    }

    @Test
    void effectLinesKeepOrder() {
        Ability ability = Technique.create(changchunWith(List.of(fireBall()))).getAbilities().getFirst();

        assertThat(ability.getEffectLines()).containsExactlyElementsOf(TechniqueFixture.FIRE_BALL_LINES);
        assertThatThrownBy(() -> ability.getEffectLines().add("x")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void nullEffectLineFail() {
        List<String> lines = new ArrayList<>();
        lines.add(null);

        assertThatThrownBy(() -> withEffectLines(fireBall(), lines))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("effectLines 에 빈 값이 있습니다");
    }

    // --- 6차: 검사 순서 ---

    @Test
    void stagePowerCheckedBeforeEffectLines() {
        AbilitySaveInfo info = withEffectLines(withStagePowers(fireBall(), fireBallStagePowers(8)), List.of());

        assertFail(changchunWith(List.of(info)), STAGE_ROWS);
    }

    @Test
    void lengthCheckedBeforePlaceholderAcrossLines() {
        assertFail(fireBallLines(List.of("{적}에게 불꽃이 튄다!", "가".repeat(101))), LENGTH);
    }

    @Test
    void fourthCheckBeforeSixth() {
        AbilitySaveInfo info = withEffectLines(fireBallWith("5", "0", "0"), List.of());

        assertFail(changchunWith(List.of(info)), "재사용 대기시간은 0.1~3,600초로 정하십시오: 화탄술");
    }

    // --- 6차: 게임에서 쓸 수 있는 꼴 ---

    @Test
    void usableInGame() {
        assertThat(only(changchunWith(List.of(fireBall()))).usableInGame()).isTrue();
        assertThat(Technique.create(TechniqueFixture.ironBodyInfo()).getAbilities().getFirst().usableInGame()).isTrue();
        assertThat(Technique.create(changchunWith(List.of(fireBall(), windRiding()))).getAbilities().get(1)
                .usableInGame()).isFalse();

        AbilitySaveInfo allEnemies = new AbilitySaveInfo("화탄술", 1, AbilityKind.ATTACK, List.of(Element.FIRE), false,
                AbilityTarget.ALL_ENEMIES, CostStat.MANA, new BigDecimal("5"), false, BigDecimal.ZERO,
                new BigDecimal("3"), UsableWhen.COMBAT_ONLY, List.of("불꽃이 퍼진다."), fireBallStagePowers(9));
        assertThat(only(changchunWith(List.of(allEnemies))).usableInGame()).isFalse();
        assertThat(only(changchunWith(List.of(withCost(fireBall(), CostStat.SENSE, "5", false)))).usableInGame())
                .isFalse();
        assertThat(only(changchunWith(List.of(withCost(fireBall(), CostStat.MANA, "5", true)))).usableInGame())
                .isFalse();
        assertThat(ironFist().costStat()).isEqualTo(CostStat.NONE);
    }

    private static final String STAGE_ROWS = "단계별 값은 배우는 단계부터 마지막 단계까지 하나씩 정하십시오: 화탄술";
    private static final String SCALE = "기본 위력은 소수 둘째 자리, 계수는 소수 셋째 자리까지 적으십시오: 화탄술 (3성)";
    private static final String LENGTH = "이펙트 문장은 한 줄 1~100자로 적으십시오: 화탄술";

    private static Ability only(TechniqueSaveInfo info) {
        return Technique.create(info).getAbilities().getFirst();
    }

    private static TechniqueSaveInfo fireBallLines(List<String> lines) {
        return changchunWith(List.of(withEffectLines(fireBall(), lines)));
    }

    /** 화탄술의 3성 값만 바꾼 장춘공 */
    private static TechniqueSaveInfo fireBallWithThirdStage(String basePower, String coefficient) {
        List<AbilityStagePower> powers = new ArrayList<>(fireBallStagePowers(9));
        powers.set(2, new AbilityStagePower(3, new BigDecimal(basePower), new BigDecimal(coefficient)));
        return changchunWith(List.of(withStagePowers(fireBall(), powers)));
    }

    private static AbilitySaveInfo fireBallNamed(String name) {
        return ability(name, 1, AbilityKind.ATTACK, List.of(Element.FIRE), AbilityTarget.SINGLE, CostStat.MANA, "5",
                "0", "3", UsableWhen.COMBAT_ONLY);
    }

    private static AbilitySaveInfo fireBallWith(String costAmount, String castSeconds, String cooldownSeconds) {
        return ability("화탄술", 1, AbilityKind.ATTACK, List.of(Element.FIRE), AbilityTarget.SINGLE, CostStat.MANA,
                costAmount, castSeconds, cooldownSeconds, UsableWhen.COMBAT_ONLY);
    }

    private static TechniqueSaveInfo ironBodyWith(AbilitySaveInfo ability) {
        return new TechniqueSaveInfo("강체결", "", List.of(), Realm.MORTAL, List.of(bodyStage(0, 0, 0)),
                List.of(ability));
    }
}
