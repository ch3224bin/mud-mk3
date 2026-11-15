package mudmk3.adapter.webapi.dto;

import mudmk3.adapter.webapi.dto.MobDetailResponse.StatsResponse;
import mudmk3.application.mob.provided.MobTypeView;
import mudmk3.domain.mob.MobKind;
import mudmk3.domain.mob.MobTier;

/** 몹 목록 한 줄. {@code rankName} 은 요수 {@code 1급}, 나머지 {@code 연기 6층} */
public record MobSummaryResponse(
        Long id,
        String name,
        MobKind kind,
        String kindName,
        String rankName,
        MobTier tier,
        String tierName,
        StatsResponse stats,
        int effectiveRespawnSeconds,
        int placementCount
) {

    public static MobSummaryResponse of(MobTypeView view) {
        return new MobSummaryResponse(view.id(), view.name(), view.kind(), view.kindName(), view.rankName(),
                view.tier(), view.tierName(), StatsResponse.of(view.stats()), view.effectiveRespawnSeconds(),
                view.placementCount());
    }
}
