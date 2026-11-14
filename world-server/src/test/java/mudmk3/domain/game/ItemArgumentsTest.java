package mudmk3.domain.game;

import mudmk3.domain.game.Amount.All;
import mudmk3.domain.game.Amount.Count;
import mudmk3.domain.game.Amount.Invalid;
import mudmk3.domain.item.ItemQuery;
import org.junit.jupiter.api.Test;

import static mudmk3.domain.game.ItemArguments.parse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemArgumentsTest {

    @Test
    void nameAndCount() {
        assertThat(parse("늑대 가죽 3")).isEqualTo(new ItemArguments(new ItemQuery("늑대 가죽", 0), new Count(3)));
        assertThat(parse("늑대 가죽")).isEqualTo(new ItemArguments(new ItemQuery("늑대 가죽", 0), Amount.one()));
        assertThat(parse("  늑대   가죽  ")).isEqualTo(new ItemArguments(new ItemQuery("늑대 가죽", 0), Amount.one()));
        assertThat(parse("가죽 999999999")).isEqualTo(new ItemArguments(new ItemQuery("가죽", 0), new Count(999_999_999)));
        assertThat(parse("가죽 007")).isEqualTo(new ItemArguments(new ItemQuery("가죽", 0), new Count(7)));
    }

    @Test
    void all() {
        assertThat(parse("가죽 전부")).isEqualTo(new ItemArguments(new ItemQuery("가죽", 0), new All()));
        assertThat(parse("전부")).isEqualTo(new ItemArguments(null, new All()));
    }

    @Test
    void spiritStoneWord() {
        assertThat(parse("하품 10")).isEqualTo(new ItemArguments(new ItemQuery("하품 영석", 0), new Count(10)));
    }

    @Test
    void ordinal() {
        assertThat(parse("2.가죽")).isEqualTo(new ItemArguments(new ItemQuery("가죽", 2), Amount.one()));
        assertThat(parse("2.가죽 전부")).isEqualTo(new ItemArguments(new ItemQuery("가죽", 2), new All()));
    }

    @Test
    void invalidCount() {
        assertThat(parse("가죽 0")).isEqualTo(new ItemArguments(new ItemQuery("가죽", 0), new Invalid()));
        assertThat(parse("가죽 1234567890")).isEqualTo(new ItemArguments(new ItemQuery("가죽", 0), new Invalid()));
        assertThat(parse("가죽 99999999999999999999")).isEqualTo(new ItemArguments(new ItemQuery("가죽", 0), new Invalid()));
    }

    @Test
    void noName() {
        assertThat(parse("3")).isEqualTo(new ItemArguments(null, new Count(3)));
        assertThat(parse("")).isEqualTo(new ItemArguments(null, Amount.one()));
        assertThat(parse("   ")).isEqualTo(new ItemArguments(null, Amount.one()));
        assertThat(parse("0")).isEqualTo(new ItemArguments(null, new Invalid()));
    }

    @Test
    void countMustBePositiveFail() {
        assertThatThrownBy(() -> new Count(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Count(1_000_000_000)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void hasAmount() {
        assertThat(ItemArguments.hasAmount("회춘단 3")).isTrue();
        assertThat(ItemArguments.hasAmount("늑대 가죽 전부")).isTrue();
        assertThat(ItemArguments.hasAmount("회춘단 0")).isTrue();
        assertThat(ItemArguments.hasAmount("늑대 가죽")).isFalse();
        assertThat(ItemArguments.hasAmount("회춘단")).isFalse();
        assertThat(ItemArguments.hasAmount("")).isFalse();
    }
}
