package mudmk3.domain.item;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ItemCategoryTest {

    @Test
    void table() {
        assertThat(ItemCategory.values())
                .extracting(ItemCategory::name, ItemCategory::koreanName, ItemCategory::stackSize,
                        ItemCategory::buyRatePercent, ItemCategory::creatableByAdmin)
                .containsExactly(
                        tuple("ARTIFACT", "법기", 1, 25, true),
                        tuple("PILL", "단약", 99, 25, true),
                        tuple("ELIXIR", "영약", 99, 25, true),
                        tuple("TALISMAN", "부적", 99, 25, true),
                        tuple("FORMATION_DISK", "진법원반", 1, 25, true),
                        tuple("SPIRIT_STONE", "영석", 10_000, 0, false),
                        tuple("MATERIAL", "재료", 99, 40, true),
                        tuple("SCROLL", "옥간·서책", 99, 25, true),
                        tuple("TOOL", "제작 도구", 1, 25, true),
                        tuple("BAG", "저물대", 0, 25, true),
                        tuple("BEAST_EGG", "영수 알·새끼", 1, 0, true),
                        tuple("BEAST_FOOD", "영수 먹이", 99, 25, true),
                        tuple("PUPPET", "괴뢰", 1, 25, true),
                        tuple("PASS", "출입권", 99, 25, true),
                        tuple("QUEST", "퀘스트 물건", 1, 0, true));
    }
}
