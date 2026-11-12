package mudmk3.adapter.persistence.memory;

import mudmk3.application.game.required.PresenceRepository;
import mudmk3.domain.game.OnlinePlayer;
import mudmk3.support.stereotype.Adapter;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** 접속 중인 플레이어를 캐릭터 id로 메모리에 둔다. 게임 처리가 직렬화되므로 조회는 단순 순회로 한다 */
@Adapter
public class InMemoryPresenceRepository implements PresenceRepository {

    private final Map<Long, OnlinePlayer> players = new ConcurrentHashMap<>();

    @Override
    public void save(OnlinePlayer player) {
        players.put(player.getCharacterId(), player);
    }

    @Override
    public Optional<OnlinePlayer> findByCharacterId(Long characterId) {
        return Optional.ofNullable(players.get(characterId));
    }

    @Override
    public Optional<OnlinePlayer> findBySessionId(String sessionId) {
        return players.values().stream()
                .filter(player -> player.getSessionId().equals(sessionId))
                .findFirst();
    }

    @Override
    public List<OnlinePlayer> findAllInRoom(Long roomId) {
        return players.values().stream()
                .filter(player -> player.isIn(roomId))
                .sorted(Comparator.comparingLong(OnlinePlayer::getEnteredAt))
                .toList();
    }

    @Override
    public List<OnlinePlayer> findAll() {
        return players.values().stream()
                .sorted(Comparator.comparing(OnlinePlayer::getName))
                .toList();
    }

    @Override
    public void delete(Long characterId) {
        players.remove(characterId);
    }
}
