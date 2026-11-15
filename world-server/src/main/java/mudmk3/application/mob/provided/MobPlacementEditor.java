package mudmk3.application.mob.provided;

import jakarta.validation.Valid;

/** 관리자가 방에 몹을 두고 고치고 치운다 */
public interface MobPlacementEditor {

    /** 없는 방은 1차 문구, 없는 몹은 {@code 몹을 찾을 수 없습니다. id: N}(400) */
    MobPlacementView create(Long roomId, @Valid MobPlacementSaveRequest request);

    /** 몹과 수를 바꾼다. 없는 배치는 {@code 몹 배치를 찾을 수 없습니다. id: N}(400) */
    MobPlacementView update(Long placementId, @Valid MobPlacementSaveRequest request);

    void delete(Long placementId);
}
