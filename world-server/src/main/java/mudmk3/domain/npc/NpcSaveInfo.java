package mudmk3.domain.npc;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import mudmk3.domain.shared.Names;

import java.util.List;

/** NPC를 만들거나 고칠 때 받는 값. 이름·인사말·기본 대답은 앞뒤 공백을 자른 값으로 검증한다 */
public record NpcSaveInfo(
        @NotBlank @Pattern(regexp = "^\\S{1,12}$") String name,
        @NotBlank @Size(max = 500) String greeting,
        @NotBlank @Size(max = 500) String defaultAnswer,
        @Size(max = 30) List<NpcTopic> topics,
        boolean llmEnabled
) {
    public NpcSaveInfo {
        name = Names.strip(name);
        greeting = Names.strip(greeting);
        defaultAnswer = Names.strip(defaultAnswer);
    }
}
