package mudmk3.adapter.webapi.dto;

import mudmk3.domain.npc.Npc;
import mudmk3.domain.npc.ShopKind;
import mudmk3.domain.room.Room;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/** NPC 목록의 한 줄. 배치되지 않았으면 방 세 값 모두 null, 상점이 아니면 shopKindName 이 null */
public record NpcSummaryResponse(
        Long id,
        String name,
        boolean llmEnabled,
        int topicCount,
        @Nullable Long roomId,
        @Nullable String roomName,
        @Nullable String zoneName,
        @Nullable String shopKindName
) {

    public static NpcSummaryResponse of(Npc npc, Optional<Room> placedRoom) {
        Room room = placedRoom.orElse(null);
        return new NpcSummaryResponse(npc.getId(), npc.getName(), npc.isLlmEnabled(), npc.getTopics().size(),
                room == null ? null : room.getId(),
                room == null ? null : room.getName(),
                room == null ? null : room.getZone().getName(),
                npc.findShopKind().map(ShopKind::koreanName).orElse(null));
    }
}
