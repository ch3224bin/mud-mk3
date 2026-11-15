package mudmk3.application.technique.provided;

import jakarta.persistence.EntityManager;
import mudmk3.ApplicationServiceTest;
import mudmk3.application.technique.TechniqueModifyService;
import mudmk3.application.technique.required.TechniqueRepository;
import mudmk3.domain.shared.Element;
import mudmk3.domain.technique.AbilityKind;
import mudmk3.domain.technique.AbilityTarget;
import mudmk3.domain.technique.CostStat;
import mudmk3.domain.technique.TechniqueFixture;
import mudmk3.domain.technique.UsableWhen;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static mudmk3.application.technique.provided.TechniqueRequestFixture.changchunRequest;
import static mudmk3.application.technique.provided.TechniqueRequestFixture.requestOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/** 서버가 뜰 때 6차 전에 만든 신통의 이펙트 문장·단계별 값을 채우는 것(admin-tool.md 기존 신통 채우기) */
@ApplicationServiceTest
record TechniqueFillTest(TechniqueModifyService techniqueModifyService, TechniqueEditor techniqueEditor,
                         TechniqueFinder techniqueFinder, TechniqueRepository techniqueRepository,
                         EntityManager entityManager) {

    @Test
    void fillsSingleTargetLine() {
        Long id = techniqueEditor.create(changchunRequest("장춘공")).id();
        toOldState();

        techniqueModifyService.fillMissingAbilityData();

        assertThat(ability(id, "화탄술").effectLines()).containsExactly("{대상}에게 화탄술을 펼친다!");
    }

    @Test
    void fillsSelfLine() {
        Long id = techniqueEditor.create(changchunRequest("장춘공")).id();
        toOldState();

        techniqueModifyService.fillMissingAbilityData();

        assertThat(ability(id, "어풍결").effectLines()).containsExactly("어풍결을 펼친다.");
        assertThat(ability(id, "천안술").effectLines()).containsExactly("천안술을 펼친다.");
    }

    @Test
    void fillsElementalStagePowers() {
        Long id = techniqueEditor.create(changchunRequest("장춘공")).id();
        toOldState();

        techniqueModifyService.fillMissingAbilityData();

        assertThat(ability(id, "화탄술").stagePowers()).containsExactly(
                power(1, "2.00", "1.200"), power(2, "2.20", "1.200"), power(3, "2.40", "1.200"),
                power(4, "2.60", "1.200"), power(5, "2.80", "1.200"), power(6, "3.00", "1.200"),
                power(7, "3.20", "1.200"), power(8, "3.40", "1.200"), power(9, "3.60", "1.200"));
    }

    @Test
    void fillsFromLearnStage() {
        Long id = techniqueEditor.create(requestOf(TechniqueFixture.changchunWith(List.of(
                TechniqueFixture.fireBall(),
                TechniqueFixture.ability("낙엽인", 3, AbilityKind.ATTACK, List.of(Element.WOOD),
                        AbilityTarget.SINGLE, CostStat.MANA, "8", "0", "5", UsableWhen.COMBAT_ONLY))))).id();
        toOldState();

        techniqueModifyService.fillMissingAbilityData();

        assertThat(ability(id, "낙엽인").stagePowers()).containsExactly(
                power(3, "2.00", "1.200"), power(4, "2.20", "1.200"), power(5, "2.40", "1.200"),
                power(6, "2.60", "1.200"), power(7, "2.80", "1.200"), power(8, "3.00", "1.200"),
                power(9, "3.20", "1.200"));
    }

    @Test
    void fillsBodyRefiningStagePowers() {
        Long id = techniqueEditor.create(requestOf(TechniqueFixture.ironBodyInfo())).id();
        toOldState();

        techniqueModifyService.fillMissingAbilityData();

        TechniqueView.AbilityView ironFist = ability(id, "철권");
        assertThat(ironFist.effectLines()).containsExactly("{대상}에게 철권을 펼친다!");
        assertThat(ironFist.stagePowers()).containsExactly(
                power(1, "5.00", "2.200"), power(2, "5.50", "2.200"), power(3, "6.00", "2.200"),
                power(4, "6.50", "2.200"), power(5, "7.00", "2.200"), power(6, "7.50", "2.200"),
                power(7, "8.00", "2.200"), power(8, "8.50", "2.200"), power(9, "9.00", "2.200"));
    }

    @Test
    void noStagePowersForNonAttack() {
        Long id = techniqueEditor.create(changchunRequest("장춘공")).id();
        toOldState();

        techniqueModifyService.fillMissingAbilityData();

        assertThat(ability(id, "천안술").stagePowers()).isEmpty();
        assertThat(ability(id, "어풍결").stagePowers()).isEmpty();
    }

    @Test
    void fillKeepsExisting() {
        Long id = techniqueEditor.create(requestOf(TechniqueFixture.greenWoodInfo())).id();
        List<TechniqueView.StagePowerView> before = ability(id, "청목전").stagePowers();
        entityManager.createNativeQuery("delete from ability_effect_line").executeUpdate();
        entityManager.clear();

        techniqueModifyService.fillMissingAbilityData();

        TechniqueView.AbilityView greenBlade = ability(id, "청목전");
        assertThat(greenBlade.effectLines()).containsExactly("{대상}에게 청목전을 펼친다!");
        assertThat(greenBlade.stagePowers()).isEqualTo(before).first().isEqualTo(power(1, "3.00", "1.200"));
    }

    @Test
    void fillIsIdempotent() {
        Long id = techniqueEditor.create(changchunRequest("장춘공")).id();
        toOldState();
        techniqueModifyService.fillMissingAbilityData();
        entityManager.flush();
        entityManager.clear();
        TechniqueView first = techniqueFinder.view(id);
        long lineRows = count("ability_effect_line");
        long powerRows = count("ability_stage_power");

        techniqueModifyService.fillMissingAbilityData();
        entityManager.flush();
        entityManager.clear();

        assertThat(techniqueRepository.findById(id).orElseThrow().fillMissingAbilityData()).isZero();
        assertThat(techniqueFinder.view(id)).isEqualTo(first);
        assertThat(count("ability_effect_line")).isEqualTo(lineRows).isEqualTo(3);
        assertThat(count("ability_stage_power")).isEqualTo(powerRows).isEqualTo(9);
    }

    @Test
    void filledAbilityPassesValidation() {
        Long changchun = techniqueEditor.create(changchunRequest("장춘공")).id();
        Long ironBody = techniqueEditor.create(requestOf(TechniqueFixture.ironBodyInfo())).id();
        toOldState();
        techniqueModifyService.fillMissingAbilityData();
        entityManager.flush();
        entityManager.clear();

        assertThatCode(() -> techniqueEditor.update(changchun, requestFromView(techniqueFinder.view(changchun))))
                .doesNotThrowAnyException();
        assertThatCode(() -> techniqueEditor.update(ironBody, requestFromView(techniqueFinder.view(ironBody))))
                .doesNotThrowAnyException();
    }

    @Test
    void fillOnStartupListener() throws NoSuchMethodException {
        Method fill = TechniqueModifyService.class.getMethod("fillMissingAbilityData");

        assertThat(requireNonNull(fill.getAnnotation(EventListener.class)).value())
                .containsExactly(ApplicationReadyEvent.class);
        assertThat(requireNonNull(fill.getAnnotation(Order.class)).value()).isEqualTo(20);
    }

    /** 6차 전 pgdata 꼴: 이펙트 문장·단계별 값 행이 하나도 없다 */
    private void toOldState() {
        entityManager.flush();
        entityManager.createNativeQuery("delete from ability_effect_line").executeUpdate();
        entityManager.createNativeQuery("delete from ability_stage_power").executeUpdate();
        entityManager.clear();
    }

    private TechniqueView.AbilityView ability(Long techniqueId, String name) {
        entityManager.flush();
        entityManager.clear();
        return techniqueFinder.view(techniqueId).abilities().stream()
                .filter(ability -> ability.name().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private long count(String table) {
        return ((Number) entityManager.createNativeQuery("select count(*) from " + table).getSingleResult()).longValue();
    }

    private static TechniqueView.StagePowerView power(int stage, String basePower, String coefficient) {
        return new TechniqueView.StagePowerView(stage, new BigDecimal(basePower), new BigDecimal(coefficient));
    }

    /** 보기 값을 그대로 저장 요청으로 */
    private static TechniqueSaveRequest requestFromView(TechniqueView view) {
        List<StageRequest> stages = view.stages().stream()
                .map(stage -> new StageRequest(stage.insightRequired(), stage.aptitudeRequired(), stage.manaBonus(),
                        stage.senseBonus(), stage.spiritBonus(), stage.essenceBonus(), stage.bodyBonus(),
                        stage.strengthBonus(), stage.aptitudeGain()))
                .toList();
        List<AbilityRequest> abilities = new ArrayList<>();
        for (TechniqueView.AbilityView ability : view.abilities()) {
            abilities.add(new AbilityRequest(ability.name(), ability.learnStage(), ability.kind(), ability.elements(),
                    ability.bodyRefining(), ability.target(), ability.costStat(), ability.costAmount(),
                    ability.costPerTick(), ability.castSeconds(), ability.cooldownSeconds(), ability.usableWhen(),
                    ability.effectLines(),
                    ability.stagePowers().stream()
                            .map(power -> new StagePowerRequest(power.stage(), power.basePower(), power.coefficient()))
                            .toList()));
        }
        return new TechniqueSaveRequest(view.name(), view.description(), view.elements(), view.startRealm(),
                stages, abilities);
    }
}
