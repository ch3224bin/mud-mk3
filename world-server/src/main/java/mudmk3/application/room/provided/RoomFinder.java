package mudmk3.application.room.provided;

import mudmk3.domain.room.Room;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 방을 찾는 기능을 제공한다. 따로 적지 않으면 돌려주는 방은 지역·출구·출구 대상 방과 그 지역까지 읽혀 있다 */
public interface RoomFinder {

    Room find(Long roomId);

    Optional<Room> findOptional(Long roomId);

    /** 방 id → 방(지역까지 읽혀 있고 출구는 읽지 않는다). 없는 방은 빠진다. 한 쿼리로 읽는다 */
    Map<Long, Room> findAllById(Collection<Long> roomIds);

    /** 방 id → 방 이름(출구·지역은 읽지 않는다). 없는 방은 빠진다. 한 쿼리로 읽는다 */
    Map<Long, String> findNames(Collection<Long> roomIds);

    /** 그 지역의 방(id순). 출구 수를 셀 수 있게 출구가 읽혀 있다 */
    List<Room> findAllInZone(Long zoneId);
}
