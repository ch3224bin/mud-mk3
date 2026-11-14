package mudmk3.application.floor.provided;

import mudmk3.domain.floor.FloorPlacement;

import java.util.List;

/** 관리자가 둔 바닥 물건을 찾는다 */
public interface FloorPlacementFinder {

    FloorPlacement find(Long floorPlacementId);

    /** 그 방의 배치(id 순). 물건 종류까지 읽혀 있다 */
    List<FloorPlacement> findAllInRoom(Long roomId);
}
