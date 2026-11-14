package mudmk3.application.floor;

import lombok.RequiredArgsConstructor;
import mudmk3.application.floor.provided.FloorPlacementFinder;
import mudmk3.application.floor.required.FloorPlacementRepository;
import mudmk3.domain.floor.FloorPlacement;
import mudmk3.support.stereotype.ApplicationService;

import java.util.List;

@ApplicationService
@RequiredArgsConstructor
public class FloorQueryService implements FloorPlacementFinder {

    private final FloorPlacementRepository floorPlacementRepository;

    @Override
    public FloorPlacement find(Long floorPlacementId) {
        return floorPlacementRepository.findById(floorPlacementId)
                .orElseThrow(() -> new IllegalArgumentException("바닥 물건을 찾을 수 없습니다. id: " + floorPlacementId));
    }

    @Override
    public List<FloorPlacement> findAllInRoom(Long roomId) {
        return floorPlacementRepository.findAllByRoomIdOrderByIdAsc(roomId);
    }
}
