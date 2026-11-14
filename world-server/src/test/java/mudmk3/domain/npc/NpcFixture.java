package mudmk3.domain.npc;

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
}
