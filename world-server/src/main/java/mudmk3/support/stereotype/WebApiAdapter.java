package mudmk3.support.stereotype;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** REST API를 제공하는 웹 어댑터를 표시한다 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Adapter
@RestController
public @interface WebApiAdapter {

    @AliasFor(annotation = RestController.class)
    String value() default "";
}
