package mudmk3.application.zone;

import lombok.RequiredArgsConstructor;
import mudmk3.application.zone.provided.ZoneEditor;
import mudmk3.application.zone.provided.ZoneFinder;
import mudmk3.application.zone.provided.ZoneNotEmptyException;
import mudmk3.application.zone.provided.ZoneSaveRequest;
import mudmk3.application.zone.required.ZoneRepository;
import mudmk3.domain.zone.DuplicateZoneNameException;
import mudmk3.domain.zone.Zone;
import mudmk3.domain.zone.ZoneSaveInfo;
import mudmk3.support.stereotype.ValidatedApplicationService;

@ValidatedApplicationService
@RequiredArgsConstructor
public class ZoneModifyService implements ZoneEditor {

    private final ZoneFinder zoneFinder;
    private final ZoneRepository zoneRepository;

    @Override
    public Zone create(ZoneSaveRequest request) {
        ZoneSaveInfo info = request.toInfo();
        if (zoneRepository.existsByName(info.name())) {
            throw duplicateName(info.name());
        }
        return zoneRepository.save(Zone.create(info));
    }

    @Override
    public Zone update(Long zoneId, ZoneSaveRequest request) {
        Zone zone = zoneFinder.find(zoneId);
        ZoneSaveInfo info = request.toInfo();
        if (zoneRepository.existsByNameAndIdNot(info.name(), zoneId)) {
            throw duplicateName(info.name());
        }
        zone.update(info);
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

    private static DuplicateZoneNameException duplicateName(String name) {
        return new DuplicateZoneNameException("이미 있는 지역 이름입니다: " + name);
    }
}
