package mudmk3.application.technique;

import lombok.RequiredArgsConstructor;
import mudmk3.application.technique.provided.TechniqueFinder;
import mudmk3.application.technique.provided.TechniqueView;
import mudmk3.application.technique.required.TechniqueRepository;
import mudmk3.domain.technique.Technique;
import mudmk3.support.stereotype.ApplicationService;

import java.util.List;
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
        return toView(find(techniqueId));
    }

    @Override
    public List<TechniqueView> viewAll() {
        return techniqueRepository.findAllByOrderByNameAsc().stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public Optional<String> findAbilityName(String name) {
        return techniqueRepository.findAbilityNames(name).stream().findFirst();
    }

    private TechniqueView toView(Technique technique) {
        Long id = requireNonNull(technique.getId());
        return TechniqueView.of(technique, techniqueRepository.countLearners(id),
                techniqueRepository.findScrollItems(id));
    }
}
