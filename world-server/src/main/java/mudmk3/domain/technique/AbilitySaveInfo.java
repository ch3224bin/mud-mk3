package mudmk3.domain.technique;

import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Names;

import java.math.BigDecimal;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * 신통을 정의할 때 받는 값. 이름과 이펙트 문장(줄마다)은 앞뒤 공백을 자른다. 규칙 검사는 {@link Technique} 와
 * {@link Ability} 가 한다. 단계별 값은 공격 신통만 가진다(아니면 빈 목록)
 */
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
        UsableWhen usableWhen,
        List<String> effectLines,
        List<AbilityStagePower> stagePowers
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
        if (effectLines.stream().anyMatch(line -> line == null)) {
            throw new IllegalArgumentException("effectLines 에 빈 값이 있습니다");
        }
        effectLines = effectLines.stream().map(String::strip).toList();
        stagePowers = List.copyOf(stagePowers);
    }
}
