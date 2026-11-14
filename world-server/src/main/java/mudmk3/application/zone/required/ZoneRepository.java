package mudmk3.application.zone.required;

import mudmk3.domain.zone.Zone;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/** 지역을 저장하고 찾는다 */
public interface ZoneRepository extends Repository<Zone, Long> {

    Zone save(Zone zone);

    /** 소유 성·종문을 함께 읽는다(트랜잭션 밖에서도 읽을 수 있게) */
    @EntityGraph(attributePaths = "owner")
    Optional<Zone> findById(Long id);

    /** 소유 성·종문을 함께 읽는다 */
    @EntityGraph(attributePaths = "owner")
    List<Zone> findAllByOrderByNameAsc();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    void delete(Zone zone);

    /** 그 지역에 속한 방 수. room 슬라이스에 기대지 않으려고 JPQL로 센다 */
    @Query("select count(r) from Room r where r.zone.id = :zoneId")
    long countRooms(Long zoneId);
}
