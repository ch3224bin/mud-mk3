package mudmk3.application.world.provided;

import mudmk3.domain.room.Room;
import mudmk3.domain.world.StartingItem;

import java.util.List;
import java.util.Optional;

/** 세계 설정(시작 방·시작 소지품)을 알려 주는 기능을 제공한다 */
public interface WorldSettingsFinder {

    /** 시작 방. 지역까지 읽혀 있다 */
    Optional<Room> findStartRoom();

    boolean isStartRoom(Long roomId);

    /** 시작 소지품(입력 순서). 세계 설정 행이 없으면 빈 목록 */
    List<StartingItem> findStartingItems();
}
