package mudmk3.application.npc.provided;

import jakarta.validation.Valid;
import mudmk3.domain.npc.Npc;

/** NPC를 만들고 고치고 배치하고 지운다 */
public interface NpcEditor {

    Npc create(@Valid NpcSaveRequest request);

    /** 낱말 목록은 통째로 바꾸고, 배치는 요청의 roomId로 정한다 */
    Npc update(Long npcId, @Valid NpcSaveRequest request);

    void delete(Long npcId);
}
