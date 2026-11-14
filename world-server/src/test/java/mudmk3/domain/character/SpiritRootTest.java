package mudmk3.domain.character;

import mudmk3.domain.shared.Element;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpiritRootTest {

    @Test
    void parseTwo() {
        SpiritRoot root = SpiritRoot.parse("화 목").orElseThrow();

        assertThat(root.name()).isEqualTo("목·화 진영근");
        assertThat(root.summary()).isEqualTo("목 50, 화 50");
        assertThat(root.entries()).extracting(SpiritRootEntry::element).containsExactly(Element.WOOD, Element.FIRE);
        assertThat(root.has(Element.WOOD)).isTrue();
        assertThat(root.has(Element.METAL)).isFalse();
        assertThat(root.aptitude(Element.FIRE)).isEqualTo(50.0);
        assertThat(root.aptitude(Element.METAL)).isZero();
    }

    @Test
    void parseOne() {
        SpiritRoot root = SpiritRoot.parse("화").orElseThrow();

        assertThat(root.name()).isEqualTo("화 천영근");
        assertThat(root.summary()).isEqualTo("화 100");
    }

    @Test
    void parseThree() {
        SpiritRoot root = SpiritRoot.parse("금 목 수").orElseThrow();

        assertThat(root.name()).isEqualTo("금·목·수 진영근");
        assertThat(root.summary()).isEqualTo("금 33, 목 33, 수 33");
        assertThat(root.aptitude(Element.METAL)).isEqualTo(100.0 / 3);
    }

    @Test
    void parseFourAndFive() {
        assertThat(SpiritRoot.parse("금 목 수 화").orElseThrow().name()).isEqualTo("금·목·수·화 위영근");
        SpiritRoot five = SpiritRoot.parse("토 화 수 목 금").orElseThrow();
        assertThat(five.name()).isEqualTo("금·목·수·화·토 위영근");
        assertThat(five.summary()).isEqualTo("금 20, 목 20, 수 20, 화 20, 토 20");
    }

    @Test
    void parseExtraSpaces() {
        assertThat(SpiritRoot.parse("  화   목 ").orElseThrow().name()).isEqualTo("목·화 진영근");
    }

    @Test
    void parseFail() {
        assertThat(SpiritRoot.parse("")).isEmpty();
        assertThat(SpiritRoot.parse("   ")).isEmpty();
        assertThat(SpiritRoot.parse("금 금")).isEmpty();
        assertThat(SpiritRoot.parse("불")).isEmpty();
        assertThat(SpiritRoot.parse("금,목")).isEmpty();
        assertThat(SpiritRoot.parse("금 목 수 화 토 금")).isEmpty();
    }

    @Test
    void ofSortsEntries() {
        SpiritRoot root = SpiritRoot.of(List.of(new SpiritRootEntry(Element.FIRE, 51.5), new SpiritRootEntry(Element.WOOD, 50)));

        assertThat(root.entries()).extracting(SpiritRootEntry::element).containsExactly(Element.WOOD, Element.FIRE);
        assertThat(root.summary()).isEqualTo("목 50, 화 51");
    }

    @Test
    void ofInvalidFail() {
        assertThatThrownBy(() -> SpiritRoot.of(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SpiritRoot.of(List.of(new SpiritRootEntry(Element.FIRE, 50), new SpiritRootEntry(Element.FIRE, 50))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SpiritRootEntry(Element.FIRE, -1)).isInstanceOf(IllegalArgumentException.class);
    }
}
