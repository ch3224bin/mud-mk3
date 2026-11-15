package mudmk3.adapter.webapi.dto;

import mudmk3.application.mob.provided.MobTypeView;
import mudmk3.domain.mob.DropTier;
import mudmk3.domain.mob.MobKind;
import mudmk3.domain.mob.MobStats;
import mudmk3.domain.mob.MobTier;
import mudmk3.domain.shared.Realm;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;

/**
 * 몹 상세: 저장 칸 전부와 {@code id}·{@code kindName}·{@code realmName}·{@code tierName}·{@code rankName}(요수 {@code 1급},
 * 나머지 {@code 연기 6층})·계산한 {@code stats}·{@code effectiveRespawnSeconds}·배치된 방 {@code placements}(배치 id 순)
 */
public record MobDetailResponse(
        Long id,
        String name,
        String description,
        MobKind kind,
        String kindName,
        Realm realm,
        String realmName,
        MobTier tier,
        String tierName,
        String rankName,
        String attackName,
        @Nullable BigDecimal attackCoefficient,
        @Nullable Integer respawnSeconds,
        int effectiveRespawnSeconds,
        StatsResponse stats,
        List<FixedDropResponse> fixedDrops,
        List<TierDropResponse> tierDrops,
        List<PlacementResponse> placements
) {

    public static MobDetailResponse of(MobTypeView view) {
        return new MobDetailResponse(view.id(), view.name(), view.description(), view.kind(), view.kindName(),
                view.realm(), view.realmName(), view.tier(), view.tierName(), view.rankName(), view.attackName(),
                view.attackCoefficient(), view.respawnSeconds(), view.effectiveRespawnSeconds(),
                StatsResponse.of(view.stats()),
                view.fixedDrops().stream().map(FixedDropResponse::of).toList(),
                view.tierDrops().stream().map(TierDropResponse::of).toList(),
                view.placements().stream().map(PlacementResponse::of).toList());
    }

    /** 계산한 몹 스탯 */
    public record StatsResponse(int qiBlood, int body, int power) {
        static StatsResponse of(MobStats stats) {
            return new StatsResponse(stats.qiBlood(), stats.body(), stats.power());
        }
    }

    public record FixedDropResponse(Long itemTypeId, String itemTypeName, int count) {
        static FixedDropResponse of(MobTypeView.FixedDropView drop) {
            return new FixedDropResponse(drop.itemTypeId(), drop.itemTypeName(), drop.count());
        }
    }

    public record TierDropResponse(DropTier tier, String tierName, Long itemTypeId, String itemTypeName) {
        static TierDropResponse of(MobTypeView.TierDropView drop) {
            return new TierDropResponse(drop.tier(), drop.tierName(), drop.itemTypeId(), drop.itemTypeName());
        }
    }

    public record PlacementResponse(Long id, Long roomId, String roomName, int count) {
        static PlacementResponse of(MobTypeView.PlacementView placement) {
            return new PlacementResponse(placement.id(), placement.roomId(), placement.roomName(), placement.count());
        }
    }
}
