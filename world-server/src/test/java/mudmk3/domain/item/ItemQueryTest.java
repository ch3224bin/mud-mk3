package mudmk3.domain.item;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ItemQueryTest {

    @Test
    void parse() {
        assertThat(ItemQuery.parse("가죽")).isEqualTo(new ItemQuery("가죽", 0));
        assertThat(ItemQuery.parse("늑대 가죽")).isEqualTo(new ItemQuery("늑대 가죽", 0));
    }

    @Test
    void parseOrdinal() {
        assertThat(ItemQuery.parse("2.가죽")).isEqualTo(new ItemQuery("가죽", 2));
        assertThat(ItemQuery.parse("999.가죽")).isEqualTo(new ItemQuery("가죽", 999));
    }

    @Test
    void parseZeroOrdinal() {
        assertThat(ItemQuery.parse("0.가죽")).isEqualTo(new ItemQuery("0.가죽", 0));
        assertThat(ItemQuery.parse("1000.가죽")).isEqualTo(new ItemQuery("1000.가죽", 0));
        assertThat(ItemQuery.parse("2.")).isEqualTo(new ItemQuery("2.", 0));
    }

    @Test
    void parseSpiritStoneGrade() {
        assertThat(ItemQuery.parse("하품")).isEqualTo(new ItemQuery("하품 영석", 0));
        assertThat(ItemQuery.parse("2.극품")).isEqualTo(new ItemQuery("극품 영석", 2));
        assertThat(ItemQuery.parse("하품 영석")).isEqualTo(new ItemQuery("하품 영석", 0));
    }
}
