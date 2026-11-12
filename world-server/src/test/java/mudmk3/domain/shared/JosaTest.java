package mudmk3.domain.shared;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JosaTest {

    @Test
    void withIGa() {
        assertThat(Josa.withIGa("한백")).isEqualTo("한백이");
        assertThat(Josa.withIGa("여소")).isEqualTo("여소가");
        assertThat(Josa.withIGa("한립")).isEqualTo("한립이");
        assertThat(Josa.withIGa("청모랑")).isEqualTo("청모랑이");
        assertThat(Josa.withIGa("소나무")).isEqualTo("소나무가");
    }

    @Test
    void iGa() {
        assertThat(Josa.iGa("한백")).isEqualTo("이");
        assertThat(Josa.iGa("여소")).isEqualTo("가");
    }

    @Test
    void iGaNonHangulEnding() {
        assertThat(Josa.iGa("abc")).isEqualTo("가");
        assertThat(Josa.iGa("")).isEqualTo("가");
    }
}
