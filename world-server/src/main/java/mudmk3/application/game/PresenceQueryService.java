package mudmk3.application.game;

import lombok.RequiredArgsConstructor;
import mudmk3.application.game.provided.PresenceFinder;
import mudmk3.application.game.required.PresenceRepository;
import mudmk3.domain.game.OnlinePlayer;
import mudmk3.support.stereotype.ApplicationService;

import java.util.Set;
import java.util.stream.Collectors;

/** 메모리 presence 를 읽기만 한다. 저장소가 동시 접근에 안전하므로 게임 락을 잡지 않는다 */
@ApplicationService
@RequiredArgsConstructor
public class PresenceQueryService implements PresenceFinder {

    private final PresenceRepository presenceRepository;

    @Override
    public Set<Long> onlineCharacterIds() {
        return presenceRepository.findAll().stream()
                .map(OnlinePlayer::getCharacterId)
                .collect(Collectors.toUnmodifiableSet());
    }
}
