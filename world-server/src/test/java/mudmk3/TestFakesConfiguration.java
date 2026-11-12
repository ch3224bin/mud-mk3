package mudmk3;

import mudmk3.application.game.RecordingGameMessageSender;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** 게임 포트 테스트에서 실제 WebSocket sender 대신 보낸 것을 기록하는 가짜 sender를 쓴다(@Primary) */
@TestConfiguration
public class TestFakesConfiguration {

    @Bean
    @Primary
    RecordingGameMessageSender recordingGameMessageSender() {
        return new RecordingGameMessageSender();
    }
}
