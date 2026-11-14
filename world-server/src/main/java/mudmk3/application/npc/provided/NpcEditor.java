package mudmk3.application.npc.provided;

import jakarta.validation.Valid;
import mudmk3.domain.npc.Npc;

/** NPC를 만들고 고치고 배치하고 지우며, 상점 역할을 정한다 */
public interface NpcEditor {

    Npc create(@Valid NpcSaveRequest request);

    /** 낱말 목록은 통째로 바꾸고, 배치는 요청의 roomId로 정한다 */
    Npc update(Long npcId, @Valid NpcSaveRequest request);

    void delete(Long npcId);

    /** 상점 역할을 정한다(있던 상점은 통째로 바뀐다). 없는 물건 종류면 {@code IllegalArgumentException} */
    Npc openShop(Long npcId, @Valid ShopSaveRequest request);

    /** 상점 역할을 없앤다. 상점이 아니어도 된다 */
    void closeShop(Long npcId);
}
