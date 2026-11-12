package mudmk3.application.room.provided;

import mudmk3.domain.room.Room;

import java.util.List;
import java.util.Optional;

/** 방을 찾는 기능을 제공한다. 돌려주는 방은 지역·출구·출구 대상 방과 그 지역까지 읽혀 있다 */
public interface RoomFinder {

    Room find(Long roomId);

    Optional<Room> findOptional(Long roomId);

    /** 그 지역의 방(id순). 출구 수를 셀 수 있게 출구가 읽혀 있다 */
    List<Room> findAllInZone(Long zoneId);
}
