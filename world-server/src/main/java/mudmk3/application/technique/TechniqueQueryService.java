package mudmk3.application.technique;

import lombok.RequiredArgsConstructor;
import mudmk3.application.technique.provided.ContainedTechniqueView;
import mudmk3.application.technique.provided.TechniqueFinder;
import mudmk3.application.technique.provided.TechniqueView;
import mudmk3.application.technique.required.LearnerCount;
import mudmk3.application.technique.required.ScrollItem;
import mudmk3.application.technique.required.ScrollItemRow;
import mudmk3.application.technique.required.TechniqueRepository;
import mudmk3.domain.technique.Technique;
import mudmk3.support.stereotype.ApplicationService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@ApplicationService
@RequiredArgsConstructor
public class TechniqueQueryService implements TechniqueFinder {

    private final TechniqueRepository techniqueRepository;

    @Override
    public Technique find(Long techniqueId) {
        return techniqueRepository.findById(techniqueId)
                .orElseThrow(() -> new IllegalArgumentException("공법을 찾을 수 없습니다. id: " + techniqueId));
    }

    @Override
    public TechniqueView view(Long techniqueId) {
        Technique technique = find(techniqueId);
        return TechniqueView.of(technique, techniqueRepository.countLearners(techniqueId),
                techniqueRepository.findScrollItems(techniqueId));
    }

    @Override
    public ContainedTechniqueView viewContained(Long techniqueId) {
        return ContainedTechniqueView.of(find(techniqueId));
    }

    /** 공법 수와 상관없이 정해진 수의 쿼리로 읽는다(익힌 수·담긴 물건은 모든 공법 것을 한 번에) */
    @Override
    public List<TechniqueView> viewAll() {
        List<Technique> techniques = techniqueRepository.findAllByOrderByNameAsc();
        if (techniques.isEmpty()) return List.of();
        List<Long> ids = techniques.stream().map(technique -> requireNonNull(technique.getId())).toList();
        Map<Long, Long> learnerCounts = new HashMap<>();
        for (LearnerCount learnerCount : techniqueRepository.countLearnersByTechnique()) {
            learnerCounts.put(learnerCount.techniqueId(), learnerCount.count());
        }
        Map<Long, List<ScrollItem>> scrollItems = new HashMap<>();
        for (ScrollItemRow row : techniqueRepository.findScrollItemsIn(ids)) {
            scrollItems.computeIfAbsent(row.techniqueId(), id -> new ArrayList<>())
                    .add(new ScrollItem(row.id(), row.name()));
        }
        return techniques.stream()
                .map(technique -> {
                    Long id = requireNonNull(technique.getId());
                    return TechniqueView.of(technique, learnerCounts.getOrDefault(id, 0L),
                            scrollItems.getOrDefault(id, List.of()));
                })
                .toList();
    }

    @Override
    public Optional<String> findAbilityName(String name) {
        return techniqueRepository.findAbilityNames(name).stream().findFirst();
    }
}
