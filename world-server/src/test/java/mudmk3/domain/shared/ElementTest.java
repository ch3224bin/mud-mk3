package mudmk3.domain.shared;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ElementTest {

    @Test
    void order() {
        assertThat(Element.values())
                .containsExactly(Element.METAL, Element.WOOD, Element.WATER, Element.FIRE, Element.EARTH);
        assertThat(Element.values()).extracting(Element::koreanName).containsExactly("금", "목", "수", "화", "토");
    }

    @Test
    void fromKorean() {
        assertThat(Element.fromKorean("목")).contains(Element.WOOD);
        assertThat(Element.fromKorean("토")).contains(Element.EARTH);
        assertThat(Element.fromKorean("불")).isEmpty();
        assertThat(Element.fromKorean("")).isEmpty();
    }
}
