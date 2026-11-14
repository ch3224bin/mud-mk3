package mudmk3.application.floor.provided;

import jakarta.validation.Valid;
import mudmk3.domain.floor.FloorPlacement;

/** 관리자가 방 바닥에 물건을 두고 고치고 치운다 */
public interface FloorPlacementEditor {

    FloorPlacement create(Long roomId, @Valid FloorPlacementSaveRequest request);

    /** 물건·수·재등장을 바꾸고 남은 수를 처음 수로 되돌린다 */
    FloorPlacement update(Long floorPlacementId, @Valid FloorPlacementSaveRequest request);

    void delete(Long floorPlacementId);
}
