package mudmk3.application.technique.provided;

import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Realm;
import mudmk3.domain.technique.TechniqueFixture;
import mudmk3.domain.technique.CostStat;
import mudmk3.domain.technique.TechniqueSaveInfo;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;

/** 관리 REST 모양의 공법 요청 */
public final class TechniqueRequestFixture {

    private TechniqueRequestFixture() {
    }

    /** 장춘공 요청(값은 {@link TechniqueFixture#changchunInfo()} 와 같음) */
    public static TechniqueSaveRequest changchunRequest(String name) {
        TechniqueSaveInfo info = TechniqueFixture.changchunInfo(name);
        return new TechniqueSaveRequest(name, "목 속성 입문 공법.", List.of(Element.WOOD), Realm.MORTAL,
                stagesOf(info), abilitiesOf(info));
    }

    /** 도메인 입력과 같은 값의 요청({@link TechniqueFixture#ironBodyInfo()}·{@link TechniqueFixture#greenWoodInfo()} 등) */
    public static TechniqueSaveRequest requestOf(TechniqueSaveInfo info) {
        return new TechniqueSaveRequest(info.name(), info.description(), info.elements(), info.startRealm(),
                stagesOf(info), abilitiesOf(info));
    }

    private static List<StageRequest> stagesOf(TechniqueSaveInfo info) {
        return info.stages().stream()
                .map(stage -> new StageRequest(stage.insightRequired(), stage.aptitudeRequired(),
                        stage.manaBonus(), stage.senseBonus(), stage.spiritBonus(), stage.essenceBonus(),
                        stage.bodyBonus(), stage.strengthBonus(), stage.aptitudeGain()))
                .toList();
    }

    private static List<AbilityRequest> abilitiesOf(TechniqueSaveInfo info) {
        return info.abilities().stream()
                .map(ability -> new AbilityRequest(ability.name(), ability.learnStage(), ability.kind(),
                        ability.elements(), ability.bodyRefining(), ability.target(), ability.costStat(),
                        ability.costAmount(), ability.costPerTick(), ability.castSeconds(), ability.cooldownSeconds(),
                        ability.usableWhen(), ability.effectLines(),
                        ability.stagePowers().stream()
                                .map(power -> new StagePowerRequest(power.stage(), power.basePower(),
                                        power.coefficient()))
                                .toList()))
                .toList();
    }

    // ---- AbilityRequest 의 칸 하나만 바꾼 복사본(14칸을 나열하는 곳은 아래 copy 한 곳)

    public static AbilityRequest withEffectLines(AbilityRequest ability, @Nullable List<String> effectLines) {
        return copy(ability, ability.learnStage(), ability.castSeconds(), ability.cooldownSeconds(),
                ability.costStat(), ability.costAmount(), ability.costPerTick(), effectLines, ability.stagePowers());
    }

    public static AbilityRequest withStagePowers(AbilityRequest ability, @Nullable List<StagePowerRequest> stagePowers) {
        return copy(ability, ability.learnStage(), ability.castSeconds(), ability.cooldownSeconds(),
                ability.costStat(), ability.costAmount(), ability.costPerTick(), ability.effectLines(), stagePowers);
    }

    public static AbilityRequest withCast(AbilityRequest ability, @Nullable BigDecimal castSeconds) {
        return copy(ability, ability.learnStage(), castSeconds, ability.cooldownSeconds(),
                ability.costStat(), ability.costAmount(), ability.costPerTick(), ability.effectLines(),
                ability.stagePowers());
    }

    public static AbilityRequest withCost(AbilityRequest ability, CostStat costStat, @Nullable BigDecimal costAmount,
                                          boolean costPerTick) {
        return copy(ability, ability.learnStage(), ability.castSeconds(), ability.cooldownSeconds(),
                costStat, costAmount, costPerTick, ability.effectLines(), ability.stagePowers());
    }

    public static AbilityRequest withCooldown(AbilityRequest ability, @Nullable BigDecimal cooldownSeconds) {
        return copy(ability, ability.learnStage(), ability.castSeconds(), cooldownSeconds,
                ability.costStat(), ability.costAmount(), ability.costPerTick(), ability.effectLines(),
                ability.stagePowers());
    }

    public static AbilityRequest withLearnStage(AbilityRequest ability, @Nullable Integer learnStage) {
        return copy(ability, learnStage, ability.castSeconds(), ability.cooldownSeconds(),
                ability.costStat(), ability.costAmount(), ability.costPerTick(), ability.effectLines(),
                ability.stagePowers());
    }

    private static AbilityRequest copy(AbilityRequest ability, @Nullable Integer learnStage,
                                       @Nullable BigDecimal castSeconds, @Nullable BigDecimal cooldownSeconds,
                                       CostStat costStat, @Nullable BigDecimal costAmount, boolean costPerTick,
                                       @Nullable List<String> effectLines,
                                       @Nullable List<StagePowerRequest> stagePowers) {
        return new AbilityRequest(ability.name(), learnStage, ability.kind(), ability.elements(),
                ability.bodyRefining(), ability.target(), costStat, costAmount, costPerTick, castSeconds,
                cooldownSeconds, ability.usableWhen(), effectLines, stagePowers);
    }
}
