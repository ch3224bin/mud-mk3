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

    @Test
    void withGwaWa() {
        assertThat(Josa.withGwaWa("비검")).isEqualTo("비검과");
        assertThat(Josa.withGwaWa("단약")).isEqualTo("단약과");
        assertThat(Josa.withGwaWa("부적")).isEqualTo("부적과");
        assertThat(Josa.withGwaWa("영수 먹이")).isEqualTo("영수 먹이와");
        assertThat(Josa.gwaWa("비검")).isEqualTo("과");
    }

    @Test
    void withEunNeun() {
        assertThat(Josa.withEunNeun("회춘단")).isEqualTo("회춘단은");
        assertThat(Josa.withEunNeun("회춘단123456")).isEqualTo("회춘단123456는");
        assertThat(Josa.withEunNeun("회춘단(연기 초)")).isEqualTo("회춘단(연기 초)는");
        assertThat(Josa.withEunNeun("늑대 가죽")).isEqualTo("늑대 가죽은");
        assertThat(Josa.withEunNeun("단약방")).isEqualTo("단약방은");
        assertThat(Josa.eunNeun("한립")).isEqualTo("은");
    }

    @Test
    void withEulReul() {
        assertThat(Josa.withEulReul("법기")).isEqualTo("법기를");
        assertThat(Josa.withEulReul("회춘단")).isEqualTo("회춘단을");
        assertThat(Josa.withEulReul("늑대 가죽(상)")).isEqualTo("늑대 가죽(상)을");
        assertThat(Josa.eulReul("법기")).isEqualTo("를");
    }

    @Test
    void skipsTrailingPunctuation() {
        assertThat(Josa.withIGa("회춘단(연기 초)")).isEqualTo("회춘단(연기 초)가");
        assertThat(Josa.withIGa("회기단(연기 중)")).isEqualTo("회기단(연기 중)이");
        assertThat(Josa.withIGa("abc")).isEqualTo("abc가");
        assertThat(Josa.iGa(")")).isEqualTo("가");
    }
}
