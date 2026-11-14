package mudmk3.application.faction.required;

import mudmk3.domain.faction.Faction;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/** 성·종문을 저장하고 찾는다 */
public interface FactionRepository extends Repository<Faction, Long> {

    Faction save(Faction faction);

    Optional<Faction> findById(Long id);

    List<Faction> findAll();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    void delete(Faction faction);

    /** 그 성·종문이 소유한 지역 수. zone 슬라이스에 기대지 않으려고 JPQL로 센다 */
    @Query("select count(z) from Zone z where z.owner.id = :factionId")
    long countZones(Long factionId);
}
