package mudmk3.application.mob.provided;

import mudmk3.domain.mob.MobPlacement;

import java.util.List;

/** 몹 배치를 찾는다 */
public interface MobPlacementFinder {

    /** 모든 배치(id 순). 몹 유형과 그 드롭 표까지 읽혀 있다(서버 틱이 배치 맞추기에 쓴다) */
    List<MobPlacement> findAll();

    /** 그 방의 배치(id 순) */
    List<MobPlacementView> findAllInRoom(Long roomId);
}
