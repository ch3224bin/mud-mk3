package mudmk3.application.npc.provided;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import mudmk3.domain.npc.NpcTopic;

/** NPC 낱말 대답 요청. 낱말은 앞뒤 [ ] 를 붙여 와도 되므로 길이에 여유를 둔다. 다듬기와 규칙 검사는 {@link NpcTopic} 이 한다 */
public record NpcTopicRequest(
        @NotBlank @Size(max = 22) String word,
        @NotBlank @Size(max = NpcTopic.ANSWER_MAX_LENGTH) String answer
) {

    public NpcTopic toTopic() {
        return new NpcTopic(word, answer);
    }
}
