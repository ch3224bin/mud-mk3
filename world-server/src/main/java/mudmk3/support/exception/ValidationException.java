package mudmk3.support.exception;

import lombok.Getter;

import java.util.List;

/** 여러 검증 오류를 모아 알린다 */
@Getter
public class ValidationException extends RuntimeException {

    private final List<String> errors;

    public ValidationException(List<String> errors) {
        super(String.join(", ", errors));
        this.errors = List.copyOf(errors);
    }
}
