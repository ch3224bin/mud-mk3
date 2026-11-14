package mudmk3.domain.item;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlotsTest {

    @Test
    void slotsFor() {
        ItemType pill = ItemTypeFixture.createPill();

        assertThat(Slots.slotsFor(pill, 1)).isEqualTo(1);
        assertThat(Slots.slotsFor(pill, 99)).isEqualTo(1);
        assertThat(Slots.slotsFor(pill, 100)).isEqualTo(2);
        assertThat(Slots.slotsFor(pill, 0)).isZero();
    }

    @Test
    void slotsForSpiritStone() {
        ItemType stone = ItemType.spiritStone(SpiritStoneGrade.LOW);

        assertThat(Slots.slotsFor(stone, 10_000)).isEqualTo(1);
        assertThat(Slots.slotsFor(stone, 10_001)).isEqualTo(2);
    }

    @Test
    void slotsForBag() {
        assertThat(Slots.slotsFor(ItemTypeFixture.createBag(BagGrade.LOW), 3)).isZero();
    }

    @Test
    void slotsForSingleStack() {
        ItemType sword = ItemTypeFixture.createItemType("비검", ItemCategory.ARTIFACT, 50);

        assertThat(Slots.slotsFor(sword, 3)).isEqualTo(3);
    }

    @Test
    void capacity() {
        assertThat(Slots.BASE).isEqualTo(2);
        assertThat(Slots.MAX_BAGS).isEqualTo(3);
        assertThat(Slots.capacity(0)).isEqualTo(2);
        assertThat(Slots.capacity(20)).isEqualTo(22);
    }
}
