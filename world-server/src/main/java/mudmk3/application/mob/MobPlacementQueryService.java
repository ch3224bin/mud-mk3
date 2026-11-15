package mudmk3.application.mob;

import lombok.RequiredArgsConstructor;
import mudmk3.application.mob.provided.MobPlacementFinder;
import mudmk3.application.mob.provided.MobPlacementView;
import mudmk3.application.mob.required.MobPlacementRepository;
import mudmk3.domain.mob.MobPlacement;
import mudmk3.support.stereotype.ApplicationService;

import java.util.List;

@ApplicationService
@RequiredArgsConstructor
public class MobPlacementQueryService implements MobPlacementFinder {

    private final MobPlacementRepository mobPlacementRepository;

    @Override
    public List<MobPlacement> findAll() {
        return mobPlacementRepository.findAllByOrderByIdAsc();
    }

    @Override
    public List<MobPlacementView> findAllInRoom(Long roomId) {
        return mobPlacementRepository.findAllByRoomIdOrderByIdAsc(roomId).stream()
                .map(MobPlacementView::of)
                .toList();
    }
}
