package mudmk3.adapter.webapi.dto;

import mudmk3.domain.npc.NpcTopic;

public record NpcTopicResponse(String word, String answer) {

    public static NpcTopicResponse of(NpcTopic topic) {
        return new NpcTopicResponse(topic.word(), topic.answer());
    }
}
