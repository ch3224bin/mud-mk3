package mudmk3.application.zone;

import lombok.RequiredArgsConstructor;
import mudmk3.application.zone.provided.ZoneFinder;
import mudmk3.application.zone.required.ZoneRepository;
import mudmk3.domain.zone.Zone;
import mudmk3.support.stereotype.ApplicationService;

import java.util.List;

@ApplicationService
@RequiredArgsConstructor
public class ZoneQueryService implements ZoneFinder {

    private final ZoneRepository zoneRepository;

    @Override
    public Zone find(Long zoneId) {
        return zoneRepository.findById(zoneId)
                .orElseThrow(() -> new IllegalArgumentException("지역을 찾을 수 없습니다. id: " + zoneId));
    }

    @Override
    public List<Zone> findAll() {
        return zoneRepository.findAllByOrderByNameAsc();
    }

    @Override
    public long countRooms(Long zoneId) {
        return zoneRepository.countRooms(zoneId);
    }
}
