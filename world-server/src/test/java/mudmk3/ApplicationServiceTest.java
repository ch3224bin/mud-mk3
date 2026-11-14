package mudmk3;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** provided 포트 테스트: 실제 빈과 H2로 돌리고 테스트마다 롤백한다. 테스트 시계는 테스트마다 시작 시각에서 시작한다 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@Transactional
@Import(TestFakesConfiguration.class)
@ExtendWith(MutableClockReset.class)
public @interface ApplicationServiceTest {
}
