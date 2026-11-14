package mudmk3.application.faction.provided;

import mudmk3.domain.faction.Faction;

import java.util.List;

/** 성·종문을 찾는다 */
public interface FactionFinder {

    Faction find(Long factionId);

    /** 종류 선언 순서(성 → 종문), 같은 종류는 이름순 */
    List<Faction> findAll();

    /** 그 성·종문이 소유한 지역 수 */
    long countZones(Long factionId);
}
