package mudmk3.adapter.webapi.dto;

import mudmk3.application.technique.provided.TechniqueView;
import mudmk3.application.technique.provided.TechniqueView.AbilityView;
import mudmk3.application.technique.required.ScrollItem;
import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Realm;
import mudmk3.domain.technique.AbilityKind;
import mudmk3.domain.technique.AbilityTarget;
import mudmk3.domain.technique.CostStat;
import mudmk3.domain.technique.TechniqueStage;
import mudmk3.domain.technique.UsableWhen;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;

/**
 * 공법 상세: 저장 칸 전부와 {@code id}·{@code lineName}·{@code startRealmName}·{@code learnerCount}·{@code scrollItems}.
 * {@code stages[0]} 이 1성이다. 신통에는 {@code kindName}·{@code targetName} 을 더한다
 */
public record TechniqueDetailResponse(
        Long id,
        String name,
        String description,
        List<Element> elements,
        @Nullable String elementNames,
        String lineName,
        Realm startRealm,
        String startRealmName,
        List<StageResponse> stages,
        List<AbilityResponse> abilities,
        long learnerCount,
        List<ScrollItemResponse> scrollItems
) {

    public static TechniqueDetailResponse of(TechniqueView view) {
        return new TechniqueDetailResponse(view.id(), view.name(), view.description(), view.elements(),
                view.elementNames(), view.lineName(), view.startRealm(), view.startRealmName(),
                view.stages().stream().map(StageResponse::of).toList(),
                view.abilities().stream().map(AbilityResponse::of).toList(),
                view.learnerCount(),
                view.scrollItems().stream().map(ScrollItemResponse::of).toList());
    }

    public record StageResponse(
            int insightRequired,
            int aptitudeRequired,
            int manaBonus,
            int senseBonus,
            int spiritBonus,
            int essenceBonus,
            int bodyBonus,
            int strengthBonus,
            int aptitudeGain
    ) {
        static StageResponse of(TechniqueStage stage) {
            return new StageResponse(stage.insightRequired(), stage.aptitudeRequired(), stage.manaBonus(),
                    stage.senseBonus(), stage.spiritBonus(), stage.essenceBonus(), stage.bodyBonus(),
                    stage.strengthBonus(), stage.aptitudeGain());
        }
    }

    public record AbilityResponse(
            String name,
            int learnStage,
            AbilityKind kind,
            String kindName,
            List<Element> elements,
            boolean bodyRefining,
            AbilityTarget target,
            String targetName,
            CostStat costStat,
            BigDecimal costAmount,
            boolean costPerTick,
            BigDecimal castSeconds,
            BigDecimal cooldownSeconds,
            UsableWhen usableWhen
    ) {
        static AbilityResponse of(AbilityView ability) {
            return new AbilityResponse(ability.name(), ability.learnStage(), ability.kind(), ability.kindName(),
                    ability.elements(), ability.bodyRefining(), ability.target(), ability.targetName(),
                    ability.costStat(), ability.costAmount(), ability.costPerTick(), ability.castSeconds(),
                    ability.cooldownSeconds(), ability.usableWhen());
        }
    }

    public record ScrollItemResponse(Long id, String name) {
        static ScrollItemResponse of(ScrollItem item) {
            return new ScrollItemResponse(item.id(), item.name());
        }
    }
}
