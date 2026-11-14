package mudmk3.application.technique.provided;

import jakarta.validation.Valid;

/** 공법·신통을 만들고 고치고 지운다 */
public interface TechniqueEditor {

    TechniqueView create(@Valid TechniqueSaveRequest request);

    /** 단계 표와 신통은 통째로 바꾼다 */
    TechniqueView update(Long techniqueId, @Valid TechniqueSaveRequest request);

    void delete(Long techniqueId);
}
