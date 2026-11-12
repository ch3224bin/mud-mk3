package mudmk3.support.stereotype;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** provided 포트를 구현하는 트랜잭션 애플리케이션 서비스를 표시한다 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Service
@Transactional
public @interface ApplicationService {

    @AliasFor(annotation = Service.class)
    String value() default "";
}
