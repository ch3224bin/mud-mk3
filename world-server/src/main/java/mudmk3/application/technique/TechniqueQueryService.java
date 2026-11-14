package mudmk3.application.technique;

import lombok.RequiredArgsConstructor;
import mudmk3.application.technique.provided.TechniqueFinder;
import mudmk3.application.technique.provided.TechniqueView;
import mudmk3.application.technique.required.TechniqueRepository;
import mudmk3.domain.technique.Technique;
import mudmk3.support.stereotype.ApplicationService;

import java.util.List;

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
        return toView(find(techniqueId));
    }

    @Override
    public List<TechniqueView> viewAll() {
        return techniqueRepository.findAllByOrderByNameAsc().stream()
                .map(this::toView)
                .toList();
    }

    /** 익힌 캐릭터 수·담긴 물건은 Task 3·5가 센다(4차 Task 2에서는 0·빈 목록) */
    private TechniqueView toView(Technique technique) {
        return TechniqueView.of(technique, 0, List.of());
    }
}
