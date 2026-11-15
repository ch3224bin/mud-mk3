package mudmk3.adapter.scheduling;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class GameTickSchedulerTest {

    @Test
    void tick() {
        AtomicInteger calls = new AtomicInteger();
        GameTickScheduler scheduler = new GameTickScheduler(calls::incrementAndGet);

        scheduler.tick();

        assertThat(calls).hasValue(1);
    }

    @Test
    void tickFailureDoesNotEscape() {
        AtomicInteger calls = new AtomicInteger();
        GameTickScheduler scheduler = new GameTickScheduler(() -> {
            calls.incrementAndGet();
            throw new IllegalStateException("틱 실패");
        });

        assertThatCode(scheduler::tick).doesNotThrowAnyException();
        assertThatCode(scheduler::tick).doesNotThrowAnyException();
        assertThat(calls).hasValue(2);
    }
}
