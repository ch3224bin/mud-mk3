package mudmk3.application.technique.provided;

import mudmk3.domain.shared.Element;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;

/** 공법 요청 record 의 빠진 값 채우기와 필수 값 확인 */
final class RequestValues {

    private RequestValues() {
    }

    /** 필수 값이 없으면 {@code IllegalArgumentException("<필드> 는 꼭 보내야 합니다")} */
    static void required(@Nullable Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " 는 꼭 보내야 합니다");
        }
    }

    /** 빠진 속성 목록은 빈 목록. 목록 안의 빈 값은 받지 않는다 */
    static List<Element> elements(@Nullable List<Element> elements) {
        if (elements == null) return List.of();
        if (elements.stream().anyMatch(element -> element == null)) {
            throw new IllegalArgumentException("elements 에 빈 값이 있습니다");
        }
        return List.copyOf(elements);
    }

    static BigDecimal orZero(@Nullable BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
