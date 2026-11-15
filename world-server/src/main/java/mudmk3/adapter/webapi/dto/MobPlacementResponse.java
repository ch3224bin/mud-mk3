package mudmk3.adapter.webapi.dto;

import mudmk3.application.mob.provided.MobPlacementView;

/** 방에 둔 몹 배치 한 줄 */
public record MobPlacementResponse(Long id, Long roomId, Long mobId, String mobName, int count) {

    public static MobPlacementResponse of(MobPlacementView view) {
        return new MobPlacementResponse(view.id(), view.roomId(), view.mobId(), view.mobName(), view.count());
    }
}
