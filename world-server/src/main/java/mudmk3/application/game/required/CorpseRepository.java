package mudmk3.application.game.required;

import mudmk3.domain.combat.Corpse;

import java.util.List;

/** 쓰러진 몹이 남긴 시체를 메모리에 둔다 */
public interface CorpseRepository {

    /** 넣거나 바꾼다(시체 id 기준) */
    void save(Corpse corpse);

    /** 그 방의 시체(쓰러진 순) */
    List<Corpse> findAllInRoom(Long roomId);

    /** 모든 시체(쓰러진 순) */
    List<Corpse> findAll();

    void delete(long id);

    void deleteAll();
}
