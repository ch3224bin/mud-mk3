package mudmk3.domain.shared;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NamesTest {

    @Test
    void strip() {
        assertThat(Names.strip("  청석성 ")).isEqualTo("청석성");
        assertThat(Names.strip(null)).isNull();
    }

    @Test
    void stripBrackets() {
        assertThat(Names.stripBrackets("[단약]")).isEqualTo("단약");
        assertThat(Names.stripBrackets("  [단약] ")).isEqualTo("단약");
        assertThat(Names.stripBrackets("단약")).isEqualTo("단약");
        assertThat(Names.stripBrackets("[단약")).isEqualTo("단약");
        assertThat(Names.stripBrackets("[ 단약 ]")).isEqualTo("단약");
        assertThat(Names.stripBrackets("[]")).isEmpty();
    }
}
