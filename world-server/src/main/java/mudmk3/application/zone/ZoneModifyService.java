package mudmk3.application.zone;

import lombok.RequiredArgsConstructor;
import mudmk3.application.faction.provided.FactionFinder;
import mudmk3.application.zone.provided.ZoneEditor;
import mudmk3.application.zone.provided.ZoneFinder;
import mudmk3.application.zone.provided.ZoneNotEmptyException;
import mudmk3.application.zone.provided.ZoneSaveRequest;
import mudmk3.application.zone.required.ZoneRepository;
import mudmk3.domain.faction.Faction;
import mudmk3.domain.zone.DuplicateZoneNameException;
import mudmk3.domain.zone.Zone;
import mudmk3.domain.zone.ZoneSaveInfo;
import mudmk3.support.stereotype.ValidatedApplicationService;
import org.jspecify.annotations.Nullable;

@ValidatedApplicationService
@RequiredArgsConstructor
public class ZoneModifyService implements ZoneEditor {

    private final ZoneFinder zoneFinder;
    private final ZoneRepository zoneRepository;
    private final FactionFinder factionFinder;

    @Override
    public Zone create(ZoneSaveRequest request) {
        ZoneSaveInfo info = request.toInfo();
        if (zoneRepository.existsByName(info.name())) {
            throw duplicateName(info.name());
        }
        return zoneRepository.save(Zone.create(info, findOwner(request.ownerId())));
    }

    @Override
    public Zone update(Long zoneId, ZoneSaveRequest request) {
        Zone zone = zoneFinder.find(zoneId);
        ZoneSaveInfo info = request.toInfo();
        if (zoneRepository.existsByNameAndIdNot(info.name(), zoneId)) {
            throw duplicateName(info.name());
        }
        zone.update(info, findOwner(request.ownerId()));
        return zoneRepository.save(zone);
    }

    @Override
    public void delete(Long zoneId) {
        Zone zone = zoneFinder.find(zoneId);
        if (zoneFinder.countRooms(zoneId) > 0) {
            throw new ZoneNotEmptyException("방이 남아 있는 지역은 지울 수 없습니다");
        }
        zoneRepository.delete(zone);
    }

    private @Nullable Faction findOwner(@Nullable Long ownerId) {
        return ownerId == null ? null : factionFinder.find(ownerId);
    }

    private static DuplicateZoneNameException duplicateName(String name) {
        return new DuplicateZoneNameException("이미 있는 지역 이름입니다: " + name);
    }
}
