package mudmk3.application.technique.provided;

import mudmk3.application.technique.provided.ScrollItem;
import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Realm;
import mudmk3.domain.technique.Ability;
import mudmk3.domain.technique.AbilityKind;
import mudmk3.domain.technique.AbilityStagePower;
import mudmk3.domain.technique.AbilityTarget;
import mudmk3.domain.technique.CostStat;
import mudmk3.domain.technique.Technique;
import mudmk3.domain.technique.TechniqueStage;
import mudmk3.domain.technique.UsableWhen;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * 공법 보기 값: 서비스가 트랜잭션 안에서 만들어 트랜잭션 밖(REST 응답)에서도 읽을 수 있다.
 * {@code elementNames} 는 {@code 목}·{@code 화·토}(연체술이면 null), {@code lineName} 은 {@code 오행}·{@code 연체술}
 */
public record TechniqueView(
        Long id,
        String name,
        String description,
        List<Element> elements,
        @Nullable String elementNames,
        String lineName,
        Realm startRealm,
        String startRealmName,
        List<TechniqueStage> stages,
        List<AbilityView> abilities,
        long learnerCount,
        List<ScrollItem> scrollItems
) {
    public TechniqueView {
        elements = List.copyOf(elements);
        stages = List.copyOf(stages);
        abilities = List.copyOf(abilities);
        scrollItems = List.copyOf(scrollItems);
    }

    public static TechniqueView of(Technique technique, long learnerCount, List<ScrollItem> scrollItems) {
        return new TechniqueView(
                requireNonNull(technique.getId()),
                technique.getName(),
                technique.getDescription(),
                technique.getElements(),
                technique.elementNames().orElse(null),
                technique.lineName(),
                technique.getStartRealm(),
                technique.getStartRealm().koreanName(),
                technique.getStages(),
                technique.getAbilities().stream().map(AbilityView::of).toList(),
                learnerCount,
                scrollItems);
    }

    public int stageCount() {
        return stages.size();
    }

    public int abilityCount() {
        return abilities.size();
    }

    /**
     * 신통 보기 값(정의 순서). 종류·대상의 화면 이름을 더한다. 6차: 이펙트 문장·단계별 값(저장한 모양 그대로)과
     * 게임에서 쓸 수 있는 꼴인지
     */
    public record AbilityView(
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
            UsableWhen usableWhen,
            List<String> effectLines,
            List<StagePowerView> stagePowers,
            boolean usableInGame
    ) {
        public AbilityView {
            elements = List.copyOf(elements);
            effectLines = List.copyOf(effectLines);
            stagePowers = List.copyOf(stagePowers);
        }

        public static AbilityView of(Ability ability) {
            return new AbilityView(ability.getName(), ability.getLearnStage(), ability.getKind(),
                    ability.getKind().koreanName(), ability.getElements(), ability.isBodyRefining(),
                    ability.getTarget(), ability.getTarget().koreanName(), ability.getCostStat(),
                    ability.getCostAmount(), ability.isCostPerTick(), ability.getCastSeconds(),
                    ability.getCooldownSeconds(), ability.getUsableWhen(), ability.getEffectLines(),
                    ability.getStagePowers().stream().map(StagePowerView::of).toList(), ability.usableInGame());
        }
    }

    /** 공격 신통의 단계 하나의 값(기본 위력 scale 2, 계수 scale 3) */
    public record StagePowerView(int stage, BigDecimal basePower, BigDecimal coefficient) {
        static StagePowerView of(AbilityStagePower power) {
            return new StagePowerView(power.stage(), power.basePower(), power.coefficient());
        }
    }
}
