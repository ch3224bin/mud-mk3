package mudmk3.support.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** 시간이 필요한 서비스가 주입받는 시계. 테스트는 {@code MutableClock} 으로 바꾼다 */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
