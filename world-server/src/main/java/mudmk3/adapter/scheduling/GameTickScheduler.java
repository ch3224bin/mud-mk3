package mudmk3.adapter.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mudmk3.application.game.provided.GameTicker;
import mudmk3.support.stereotype.Adapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 1초마다 서버 틱을 부른다. {@code mud.game.tick-enabled=false}(테스트)면 빈이 없고 테스트가 틱을 직접 부른다.
 * 틱이 예외를 던져도 기록만 하고 다음 틱을 막지 않는다
 */
@Slf4j
@Adapter
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mud.game.tick-enabled", havingValue = "true", matchIfMissing = true)
public class GameTickScheduler {

    private final GameTicker gameTicker;

    @Scheduled(fixedRate = 1000)
    public void tick() {
        try {
            gameTicker.tick();
        } catch (RuntimeException e) {
            log.error("서버 틱 처리에 실패했습니다", e);
        }
    }
}
