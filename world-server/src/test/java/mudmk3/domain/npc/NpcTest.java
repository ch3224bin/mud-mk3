package mudmk3.domain.npc;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NpcTest {

    @Test
    void create() {
        Npc npc = Npc.create(new NpcSaveInfo(" 왕씨  ", "  어서 오시오. ", " 글쎄, 잘 모르겠구려. ",
                List.of(new NpcTopic("단약", "단약은 귀한 물건이오.")), false));

        assertThat(npc.getName()).isEqualTo("왕씨");
        assertThat(npc.getGreeting()).isEqualTo("어서 오시오.");
        assertThat(npc.getDefaultAnswer()).isEqualTo("글쎄, 잘 모르겠구려.");
        assertThat(npc.getTopics()).containsExactly(new NpcTopic("단약", "단약은 귀한 물건이오."));
        assertThat(npc.isLlmEnabled()).isFalse();
        assertThat(npc.findRoomId()).isEmpty();
        assertThat(NpcFixture.createNpc().isLlmEnabled()).isFalse();
    }

    @Test
    void nameWithSpaceFail() {
        assertThatThrownBy(() -> Npc.create(NpcFixture.createNpcSaveInfo("왕 씨")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Npc.create(NpcFixture.createNpcSaveInfo("가나다라마바사아자차카타파")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void topicBracketsStripped() {
        NpcTopic topic = new NpcTopic(" [단약] ", "단약은 귀한 물건이오.");

        assertThat(topic.word()).isEqualTo("단약");
    }

    @Test
    void topicAnswerRule() {
        assertThatThrownBy(() -> new NpcTopic("단약", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("대답은 1~1000자로 정하십시오: 단약");
        assertThatThrownBy(() -> new NpcTopic("단약", "가".repeat(1001)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NpcTopic("가".repeat(21), "대답"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void duplicateTopicFail() {
        assertThatThrownBy(() -> Npc.create(NpcFixture.createNpcSaveInfo("왕씨", List.of(
                new NpcTopic("단약", "가"), new NpcTopic("단약", "나")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("같은 낱말이 두 번 있습니다: 단약");
        assertThatThrownBy(() -> Npc.create(NpcFixture.createNpcSaveInfo("왕씨", List.of(
                new NpcTopic("Herb", "가"), new NpcTopic("herb", "나")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("같은 낱말이 두 번 있습니다: herb");
    }

    @Test
    void topicWithSpaceFail() {
        assertThatThrownBy(() -> new NpcTopic("늑대 퇴치", "숲의 늑대를 잡아 주시오."))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("낱말은 띄어쓰기 없이 1~20자로 정하십시오: 늑대 퇴치");
    }

    @Test
    void answerToKnownWord() {
        Npc npc = NpcFixture.createNpc();

        assertThat(npc.answerTo("일거리")).isEqualTo("숲의 늑대를 잡아 주시오.");
        assertThat(npc.findTopic("일거리")).isPresent();
    }

    @Test
    void answerToUnknownWordIsDefault() {
        Npc npc = NpcFixture.createNpc();

        assertThat(npc.answerTo("영약")).isEqualTo("글쎄, 잘 모르겠구려.");
        assertThat(npc.findTopic("영약")).isEmpty();
    }

    @Test
    void answerIgnoresBracketsAndLatinCase() {
        Npc npc = Npc.create(NpcFixture.createNpcSaveInfo("왕씨", List.of(
                new NpcTopic("단약", "단약은 귀한 물건이오."), new NpcTopic("Herb", "약초요."))));

        assertThat(npc.answerTo("[단약]")).isEqualTo("단약은 귀한 물건이오.");
        assertThat(npc.answerTo("HERB")).isEqualTo("약초요.");
    }

    @Test
    void noPrefixMatchForWord() {
        Npc npc = NpcFixture.createNpc();

        assertThat(npc.answerTo("단")).isEqualTo("글쎄, 잘 모르겠구려.");
    }

    @Test
    void placeAndUnplace() {
        Npc npc = NpcFixture.createNpc();

        npc.placeIn(3L);
        assertThat(npc.findRoomId()).contains(3L);

        npc.unplace();
        assertThat(npc.findRoomId()).isEmpty();
    }

    @Test
    void updateReplacesTopics() {
        Npc npc = NpcFixture.createNpc();

        npc.update(new NpcSaveInfo("왕노인", "허허.", "모르겠네.", List.of(
                new NpcTopic("일거리", "없소."), new NpcTopic("단약", "비싸오."), new NpcTopic("길", "북쪽이오.")), true));

        assertThat(npc.getName()).isEqualTo("왕노인");
        assertThat(npc.getGreeting()).isEqualTo("허허.");
        assertThat(npc.getDefaultAnswer()).isEqualTo("모르겠네.");
        assertThat(npc.isLlmEnabled()).isTrue();
        assertThat(npc.getTopics()).extracting(NpcTopic::word).containsExactly("일거리", "단약", "길");
    }

    @Test
    void topicsUnmodifiable() {
        Npc npc = NpcFixture.createNpc();

        assertThatThrownBy(() -> npc.getTopics().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void nameMatching() {
        Npc npc = NpcFixture.createNpc("왕씨");

        assertThat(npc.isCalled("왕씨")).isTrue();
        assertThat(npc.nameStartsWith("왕")).isTrue();
        assertThat(npc.isCalled("왕")).isFalse();
        assertThat(npc.nameStartsWith("씨")).isFalse();

        Npc latin = NpcFixture.createNpc("Wang");
        assertThat(latin.isCalled("wANG")).isTrue();
        assertThat(latin.nameStartsWith("wa")).isTrue();
    }
}
