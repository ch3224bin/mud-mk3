package mudmk3.application.game;

import lombok.RequiredArgsConstructor;
import mudmk3.application.game.required.GameMessage;
import mudmk3.application.game.required.GameMessage.SystemMessage;
import mudmk3.domain.game.Command;
import mudmk3.domain.game.OnlinePlayer;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.function.BiConsumer;

/**
 * 전투 명령({@code 공격}·{@code 수습}, D86)을 처리한다. {@link GameService} 가 게임 락과 트랜잭션 안에서 부른다.
 * 교전 상태는 {@link CombatEngine} 이 맡는다
 */
@Component
@RequiredArgsConstructor
class CombatCommandHandler {

    private final CombatEngine combatEngine;
    private final Clock clock;

    /** 전투 명령을 처리한다. send 는 (연결 id, 메시지)로 보낼 것을 모은다 */
    void handle(Command command, OnlinePlayer player, BiConsumer<String, GameMessage> send) {
        switch (command) {
            case Command.Attack attack -> combatEngine.attack(player, attack.target(), clock.instant(), send);
            // 수습은 아직 없다: sealed switch 를 채우는 임시 자리(D86). 시체·수습이 생기면 바꾼다
            case Command.Collect _ -> send.accept(player.getSessionId(), SystemMessage.error(GameService.UNKNOWN));
            default -> throw new IllegalArgumentException("전투 명령이 아닙니다: " + command);
        }
    }
}
