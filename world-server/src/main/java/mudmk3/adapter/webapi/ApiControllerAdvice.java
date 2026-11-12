package mudmk3.adapter.webapi;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import mudmk3.application.room.provided.ExitConflictException;
import mudmk3.application.room.provided.StartRoomDeletionException;
import mudmk3.application.zone.provided.ZoneNotEmptyException;
import mudmk3.domain.zone.DuplicateZoneNameException;
import mudmk3.support.exception.ValidationException;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/** REST 오류를 ProblemDetail(한국어 detail, timestamp, exception)로 통일한다 */
@Slf4j
@ControllerAdvice
public class ApiControllerAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler({
            DuplicateZoneNameException.class,
            ZoneNotEmptyException.class,
            ExitConflictException.class,
            StartRoomDeletionException.class
    })
    public ProblemDetail handleConflict(RuntimeException exception) {
        return problem(HttpStatus.CONFLICT, exception.getMessage(), exception);
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            ValidationException.class,
            ConstraintViolationException.class
    })
    public ProblemDetail handleBadRequest(RuntimeException exception) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception) {
        log.error("처리하지 못한 오류", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다", exception);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String fields = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getField)
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));
        ex.getBody().setDetail("입력 값이 올바르지 않습니다: " + fields);
        return handleExceptionInternal(ex, null, headers, status, request);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status,
                "값이 올바르지 않습니다: " + ex.getPropertyName() + "=" + ex.getValue());
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, "요청 본문을 읽을 수 없습니다");
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    /** 부모 클래스가 처리하는 Spring MVC 오류 응답에도 timestamp·exception 속성을 넣는다 */
    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail problemDetail) {
            addProperties(problemDetail, ex);
        }
        return response;
    }

    private static ProblemDetail problem(HttpStatus status, @Nullable String detail, Throwable exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        addProperties(problemDetail, exception);
        return problemDetail;
    }

    private static void addProperties(ProblemDetail problemDetail, Throwable exception) {
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("exception", exception.getClass().getSimpleName());
    }
}
