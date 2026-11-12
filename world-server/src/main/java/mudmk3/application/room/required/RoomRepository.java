package mudmk3.application.room.required;

import mudmk3.domain.room.Room;
import mudmk3.domain.zone.Zone;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/** 방(출구 포함)을 저장하고 찾는다 */
public interface RoomRepository extends Repository<Room, Long> {

    Room save(Room room);

    Optional<Room> findById(Long id);

    List<Room> findAllByZoneOrderByIdAsc(Zone zone);

    long countByZone(Zone zone);

    /** 그 방으로 가는 출구를 가진 방들(id순) */
    @Query("select distinct r from Room r join r.exits e where e.target = :target order by r.id")
    List<Room> findAllWithExitTo(Room target);

    void delete(Room room);
}
