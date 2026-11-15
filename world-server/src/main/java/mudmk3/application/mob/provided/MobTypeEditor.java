package mudmk3.application.mob.provided;

import jakarta.validation.Valid;

/** 몹 유형을 만들고 고치고 지운다 */
public interface MobTypeEditor {

    MobTypeView create(@Valid MobTypeSaveRequest request);

    /** 드롭 표는 통째로 바꾼다 */
    MobTypeView update(Long mobTypeId, @Valid MobTypeSaveRequest request);

    /** 배치된 몹은 {@code MobTypeInUseException}(409) */
    void delete(Long mobTypeId);
}
