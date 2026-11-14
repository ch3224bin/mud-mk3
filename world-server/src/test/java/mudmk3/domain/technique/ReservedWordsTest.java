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
        assertThat(ReservedWords.ALL).hasSize(104).doesNotHaveDuplicates();
    }
}
