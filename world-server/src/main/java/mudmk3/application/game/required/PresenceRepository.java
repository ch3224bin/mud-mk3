package mudmk3.application.game.required;

import mudmk3.domain.game.OnlinePlayer;

import java.util.List;
import java.util.Optional;

/** 접속 중인 플레이어(누가 어느 방에 있나)를 메모리에 둔다 */
public interface PresenceRepository {

    /** 넣거나 바꾼다(캐릭터 id 기준) */
    void save(OnlinePlayer player);

    Optional<OnlinePlayer> findByCharacterId(Long characterId);

    Optional<OnlinePlayer> findBySessionId(String sessionId);

    /** 그 방에 있는 사람(들어온 순서) */
    List<OnlinePlayer> findAllInRoom(Long roomId);

    /** 접속한 모든 사람(이름순) */
    List<OnlinePlayer> findAll();

    void delete(Long characterId);
}
