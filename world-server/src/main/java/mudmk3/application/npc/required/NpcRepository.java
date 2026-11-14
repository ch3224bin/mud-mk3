package mudmk3.application.npc.required;

import mudmk3.domain.npc.Npc;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * NPC(낱말 대답·상점 물건 포함)를 저장하고 찾는다. 낱말·상점 물건은 트랜잭션 밖에서도 읽을 수 있게 함께 읽는다
 * (둘 다 순서 열이 있는 리스트라 여러 bag 문제가 없다)
 */
public interface NpcRepository extends Repository<Npc, Long> {

    Npc save(Npc npc);

    @EntityGraph(attributePaths = {"topics", "shopItems"})
    Optional<Npc> findById(Long id);

    @EntityGraph(attributePaths = {"topics", "shopItems"})
    List<Npc> findAllByOrderByNameAsc();

    /** 그 방에 배치된 NPC(만든 순서 = id 순) */
    @EntityGraph(attributePaths = {"topics", "shopItems"})
    List<Npc> findAllByRoomIdOrderByIdAsc(Long roomId);

    /** 영문 대소문자를 가리지 않고 같은 이름이 있는지 */
    boolean existsByNameIgnoreCase(String name);

    /** 영문 대소문자를 가리지 않고 다른 NPC가 같은 이름을 쓰는지 */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    void delete(Npc npc);
}
