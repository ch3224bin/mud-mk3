package mudmk3.domain.item;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class ItemNamesTest {

    @Test
    void matches() {
        assertThat(ItemNames.matches("회춘단(연기 초)", "회춘단")).isTrue();
        assertThat(ItemNames.matches("늑대 가죽", "가죽")).isTrue();
        assertThat(ItemNames.matches("늑대 가죽", "늑대 가죽")).isTrue();
        assertThat(ItemNames.matches("늑대 가죽", "늑")).isTrue();
        assertThat(ItemNames.matches("늑대 가죽", "가")).isTrue();
        assertThat(ItemNames.matches("Jade Slip", "sl")).isTrue();
    }

    @Test
    void matchesFail() {
        assertThat(ItemNames.matches("늑대 가죽", "죽")).isFalse();
        assertThat(ItemNames.matches("늑대 가죽", "늑대 가죽 끈")).isFalse();
        assertThat(ItemNames.matches("늑대 가죽", "")).isFalse();
    }

    @Test
    void selectPrefersFullMatch() {
        List<String> items = List.of("늑대 가죽", "가죽", "가죽끈");

        assertThat(ItemNames.select(items, Function.identity(), ItemQuery.parse("가죽"))).contains("가죽");
    }

    @Test
    void selectFirstVisible() {
        List<String> items = List.of("회기단", "늑대 가죽", "곰 가죽");

        assertThat(ItemNames.select(items, Function.identity(), ItemQuery.parse("가죽"))).contains("늑대 가죽");
    }

    @Test
    void selectOrdinalCountsInOrder() {
        List<String> items = List.of("늑대 가죽", "가죽", "곰 가죽");

        assertThat(ItemNames.select(items, Function.identity(), ItemQuery.parse("1.가죽"))).contains("늑대 가죽");
        assertThat(ItemNames.select(items, Function.identity(), ItemQuery.parse("2.가죽"))).contains("가죽");
        assertThat(ItemNames.select(items, Function.identity(), ItemQuery.parse("3.가죽"))).contains("곰 가죽");
        assertThat(ItemNames.select(items, Function.identity(), ItemQuery.parse("4.가죽"))).isEmpty();
    }

    @Test
    void selectNone() {
        assertThat(ItemNames.select(List.of("늑대 가죽"), Function.identity(), ItemQuery.parse("죽"))).isEmpty();
    }

    @Test
    void selectSpiritStoneByGrade() {
        List<String> items = List.of("하품 영석", "중품 영석");

        assertThat(ItemNames.select(items, Function.identity(), ItemQuery.parse("중품"))).contains("중품 영석");
    }
}
