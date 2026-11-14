package mudmk3.application.npc.provided;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import mudmk3.domain.npc.NpcSaveInfo;
import mudmk3.domain.shared.Names;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * NPC를 만들거나 고치는 요청. 이름·인사말·기본 대답은 앞뒤 공백을 자른 값으로 검증한다.
 * 낱말 목록이 없으면 빈 목록, 배치 방이 없으면 배치하지 않는다
 */
public record NpcSaveRequest(
        @NotBlank @Size(max = 12) @Pattern(regexp = "^\\S{1,12}$") String name,
        @NotBlank @Size(max = 500) String greeting,
        @NotBlank @Size(max = 500) String defaultAnswer,
        @Nullable @Size(max = 30) List<@NotNull @Valid NpcTopicRequest> topics,
        boolean llmEnabled,
        @Nullable Long roomId
) {
    public NpcSaveRequest {
        name = Names.strip(name);
        greeting = Names.strip(greeting);
        defaultAnswer = Names.strip(defaultAnswer);
    }

    public NpcSaveInfo toInfo() {
        List<NpcTopicRequest> requests = topics == null ? List.of() : topics;
        return new NpcSaveInfo(name, greeting, defaultAnswer,
                requests.stream().map(NpcTopicRequest::toTopic).toList(), llmEnabled);
    }
}
