package mudmk3.domain.npc;

import mudmk3.domain.item.ItemCategory;
import org.junit.jupiter.api.Test;

import static mudmk3.domain.item.ItemCategory.ARTIFACT;
import static mudmk3.domain.item.ItemCategory.BAG;
import static mudmk3.domain.item.ItemCategory.BEAST_EGG;
import static mudmk3.domain.item.ItemCategory.BEAST_FOOD;
import static mudmk3.domain.item.ItemCategory.ELIXIR;
import static mudmk3.domain.item.ItemCategory.FORMATION_DISK;
import static mudmk3.domain.item.ItemCategory.MATERIAL;
import static mudmk3.domain.item.ItemCategory.PASS;
import static mudmk3.domain.item.ItemCategory.PILL;
import static mudmk3.domain.item.ItemCategory.PUPPET;
import static mudmk3.domain.item.ItemCategory.QUEST;
import static mudmk3.domain.item.ItemCategory.SCROLL;
import static mudmk3.domain.item.ItemCategory.SPIRIT_STONE;
import static mudmk3.domain.item.ItemCategory.TALISMAN;
import static mudmk3.domain.item.ItemCategory.TOOL;
import static org.assertj.core.api.Assertions.assertThat;

class ShopKindTest {

    @Test
    void table() {
        assertThat(ShopKind.values()).extracting(ShopKind::name).containsExactly(
                "PILL_SHOP", "ARTIFACT_SHOP", "BOOK_SHOP", "TALISMAN_SHOP", "MATERIAL_SHOP", "GENERAL_STORE", "BEAST_SHOP");
        assertThat(ShopKind.values()).extracting(ShopKind::koreanName).containsExactly(
                "단약방", "법기점", "서방", "부적방", "재료상", "잡화점", "영수점");

        assertThat(ShopKind.PILL_SHOP.buyCategories()).containsExactly(PILL, ELIXIR);
        assertThat(ShopKind.ARTIFACT_SHOP.buyCategories()).containsExactly(ARTIFACT, PUPPET, TOOL);
        assertThat(ShopKind.BOOK_SHOP.buyCategories()).containsExactly(SCROLL);
        assertThat(ShopKind.TALISMAN_SHOP.buyCategories()).containsExactly(TALISMAN, FORMATION_DISK);
        assertThat(ShopKind.MATERIAL_SHOP.buyCategories()).containsExactly(MATERIAL);
        assertThat(ShopKind.GENERAL_STORE.buyCategories()).containsExactly(BAG, BEAST_FOOD, PASS);
        assertThat(ShopKind.BEAST_SHOP.buyCategories()).isEmpty();
    }

    @Test
    void sells() {
        assertThat(ShopKind.PILL_SHOP.sells(PILL)).isTrue();
        assertThat(ShopKind.PILL_SHOP.sells(ELIXIR)).isFalse();
        assertThat(ShopKind.ARTIFACT_SHOP.sells(PUPPET)).isTrue();
        assertThat(ShopKind.ARTIFACT_SHOP.sells(TOOL)).isTrue();
        assertThat(ShopKind.TALISMAN_SHOP.sells(FORMATION_DISK)).isFalse();
        assertThat(ShopKind.GENERAL_STORE.sells(BAG)).isTrue();
        assertThat(ShopKind.BEAST_SHOP.sells(BEAST_EGG)).isTrue();
        assertThat(ShopKind.BEAST_SHOP.sells(PILL)).isTrue();
        for (ShopKind kind : ShopKind.values()) {
            assertThat(kind.sells(SPIRIT_STONE)).isFalse();
            assertThat(kind.sells(QUEST)).isFalse();
        }
    }

    @Test
    void buys() {
        assertThat(ShopKind.PILL_SHOP.buys(ELIXIR)).isTrue();
        assertThat(ShopKind.PILL_SHOP.buys(MATERIAL)).isFalse();
        assertThat(ShopKind.TALISMAN_SHOP.buys(FORMATION_DISK)).isTrue();
        assertThat(ShopKind.BEAST_SHOP.buys(PILL)).isFalse();
    }

    @Test
    void buyerOf() {
        assertThat(ShopKind.buyerOf(MATERIAL)).contains(ShopKind.MATERIAL_SHOP);
        assertThat(ShopKind.buyerOf(PILL)).contains(ShopKind.PILL_SHOP);
        assertThat(ShopKind.buyerOf(BEAST_EGG)).isEmpty();
        assertThat(ShopKind.buyerOf(SPIRIT_STONE)).isEmpty();
        assertThat(ShopKind.buyerOf(QUEST)).isEmpty();
        for (ItemCategory category : ItemCategory.values()) {
            // 사 주는 상점이 있는 종류는 매입 비율이 있다
            assertThat(ShopKind.buyerOf(category).isPresent()).isEqualTo(category.buyRatePercent() > 0);
        }
    }
}
