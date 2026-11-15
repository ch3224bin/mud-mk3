package mudmk3.support.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.random.RandomGenerator;

/** 드롭 굴림이 쓰는 난수. 게임 락 안에서만 쓴다. 테스트는 {@code ScriptedRandom} 으로 바꾼다 */
@Configuration
public class RandomConfig {

    @Bean
    RandomGenerator randomGenerator() {
        return RandomGenerator.getDefault();
    }
}
