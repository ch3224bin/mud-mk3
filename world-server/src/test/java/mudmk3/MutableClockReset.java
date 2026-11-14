package mudmk3;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/** 테스트마다 시작 전에 {@link MutableClock} 을 시작 시각으로 되돌린다(컨텍스트와 시계가 테스트끼리 공유되므로) */
public class MutableClockReset implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        SpringExtension.getApplicationContext(context).getBean(MutableClock.class).reset();
    }
}
