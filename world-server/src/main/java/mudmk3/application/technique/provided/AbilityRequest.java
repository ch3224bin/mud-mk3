package mudmk3.application.technique.provided;

import mudmk3.domain.shared.Element;
import mudmk3.domain.technique.AbilityKind;
import mudmk3.domain.technique.AbilitySaveInfo;
import mudmk3.domain.technique.AbilityTarget;
import mudmk3.domain.technique.CostStat;
import mudmk3.domain.technique.UsableWhen;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * 신통 한 줄 요청. 빠진 숫자는 0, 빠진 속성은 빈 목록, 빠진 이름은 빈 글(도메인이 이름 규칙으로 막는다)이다.
 * 종류·대상·소모 스탯·쓸 수 있는 때가 없으면 만들 때 {@code IllegalArgumentException} 을 던져,
 * JSON 을 읽는 중이면 공통 400 {@code 요청 본문을 읽을 수 없습니다} 가 되게 한다
 */
public record AbilityRequest(
        String name,
        @Nullable Integer learnStage,
        AbilityKind kind,
        @Nullable List<Element> elements,
        boolean bodyRefining,
        AbilityTarget target,
        CostStat costStat,
        @Nullable BigDecimal costAmount,
        boolean costPerTick,
        @Nullable BigDecimal castSeconds,
        @Nullable BigDecimal cooldownSeconds,
        UsableWhen usableWhen
) {
    public AbilityRequest {
        name = name == null ? "" : name;
        learnStage = Objects.requireNonNullElse(learnStage, 0);
        RequestValues.required(kind, "kind");
        elements = RequestValues.elements(elements);
        RequestValues.required(target, "target");
        RequestValues.required(costStat, "costStat");
        costAmount = RequestValues.orZero(costAmount);
        castSeconds = RequestValues.orZero(castSeconds);
        cooldownSeconds = RequestValues.orZero(cooldownSeconds);
        RequestValues.required(usableWhen, "usableWhen");
    }

    public AbilitySaveInfo toInfo() {
        return new AbilitySaveInfo(name, Objects.requireNonNullElse(learnStage, 0), kind, RequestValues.elements(elements),
                bodyRefining, target, costStat, RequestValues.orZero(costAmount), costPerTick,
                RequestValues.orZero(castSeconds), RequestValues.orZero(cooldownSeconds), usableWhen);
    }
}
