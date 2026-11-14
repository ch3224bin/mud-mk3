package mudmk3.application.faction;

import lombok.RequiredArgsConstructor;
import mudmk3.application.faction.provided.FactionFinder;
import mudmk3.application.faction.required.FactionRepository;
import mudmk3.domain.faction.Faction;
import mudmk3.support.stereotype.ApplicationService;

import java.util.Comparator;
import java.util.List;

@ApplicationService
@RequiredArgsConstructor
public class FactionQueryService implements FactionFinder {

    private final FactionRepository factionRepository;

    @Override
    public Faction find(Long factionId) {
        return factionRepository.findById(factionId)
                .orElseThrow(() -> new IllegalArgumentException("성·종문을 찾을 수 없습니다. id: " + factionId));
    }

    @Override
    public List<Faction> findAll() {
        return factionRepository.findAll().stream()
                .sorted(Comparator.comparing(Faction::getKind).thenComparing(Faction::getName))
                .toList();
    }

    @Override
    public long countZones(Long factionId) {
        return factionRepository.countZones(factionId);
    }
}
