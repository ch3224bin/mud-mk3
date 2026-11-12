package mudmk3.application.room.required;

import mudmk3.domain.room.Room;
import mudmk3.domain.zone.Zone;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/** 방(출구 포함)을 저장하고 찾는다 */
public interface RoomRepository extends Repository<Room, Long> {

    Room save(Room room);

    /** 지역·출구·출구 대상 방과 그 지역까지 함께 읽는다(트랜잭션 밖에서도 읽을 수 있게) */
    @EntityGraph(attributePaths = {"zone", "exits", "exits.target", "exits.target.zone"})
    Optional<Room> findById(Long id);

    /** 출구 수를 셀 수 있게 출구를 함께 읽는다 */
    @EntityGraph(attributePaths = "exits")
    List<Room> findAllByZoneOrderByIdAsc(Zone zone);

    long countByZone(Zone zone);

    /** 그 방으로 가는 출구를 가진 방들(id순) */
    @Query("select distinct r from Room r join r.exits e where e.target = :target order by r.id")
    List<Room> findAllWithExitTo(Room target);

    void delete(Room room);

    /** 지금까지의 변경을 DB에 반영한다(외래 키 순서를 지켜야 할 때) */
    void flush();
}
