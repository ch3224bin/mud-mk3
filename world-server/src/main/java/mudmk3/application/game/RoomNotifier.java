package mudmk3.application.game;

import lombok.RequiredArgsConstructor;
import mudmk3.application.game.required.GameMessage;
import mudmk3.application.game.required.PresenceRepository;
import mudmk3.domain.game.OnlinePlayer;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;

/**
 * 같은 방의 다른 사람에게 알린다. 알림(tone·event)은 부르는 쪽이 만든다. {@link GameService} 의 게임 락과 트랜잭션 안에서만 불린다
 */
@Component
@RequiredArgsConstructor
class RoomNotifier {

    private final PresenceRepository presenceRepository;

    /** 행동한 사람이 있는 방({@code actor.getRoomId()})의 다른 사람에게 */
    void notifyOthers(OnlinePlayer actor, GameMessage notice, BiConsumer<String, GameMessage> send) {
        notifyOthers(actor, actor.getRoomId(), notice, send);
    }

    /** 그 방({@code roomId})의 다른 사람에게. 이동처럼 행동한 사람의 지금 방과 알릴 방이 다를 때 쓴다 */
    void notifyOthers(OnlinePlayer actor, Long roomId, GameMessage notice, BiConsumer<String, GameMessage> send) {
        presenceRepository.findAllInRoom(roomId).stream()
                .filter(other -> !other.getCharacterId().equals(actor.getCharacterId()))
                .forEach(other -> send.accept(other.getSessionId(), notice));
    }
}
