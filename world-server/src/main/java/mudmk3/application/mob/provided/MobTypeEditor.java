package mudmk3.application.mob.provided;

import jakarta.validation.Valid;

/** 몹 유형을 만들고 고치고 지운다 */
public interface MobTypeEditor {

    MobTypeView create(@Valid MobTypeSaveRequest request);

    /** 드롭 표는 통째로 바꾼다 */
    MobTypeView update(Long mobTypeId, @Valid MobTypeSaveRequest request);

    /** 그 몹의 배치도 함께 지운다 */
    DeletedMobType delete(Long mobTypeId);
}
