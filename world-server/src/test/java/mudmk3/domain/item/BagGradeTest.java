package mudmk3.domain.item;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class BagGradeTest {

    @Test
    void table() {
        assertThat(BagGrade.values())
                .extracting(BagGrade::name, BagGrade::koreanName, BagGrade::displayName, BagGrade::slots,
                        BagGrade::isBracelet)
                .containsExactly(
                        tuple("LOW", "하급", "하급 저물대", 20, false),
                        tuple("MIDDLE", "중급", "중급 저물대", 40, false),
                        tuple("HIGH", "상급", "상급 저물대", 80, false),
                        tuple("BRACELET_LOW", "하급", "하급 저물탁", 100, true),
                        tuple("BRACELET_MIDDLE", "중급", "중급 저물탁", 200, true),
                        tuple("BRACELET_HIGH", "상급", "상급 저물탁", 400, true));
    }

    @Test
    void braceletItemType() {
        ItemType bracelet = ItemTypeFixture.createBag(BagGrade.BRACELET_HIGH);
        ItemType bag = ItemTypeFixture.createBag(BagGrade.HIGH);

        assertThat(bracelet.getName()).isEqualTo("상급 저물탁");
        assertThat(bracelet.isBracelet()).isTrue();
        assertThat(bracelet.bagSlots()).isEqualTo(400);
        assertThat(bag.isBracelet()).isFalse();
        assertThat(ItemTypeFixture.createPill().isBracelet()).isFalse();
    }
}
