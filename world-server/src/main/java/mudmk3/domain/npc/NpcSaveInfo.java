package mudmk3.domain.npc;

import mudmk3.domain.shared.Names;

import java.util.List;

/** NPC를 만들거나 고칠 때 받는 값. 이름·인사말·기본 대답은 앞뒤 공백을 자른다. 규칙 검사는 {@link Npc} 가 한다 */
public record NpcSaveInfo(
        String name,
        String greeting,
        String defaultAnswer,
        List<NpcTopic> topics,
        boolean llmEnabled
) {
    public NpcSaveInfo {
        name = Names.strip(name);
        greeting = Names.strip(greeting);
        defaultAnswer = Names.strip(defaultAnswer);
    }
}
