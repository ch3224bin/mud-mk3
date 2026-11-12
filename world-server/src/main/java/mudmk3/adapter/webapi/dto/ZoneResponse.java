package mudmk3.adapter.webapi.dto;

import mudmk3.domain.zone.Zone;

public record ZoneResponse(Long id, String name, String description, long roomCount) {

    public static ZoneResponse of(Zone zone, long roomCount) {
        return new ZoneResponse(zone.getId(), zone.getName(), zone.getDescription(), roomCount);
    }
}
