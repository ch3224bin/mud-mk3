package mudmk3.domain.item;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PricingTest {

    @Test
    void defaultSellPrice() {
        assertThat(Pricing.defaultSellPrice(ItemTypeFixture.createPill())).isEqualTo(3);
    }

    @Test
    void pillBuyPrice() {
        ItemType pill = ItemTypeFixture.createPill();

        assertThat(Pricing.unitBuyPrice(pill)).isEqualByComparingTo(new BigDecimal("0.75"));
        assertThat(Pricing.buyTotal(pill, 3)).isEqualTo(2);
        assertThat(Pricing.buyTotal(pill, 1)).isZero();
    }

    @Test
    void materialBuyPrice() {
        ItemType hide = ItemTypeFixture.createWolfHide();

        assertThat(Pricing.unitBuyPrice(hide)).isEqualByComparingTo(new BigDecimal("0.8"));
        assertThat(Pricing.buyTotal(hide, 10)).isEqualTo(8);
    }

    @Test
    void notBoughtCategory() {
        ItemType quest = ItemTypeFixture.createItemType("붉은 편지", ItemCategory.QUEST, 10);

        assertThat(Pricing.unitBuyPrice(quest)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(Pricing.buyTotal(quest, 5)).isZero();
        assertThat(Pricing.isFairSellPrice(quest, 1)).isTrue();
    }

    @Test
    void isFairSellPrice() {
        ItemType pill = ItemTypeFixture.createPill();

        assertThat(Pricing.isFairSellPrice(pill, 1)).isFalse();
        assertThat(Pricing.isFairSellPrice(pill, 2)).isTrue();
    }

    @Test
    void isFairSellPriceBoundary() {
        // 기준가 2, 재료 40%: 매입가 0.8. 판매가 1.6 은 정확히 2배라 안 되고, 2 는 된다
        ItemType hide = ItemTypeFixture.createWolfHide();

        assertThat(Pricing.isFairSellPrice(hide, 1)).isFalse();
        assertThat(Pricing.isFairSellPrice(hide, 2)).isTrue();
    }

    @Test
    void isFairSellPriceByCategoryAndBaseValue() {
        // 기준가·판매가가 상한(100,000,000)이어도 넘치지 않는다
        assertThat(Pricing.isFairSellPrice(ItemCategory.MATERIAL, 100_000_000, 100_000_000)).isTrue();
        assertThat(Pricing.isFairSellPrice(ItemCategory.MATERIAL, 100_000_000, 80_000_000)).isFalse();
        assertThat(Pricing.isFairSellPrice(ItemCategory.QUEST, 100_000_000, 1)).isTrue();
    }

    @Test
    void isFairSellPriceOverflowFail() {
        assertThatThrownBy(() -> assertThat(Pricing.isFairSellPrice(ItemCategory.PILL, 1, Long.MAX_VALUE / 10)).isTrue())
                .isInstanceOf(ArithmeticException.class);
    }
}
