package mudmk3.application.zone.provided;

import mudmk3.domain.zone.Zone;

import java.util.List;

/** 지역을 찾는 기능을 제공한다 */
public interface ZoneFinder {

    Zone find(Long zoneId);

    /** 이름순 */
    List<Zone> findAll();

    long countRooms(Long zoneId);
}
