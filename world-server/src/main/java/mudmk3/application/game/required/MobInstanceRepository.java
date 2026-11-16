package mudmk3.application.game.required;

import mudmk3.domain.combat.MobInstance;

import java.util.List;
import java.util.Optional;

/** 세계에 나타난 몹(살아 있거나 재등장을 기다리는)을 메모리에 둔다 */
public interface MobInstanceRepository {

    /** 넣거나 바꾼다(몹 id 기준) */
    void save(MobInstance mob);

    Optional<MobInstance> findById(long id);

    /** 그 방의 살아 있는 몹((배치 id, 자리) 순) */
    List<MobInstance> findAllInRoom(Long roomId);

    /** 모든 몹(쓰러진 것 포함, (배치 id, 자리) 순) */
    List<MobInstance> findAll();

    void delete(long id);

    /** 그 방의 몹을 모두 지운다(쓰러진 것 포함) */
    void deleteAllInRoom(Long roomId);

    void deleteAll();
}
