package mudmk3.domain.technique;

import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Names;

import java.math.BigDecimal;
import java.util.List;

import static java.util.Objects.requireNonNull;

/** 신통을 정의할 때 받는 값. 이름은 앞뒤 공백을 자른다. 규칙 검사는 {@link Technique} 와 {@link Ability} 가 한다 */
public record AbilitySaveInfo(
        String name,
        int learnStage,
        AbilityKind kind,
        List<Element> elements,
        boolean bodyRefining,
        AbilityTarget target,
        CostStat costStat,
        BigDecimal costAmount,
        boolean costPerTick,
        BigDecimal castSeconds,
        BigDecimal cooldownSeconds,
        UsableWhen usableWhen
) {
    public AbilitySaveInfo {
        name = requireNonNull(Names.strip(name));
        requireNonNull(kind);
        elements = List.copyOf(elements);
        requireNonNull(target);
        requireNonNull(costStat);
        requireNonNull(costAmount);
        requireNonNull(castSeconds);
        requireNonNull(cooldownSeconds);
        requireNonNull(usableWhen);
    }
}
