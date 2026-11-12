package mudmk3.support.stereotype;

import org.springframework.core.annotation.AliasFor;
import org.springframework.validation.annotation.Validated;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 포트 메서드의 {@code @Valid} 입력을 검증하는 애플리케이션 서비스를 표시한다 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApplicationService
@Validated
public @interface ValidatedApplicationService {

    @AliasFor(annotation = ApplicationService.class)
    String value() default "";
}
