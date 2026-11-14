package mudmk3.application.technique;

import lombok.RequiredArgsConstructor;
import mudmk3.application.technique.provided.TechniqueEditor;
import mudmk3.application.technique.provided.TechniqueFinder;
import mudmk3.application.technique.provided.TechniqueInUseException;
import mudmk3.application.technique.provided.TechniqueSaveRequest;
import mudmk3.application.technique.provided.TechniqueView;
import mudmk3.application.technique.required.ScrollItem;
import mudmk3.application.technique.required.TechniqueRepository;
import mudmk3.domain.technique.DuplicateTechniqueNameException;
import mudmk3.domain.technique.Technique;
import mudmk3.domain.technique.TechniqueSaveInfo;
import mudmk3.support.stereotype.ValidatedApplicationService;

import java.util.List;

import static java.util.Objects.requireNonNull;

@ValidatedApplicationService
@RequiredArgsConstructor
public class TechniqueModifyService implements TechniqueEditor {

    private final TechniqueFinder techniqueFinder;
    private final TechniqueRepository techniqueRepository;

    @Override
    public TechniqueView create(TechniqueSaveRequest request) {
        TechniqueSaveInfo info = request.toInfo();
        if (techniqueRepository.existsByNameIgnoreCase(info.name())) {
            throw duplicateName(info.name());
        }
        Technique technique = techniqueRepository.save(Technique.create(info));
        return techniqueFinder.view(requireNonNull(technique.getId()));
    }

    @Override
    public TechniqueView update(Long techniqueId, TechniqueSaveRequest request) {
        Technique technique = techniqueFinder.find(techniqueId);
        TechniqueSaveInfo info = request.toInfo();
        if (techniqueRepository.existsByNameIgnoreCaseAndIdNot(info.name(), techniqueId)) {
            throw duplicateName(info.name());
        }
        checkStageCount(technique, info.stages().size());
        technique.update(info);
        techniqueRepository.save(technique);
        return techniqueFinder.view(techniqueId);
    }

    @Override
    public void delete(Long techniqueId) {
        Technique technique = techniqueFinder.find(techniqueId);
        checkDeletable(technique);
        techniqueRepository.delete(technique);
    }

    /** 익힌 캐릭터의 단계보다 단계 수를 줄이면 {@code TechniqueInUseException}(409). Task 5가 채운다 */
    @SuppressWarnings("unused")
    private void checkStageCount(Technique technique, int newCount) {
    }

    /** 익힌 캐릭터·담긴 물건이 있으면 {@code TechniqueInUseException}(409). 익힌 캐릭터는 Task 5가 채운다 */
    private void checkDeletable(Technique technique) {
        List<ScrollItem> scrollItems = techniqueRepository.findScrollItems(requireNonNull(technique.getId()));
        if (!scrollItems.isEmpty()) {
            throw new TechniqueInUseException("이 공법이 담긴 물건이 있어 지울 수 없습니다: " + scrollItems.getFirst().name());
        }
    }

    private static DuplicateTechniqueNameException duplicateName(String name) {
        return new DuplicateTechniqueNameException("이미 있는 공법 이름입니다: " + name);
    }
}
