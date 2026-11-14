package mudmk3.adapter.webapi.dto;

import mudmk3.domain.npc.Npc;
import mudmk3.domain.room.Room;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/** NPC 하나의 자세한 정보. topics 는 입력 순서, 배치되지 않았으면 방·지역 네 값 모두 null */
public record NpcDetailResponse(
        Long id,
        String name,
        String greeting,
        String defaultAnswer,
        List<NpcTopicResponse> topics,
        boolean llmEnabled,
        @Nullable Long roomId,
        @Nullable String roomName,
        @Nullable Long zoneId,
        @Nullable String zoneName
) {

    public static NpcDetailResponse of(Npc npc, Optional<Room> placedRoom) {
        Room room = placedRoom.orElse(null);
        return new NpcDetailResponse(npc.getId(), npc.getName(), npc.getGreeting(), npc.getDefaultAnswer(),
                npc.getTopics().stream().map(NpcTopicResponse::of).toList(), npc.isLlmEnabled(),
                room == null ? null : room.getId(),
                room == null ? null : room.getName(),
                room == null ? null : room.getZone().getId(),
                room == null ? null : room.getZone().getName());
    }
}
