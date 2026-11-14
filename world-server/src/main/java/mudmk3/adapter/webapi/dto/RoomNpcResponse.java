package mudmk3.adapter.webapi.dto;

import mudmk3.domain.npc.Npc;

/** 방에 배치된 NPC */
public record RoomNpcResponse(Long id, String name) {

    public static RoomNpcResponse of(Npc npc) {
        return new RoomNpcResponse(npc.getId(), npc.getName());
    }
}
