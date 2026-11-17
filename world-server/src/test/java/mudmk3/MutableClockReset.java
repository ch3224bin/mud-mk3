package mudmk3;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/** 테스트마다 시작 전과 끝난 뒤에 {@link MutableClock} 을 시작 시각으로 되돌린다(컨텍스트와 시계가 테스트끼리 공유되므로) */
public class MutableClockReset implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        reset(context);
    }

    /** 끝난 뒤에도 되돌려, 시계를 읽는 다른 테스트 클래스(컨텍스트 공유)에 이 테스트의 시각이 남지 않게 한다 */
    @Override
    public void afterEach(ExtensionContext context) {
        reset(context);
    }

    private static void reset(ExtensionContext context) {
        SpringExtension.getApplicationContext(context).getBean(MutableClock.class).reset();
    }
}
