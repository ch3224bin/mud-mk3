package mudmk3.adapter.webapi.dto;

import mudmk3.domain.faction.Faction;
import mudmk3.domain.faction.FactionKind;

public record FactionResponse(
        Long id,
        FactionKind kind,
        String kindName,
        String name,
        String description,
        long zoneCount
) {

    public static FactionResponse of(Faction faction, long zoneCount) {
        return new FactionResponse(faction.getId(), faction.getKind(), faction.getKind().koreanName(),
                faction.getName(), faction.getDescription(), zoneCount);
    }
}
