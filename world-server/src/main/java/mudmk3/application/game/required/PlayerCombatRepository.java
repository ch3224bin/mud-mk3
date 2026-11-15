package mudmk3.application.game.required;

import mudmk3.domain.combat.PlayerCombat;

import java.util.List;
import java.util.Optional;

/** 플레이어의 교전 상태를 메모리에 둔다 */
public interface PlayerCombatRepository {

    /** 넣거나 바꾼다(캐릭터 id 기준) */
    void save(PlayerCombat combat);

    Optional<PlayerCombat> find(Long characterId);

    /** 모든 교전 상태(캐릭터 id 순) */
    List<PlayerCombat> findAll();

    void delete(Long characterId);

    void deleteAll();
}
