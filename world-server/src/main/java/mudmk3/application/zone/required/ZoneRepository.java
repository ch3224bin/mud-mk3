package mudmk3.application.zone.required;

import mudmk3.domain.zone.Zone;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/** 지역을 저장하고 찾는다 */
public interface ZoneRepository extends Repository<Zone, Long> {

    Zone save(Zone zone);

    Optional<Zone> findById(Long id);

    List<Zone> findAllByOrderByNameAsc();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    void delete(Zone zone);
}
