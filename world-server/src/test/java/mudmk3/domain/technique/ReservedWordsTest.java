package mudmk3.domain.technique;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReservedWordsTest {

    @Test
    void contains() {
        assertThat(ReservedWords.contains("상태")).isTrue();
        assertThat(ReservedWords.contains("ㅅ")).isTrue();
        assertThat(ReservedWords.contains("참오")).isTrue();
        assertThat(ReservedWords.contains("e")).isTrue();
        assertThat(ReservedWords.contains("'")).isTrue();
    }

    @Test
    void directionInitialsAndLookShortcuts() {
        for (String word : new String[]{"ㄷ", "ㅅ", "ㄴ", "ㅂ", "ㅂㄷ", "ㅂㅅ", "ㄴㄷ", "ㄴㅅ", "ㅇ", "ㅇㄹ", "봐", "보", "ㅅㅌ"}) {
            assertThat(ReservedWords.contains(word)).as(word).isTrue();
        }
    }

    @Test
    void ignoresCase() {
        assertThat(ReservedWords.contains("E")).isTrue();
        assertThat(ReservedWords.contains("NE")).isTrue();
    }

    @Test
    void notReserved() {
        assertThat(ReservedWords.contains("화탄술")).isFalse();
        assertThat(ReservedWords.contains("상")).isFalse();
    }

    @Test
    void listMatchesPlan() {
        assertThat(ReservedWords.ALL).hasSize(116).doesNotHaveDuplicates();
    }

    @Test
    void attackReserved() {
        assertThat(ReservedWords.contains("공격")).isTrue();
        assertThat(ReservedWords.ALL.getLast()).isEqualTo("공격");
    }
}
