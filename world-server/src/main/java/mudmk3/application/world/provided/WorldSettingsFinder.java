package mudmk3.application.world.provided;

import mudmk3.domain.room.Room;

import java.util.Optional;

/** 세계 설정(시작 방)을 알려 주는 기능을 제공한다 */
public interface WorldSettingsFinder {

    /** 시작 방. 지역까지 읽혀 있다 */
    Optional<Room> findStartRoom();

    boolean isStartRoom(Long roomId);
}
