package mudmk3.application.technique.provided;

import mudmk3.domain.technique.AbilityStagePower;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

import static java.util.Objects.requireNonNull;

/**
 * 공격 신통의 단계 하나의 값 요청. 셋 중 하나라도 없으면 만들 때 {@code IllegalArgumentException} 을 던져,
 * JSON 을 읽는 중이면 공통 400 {@code 요청 본문을 읽을 수 없습니다} 가 되게 한다. 범위·소수 자리 검사는 도메인이 한다
 */
public record StagePowerRequest(
        @Nullable Integer stage,
        @Nullable BigDecimal basePower,
        @Nullable BigDecimal coefficient
) {
    public StagePowerRequest {
        RequestValues.required(stage, "stage");
        RequestValues.required(basePower, "basePower");
        RequestValues.required(coefficient, "coefficient");
    }

    /** 컴팩트 생성자가 이미 확인했지만 컴포넌트가 @Nullable 타입이라 다시 확인한다 */
    public AbilityStagePower toInfo() {
        return new AbilityStagePower(requireNonNull(stage), requireNonNull(basePower), requireNonNull(coefficient));
    }
}
