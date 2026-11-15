package mudmk3.application.mob;

import lombok.RequiredArgsConstructor;
import mudmk3.application.mob.provided.MobPlacementEditor;
import mudmk3.application.mob.provided.MobPlacementSaveRequest;
import mudmk3.application.mob.provided.MobPlacementView;
import mudmk3.application.mob.provided.MobTypeFinder;
import mudmk3.application.mob.required.MobPlacementRepository;
import mudmk3.application.room.provided.RoomDeletedEvent;
import mudmk3.application.room.provided.RoomFinder;
import mudmk3.domain.mob.MobPlacement;
import mudmk3.domain.mob.MobType;
import mudmk3.support.stereotype.ValidatedApplicationService;
import org.springframework.context.event.EventListener;

/** 검사 순서: 없는 방·배치 → 없는 몹 → 수 범위(도메인) */
@ValidatedApplicationService
@RequiredArgsConstructor
public class MobPlacementModifyService implements MobPlacementEditor {

    private final MobPlacementRepository mobPlacementRepository;
    private final MobTypeFinder mobTypeFinder;
    private final RoomFinder roomFinder;

    @Override
    public MobPlacementView create(Long roomId, MobPlacementSaveRequest request) {
        roomFinder.find(roomId);
        MobType mobType = mobTypeFinder.find(request.mobId());
        return MobPlacementView.of(mobPlacementRepository.save(MobPlacement.create(roomId, mobType, request.count())));
    }

    @Override
    public MobPlacementView update(Long placementId, MobPlacementSaveRequest request) {
        MobPlacement placement = find(placementId);
        MobType mobType = mobTypeFinder.find(request.mobId());
        placement.update(mobType, request.count());
        return MobPlacementView.of(mobPlacementRepository.save(placement));
    }

    @Override
    public void delete(Long placementId) {
        mobPlacementRepository.delete(find(placementId));
    }

    /** 방이 지워지면 그 방의 몹 배치를 지운다(방을 지우는 트랜잭션 안에서) */
    @EventListener
    public void onRoomDeleted(RoomDeletedEvent event) {
        mobPlacementRepository.deleteByRoomId(event.roomId());
    }

    private MobPlacement find(Long placementId) {
        return mobPlacementRepository.findById(placementId)
                .orElseThrow(() -> new IllegalArgumentException("몹 배치를 찾을 수 없습니다. id: " + placementId));
    }
}
