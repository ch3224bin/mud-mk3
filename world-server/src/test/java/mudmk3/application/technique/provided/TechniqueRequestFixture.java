package mudmk3.application.technique.provided;

import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Realm;
import mudmk3.domain.technique.TechniqueFixture;

import java.util.List;

/** 관리 REST 모양의 공법 요청 */
public final class TechniqueRequestFixture {

    private TechniqueRequestFixture() {
    }

    /** 장춘공 요청(값은 {@link TechniqueFixture#changchunInfo()} 와 같음) */
    public static TechniqueSaveRequest changchunRequest(String name) {
        return new TechniqueSaveRequest(name, "목 속성 입문 공법.", List.of(Element.WOOD), Realm.MORTAL,
                TechniqueFixture.changchunStages().stream()
                        .map(stage -> new StageRequest(stage.insightRequired(), stage.aptitudeRequired(),
                                stage.manaBonus(), stage.senseBonus(), stage.spiritBonus(), stage.essenceBonus(),
                                stage.bodyBonus(), stage.strengthBonus(), stage.aptitudeGain()))
                        .toList(),
                TechniqueFixture.changchunAbilities().stream()
                        .map(info -> new AbilityRequest(info.name(), info.learnStage(), info.kind(), info.elements(),
                                info.bodyRefining(), info.target(), info.costStat(), info.costAmount(),
                                info.costPerTick(), info.castSeconds(), info.cooldownSeconds(), info.usableWhen()))
                        .toList());
    }
}
