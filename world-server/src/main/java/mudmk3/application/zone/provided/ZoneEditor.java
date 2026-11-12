package mudmk3.application.zone.provided;

import jakarta.validation.Valid;
import mudmk3.domain.zone.Zone;

/** 지역을 만들고 고치고 지우는 기능을 제공한다 */
public interface ZoneEditor {

    Zone create(@Valid ZoneSaveRequest request);

    Zone update(Long zoneId, @Valid ZoneSaveRequest request);

    void delete(Long zoneId);
}
