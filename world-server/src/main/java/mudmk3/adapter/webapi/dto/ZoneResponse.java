package mudmk3.adapter.webapi.dto;

import mudmk3.domain.faction.Faction;
import mudmk3.domain.zone.Zone;
import org.jspecify.annotations.Nullable;

/** 지역. 소유 성·종문이 없으면 소유 세 값 모두 null. ownerKind 는 종류 이름(CASTLE·SECT) */
public record ZoneResponse(
        Long id,
        String name,
        String description,
        long roomCount,
        @Nullable Long ownerId,
        @Nullable String ownerName,
        @Nullable String ownerKind
) {

    public static ZoneResponse of(Zone zone, long roomCount) {
        Faction owner = zone.findOwner().orElse(null);
        return new ZoneResponse(zone.getId(), zone.getName(), zone.getDescription(), roomCount,
                owner == null ? null : owner.getId(),
                owner == null ? null : owner.getName(),
                owner == null ? null : owner.getKind().name());
    }
}
