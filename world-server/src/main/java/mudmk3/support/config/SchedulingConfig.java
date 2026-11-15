package mudmk3.support.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 서버 틱 스케줄링을 켠다. {@code mud.game.tick-enabled=false}(테스트)면 켜지 않는다 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "mud.game.tick-enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
