package mudmk3.application.technique;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.util.Objects.requireNonNull;

@Slf4j
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

    /**
     * 서버가 뜰 때 6차 전에 만든 신통의 비어 있는 이펙트 문장·단계별 값을 채운다
     * (경지 채우기 {@code @Order(0)}·영석 심기 {@code @Order(10)} 다음). 검사를 거치지 않는다
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(20)
    @Transactional
    public void fillMissingAbilityData() {
        int filled = 0;
        for (Technique technique : techniqueRepository.findAllByOrderByNameAsc()) {
            int count = technique.fillMissingAbilityData();
            if (count > 0) {
                techniqueRepository.save(technique);
                filled += count;
            }
        }
        if (filled > 0) {
            log.info("기존 신통 {}개의 이펙트 문장·단계별 값을 채웠습니다.", filled);
        }
    }

    /** 익힌 캐릭터의 단계보다 단계 수를 줄이면 {@code TechniqueInUseException}(409) */
    private void checkStageCount(Technique technique, int newCount) {
        int maxLearned = techniqueRepository.maxLearnedStage(requireNonNull(technique.getId())).orElse(0);
        if (newCount < maxLearned) {
            throw new TechniqueInUseException("이 공법을 더 높은 단계까지 익힌 캐릭터가 있어 단계 수를 줄일 수 없습니다.");
        }
    }

    /** 익힌 캐릭터가 있으면, 그다음 담긴 물건이 있으면 {@code TechniqueInUseException}(409) */
    private void checkDeletable(Technique technique) {
        if (techniqueRepository.countLearners(requireNonNull(technique.getId())) > 0) {
            throw new TechniqueInUseException("익힌 캐릭터가 있는 공법은 지울 수 없습니다: " + technique.getName());
        }
        List<ScrollItem> scrollItems = techniqueRepository.findScrollItems(requireNonNull(technique.getId()));
        if (!scrollItems.isEmpty()) {
            throw new TechniqueInUseException("이 공법이 담긴 물건이 있어 지울 수 없습니다: " + scrollItems.getFirst().name());
        }
    }

    private static DuplicateTechniqueNameException duplicateName(String name) {
        return new DuplicateTechniqueNameException("이미 있는 공법 이름입니다: " + name);
    }
}
