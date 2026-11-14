package mudmk3.domain.technique;

import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Realm;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static mudmk3.domain.technique.TechniqueFixture.ability;
import static mudmk3.domain.technique.TechniqueFixture.bodyStage;
import static mudmk3.domain.technique.TechniqueFixture.changchunWith;
import static mudmk3.domain.technique.TechniqueFixture.fireBall;
import static mudmk3.domain.technique.TechniqueTest.assertFail;
import static org.assertj.core.api.Assertions.assertThat;

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
                List.of(TechniqueFixture.stage(0, 15, 0, 0, 0, 0)), List.of(fireBall())));
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
                UsableWhen.COMBAT_ONLY);

        assertFail(ironBodyWith(info), "연체 신통에는 오행 속성을 함께 둘 수 없습니다: 철권");
    }

    @Test
    void manaOnBodyRefiningFail() {
        AbilitySaveInfo info = new AbilitySaveInfo("철권", 1, AbilityKind.ATTACK, List.of(), true,
                AbilityTarget.SINGLE, CostStat.MANA, new BigDecimal("5"), false, BigDecimal.ZERO, new BigDecimal("4"),
                UsableWhen.COMBAT_ONLY);

        assertFail(ironBodyWith(info), "연체술 공법의 신통은 법력을 쓰지 않습니다: 철권");
        // 연체술 공법이라도 정혈 소모는 된다
        AbilitySaveInfo essence = new AbilitySaveInfo("철권", 1, AbilityKind.ATTACK, List.of(), true,
                AbilityTarget.SINGLE, CostStat.ESSENCE, new BigDecimal("1.5"), true, BigDecimal.ZERO,
                new BigDecimal("4"), UsableWhen.COMBAT_ONLY);
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
