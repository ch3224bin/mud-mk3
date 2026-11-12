package mudmk3;

import mudmk3.application.game.RecordingGameMessageSender;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** 테스트에서 외부 연동 포트를 대신하는 가짜 빈을 모은다 */
@TestConfiguration
public class TestFakesConfiguration {

    @Bean
    @Primary
    RecordingGameMessageSender recordingGameMessageSender() {
        return new RecordingGameMessageSender();
    }
}
