package mudmk3.application.technique.provided;

import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Realm;
import mudmk3.domain.technique.TechniqueFixture;
import mudmk3.domain.technique.TechniqueSaveInfo;

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
}
