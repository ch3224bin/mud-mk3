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
    private final CorpseHandler corpseHandler;
    private final Clock clock;

    /** 전투 명령을 처리한다. send 는 (연결 id, 메시지)로 보낼 것을 모은다 */
    void handle(Command command, OnlinePlayer player, BiConsumer<String, GameMessage> send) {
        switch (command) {
            case Command.Attack attack -> combatEngine.attack(player, attack.target(), clock.instant(), send);
            case Command.Collect collect -> collect(player, collect, send);
            default -> throw new IllegalArgumentException("전투 명령이 아닙니다: " + command);
        }
    }

    /** {@code 수습 <시체>}: 교전 중이면 {@code 싸우는 중에는 할 수 없습니다.} */
    private void collect(OnlinePlayer player, Command.Collect collect, BiConsumer<String, GameMessage> send) {
        if (combatEngine.isFighting(player.getCharacterId())) {
            send.accept(player.getSessionId(), SystemMessage.error(CombatTexts.CANNOT_DO));
            return;
        }
        corpseHandler.collect(player, collect.corpse(), clock.instant(), send);
    }
}
