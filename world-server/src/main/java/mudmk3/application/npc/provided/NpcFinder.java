package mudmk3.application.npc.provided;

import mudmk3.domain.npc.Npc;

import java.util.List;

/** NPC를 찾는다. 돌려주는 NPC는 낱말 대답까지 읽혀 있다 */
public interface NpcFinder {

    Npc find(Long npcId);

    /** 이름순 */
    List<Npc> findAll();

    /** 그 방에 배치된 NPC(만든 순서 = id 순) */
    List<Npc> findAllInRoom(Long roomId);

    /** 그 이름의 NPC가 있는가(영문 대소문자 무시) */
    boolean existsByName(String name);
}
