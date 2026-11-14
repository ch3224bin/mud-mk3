package mudmk3.domain.npc;

import mudmk3.application.npc.provided.NpcSaveRequest;
import mudmk3.application.npc.provided.NpcTopicRequest;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class NpcFixture {

    public static NpcSaveInfo createNpcSaveInfo(String name, List<NpcTopic> topics) {
        return new NpcSaveInfo(name, "어서 오시오.", "글쎄, 잘 모르겠구려.", topics, false);
    }

    public static NpcSaveInfo createNpcSaveInfo(String name) {
        return createNpcSaveInfo(name, List.of(
                new NpcTopic("단약", "단약은 귀한 물건이오."),
                new NpcTopic("일거리", "숲의 늑대를 잡아 주시오.")));
    }

    public static Npc createNpc() {
        return createNpc("왕씨");
    }

    public static Npc createNpc(String name) {
        return Npc.create(createNpcSaveInfo(name));
    }

    public static NpcSaveRequest createNpcSaveRequest(String name, @Nullable Long roomId) {
        return new NpcSaveRequest(name, "어서 오시오.", "글쎄, 잘 모르겠구려.", List.of(
                new NpcTopicRequest("단약", "단약은 귀한 물건이오."),
                new NpcTopicRequest("일거리", "숲의 늑대를 잡아 주시오.")), false, roomId);
    }
}
