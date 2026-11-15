package mudmk3.domain.technique;

import java.math.BigDecimal;

import static java.util.Objects.requireNonNull;

/**
 * 공격 신통의 단계 하나의 값: 단계(공법의 n성), 기본 위력, 신통 계수.
 * 범위·소수 자리는 단계 번호와 신통 이름을 문구에 넣어야 하므로 {@link Ability} 가 검사한다
 */
public record AbilityStagePower(
        int stage,
        BigDecimal basePower,
        BigDecimal coefficient
) {
    public AbilityStagePower {
        requireNonNull(basePower);
        requireNonNull(coefficient);
    }
}
