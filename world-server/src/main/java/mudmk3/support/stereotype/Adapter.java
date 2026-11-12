package mudmk3.support.stereotype;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 외부 세계(웹·WebSocket·DB·외부 시스템)와 연결하는 어댑터 빈을 표시한다 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Adapter {

    @AliasFor(annotation = Component.class)
    String value() default "";
}
