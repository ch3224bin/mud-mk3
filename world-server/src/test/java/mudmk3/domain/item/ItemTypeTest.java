package mudmk3.domain.item;

import org.junit.jupiter.api.Test;

import static mudmk3.domain.item.ItemTypeFixture.createBag;
import static mudmk3.domain.item.ItemTypeFixture.createItemTypeSaveInfo;
import static mudmk3.domain.item.ItemTypeFixture.createPill;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemTypeTest {

    @Test
    void create() {
        ItemType itemType = ItemType.create(new ItemTypeSaveInfo("  늑대   가죽 ", null, ItemCategory.MATERIAL, 2, true, null));

        assertThat(itemType.getName()).isEqualTo("늑대 가죽");
        assertThat(itemType.getDescription()).isEmpty();
        assertThat(itemType.getCategory()).isEqualTo(ItemCategory.MATERIAL);
        assertThat(itemType.getBaseValue()).isEqualTo(2);
        assertThat(itemType.isTradable()).isTrue();
        assertThat(itemType.findBagGrade()).isEmpty();
        assertThat(itemType.findSpiritStoneGrade()).isEmpty();
        assertThat(itemType.stackSize()).isEqualTo(99);
        assertThat(itemType.bagSlots()).isZero();
        assertThat(itemType.isBag()).isFalse();
        assertThat(itemType.isSpiritStone()).isFalse();
        assertThat(itemType.isQuest()).isFalse();
        assertThat(itemType.isBindable()).isFalse();
    }

    @Test
    void createWithDescription() {
        ItemType itemType = ItemType.create(new ItemTypeSaveInfo("비검", "날이 선 작은 검이다.", ItemCategory.ARTIFACT, 50, false, null));

        assertThat(itemType.getDescription()).isEqualTo("날이 선 작은 검이다.");
        assertThat(itemType.isTradable()).isFalse();
        assertThat(itemType.isBindable()).isTrue();
        assertThat(itemType.stackSize()).isEqualTo(1);
    }

    @Test
    void createBagItem() {
        ItemType bag = createBag(BagGrade.MIDDLE);

        assertThat(bag.isBag()).isTrue();
        assertThat(bag.findBagGrade()).contains(BagGrade.MIDDLE);
        assertThat(bag.bagSlots()).isEqualTo(40);
        assertThat(bag.stackSize()).isZero();
        assertThat(bag.isBindable()).isTrue();
    }

    @Test
    void quest() {
        assertThat(ItemTypeFixture.createItemType("붉은 편지", ItemCategory.QUEST, 0).isQuest()).isTrue();
    }

    @Test
    void update() {
        ItemType itemType = createPill();

        itemType.update(new ItemTypeSaveInfo(" 회기단  (연기 초) ", "푸른 단약이다.", ItemCategory.PILL, 4, false, null));

        assertThat(itemType.getName()).isEqualTo("회기단 (연기 초)");
        assertThat(itemType.getDescription()).isEqualTo("푸른 단약이다.");
        assertThat(itemType.getBaseValue()).isEqualTo(4);
        assertThat(itemType.isTradable()).isFalse();
    }

    @Test
    void isCalled() {
        ItemType itemType = ItemTypeFixture.createItemType("Jade Slip", ItemCategory.SCROLL, 1);

        assertThat(itemType.isCalled("jade slip")).isTrue();
        assertThat(itemType.isCalled("jade")).isFalse();
    }

    @Test
    void nameLengthFail() {
        assertThatThrownBy(() -> ItemType.create(createItemTypeSaveInfo("   ", ItemCategory.PILL, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("물건 이름은 1~20자로 정하십시오: ");
        String longName = "가".repeat(21);
        assertThatThrownBy(() -> ItemType.create(createItemTypeSaveInfo(longName, ItemCategory.PILL, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("물건 이름은 1~20자로 정하십시오: " + longName);
    }

    @Test
    void bagNeedsGrade() {
        assertThatThrownBy(() -> ItemType.create(new ItemTypeSaveInfo("저물대", null, ItemCategory.BAG, 10, true, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("저물대 등급을 고르십시오.");
    }

    @Test
    void gradeOnlyForBag() {
        assertThatThrownBy(() -> ItemType.create(new ItemTypeSaveInfo("회춘단", null, ItemCategory.PILL, 3, true, BagGrade.LOW)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("저물대 등급은 저물대에만 정합니다.");
    }

    @Test
    void reservedGradeNameFail() {
        assertThatThrownBy(() -> ItemType.create(createItemTypeSaveInfo("하품", ItemCategory.MATERIAL, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("영석 등급 이름은 물건 이름으로 쓸 수 없습니다: 하품");
    }

    @Test
    void lastWordNumberFail() {
        assertThatThrownBy(() -> ItemType.create(createItemTypeSaveInfo("회춘단 2", ItemCategory.PILL, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("물건 이름의 마지막 낱말은 수나 '전부'일 수 없습니다: 회춘단 2");
        assertThatThrownBy(() -> ItemType.create(createItemTypeSaveInfo("가죽 전부", ItemCategory.MATERIAL, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("물건 이름의 마지막 낱말은 수나 '전부'일 수 없습니다: 가죽 전부");
    }

    @Test
    void ordinalPrefixFail() {
        assertThatThrownBy(() -> ItemType.create(createItemTypeSaveInfo("2.가죽", ItemCategory.MATERIAL, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("물건 이름은 '숫자.' 로 시작할 수 없습니다: 2.가죽");
    }

    @Test
    void descriptionLengthFail() {
        assertThatThrownBy(() -> ItemType.create(new ItemTypeSaveInfo("회춘단", "가".repeat(501), ItemCategory.PILL, 3, true, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("묘사는 500자까지 쓸 수 있습니다.");
    }

    @Test
    void spiritStoneCategoryFail() {
        assertThatThrownBy(() -> ItemType.create(createItemTypeSaveInfo("영석", ItemCategory.SPIRIT_STONE, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("영석 종류는 만들 수 없습니다.");
        ItemType pill = createPill();
        assertThatThrownBy(() -> pill.update(createItemTypeSaveInfo("영석", ItemCategory.SPIRIT_STONE, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("영석 종류는 만들 수 없습니다.");
    }

    @Test
    void baseValueRangeFail() {
        assertThatThrownBy(() -> ItemType.create(createItemTypeSaveInfo("회춘단", ItemCategory.PILL, -1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("기준가는 0 이상 100,000,000 이하로 정하십시오.");
        assertThatThrownBy(() -> ItemType.create(createItemTypeSaveInfo("회춘단", ItemCategory.PILL, 100_000_001)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("기준가는 0 이상 100,000,000 이하로 정하십시오.");
        assertThat(ItemType.create(createItemTypeSaveInfo("회춘단", ItemCategory.PILL, 100_000_000)).getBaseValue())
                .isEqualTo(100_000_000);
    }

    @Test
    void spiritStone() {
        ItemType stone = ItemType.spiritStone(SpiritStoneGrade.LOW);

        assertThat(stone.getName()).isEqualTo("하품 영석");
        assertThat(stone.getBaseValue()).isEqualTo(1);
        assertThat(stone.getDescription()).isEqualTo("영기가 옅게 서린 돌. 수선자들이 값을 치르는 데 쓴다.");
        assertThat(stone.getCategory()).isEqualTo(ItemCategory.SPIRIT_STONE);
        assertThat(stone.isTradable()).isTrue();
        assertThat(stone.stackSize()).isEqualTo(10_000);
        assertThat(stone.isSpiritStone()).isTrue();
        assertThat(stone.findSpiritStoneGrade()).contains(SpiritStoneGrade.LOW);
    }

    @Test
    void spiritStoneDescriptions() {
        assertThat(ItemType.spiritStone(SpiritStoneGrade.MIDDLE).getDescription())
                .isEqualTo("영기가 또렷하게 서린 돌. 하품 영석 100개의 값이다.");
        assertThat(ItemType.spiritStone(SpiritStoneGrade.HIGH).getDescription())
                .isEqualTo("영기가 짙게 서린 돌. 하품 영석 10,000개의 값이다.");
        assertThat(ItemType.spiritStone(SpiritStoneGrade.TOP).getDescription())
                .isEqualTo("영기가 넘실대는 돌. 하품 영석 1,000,000개의 값이다.");
        assertThat(ItemType.spiritStone(SpiritStoneGrade.TOP).getBaseValue()).isEqualTo(1_000_000);
    }

    @Test
    void updateSpiritStoneFail() {
        ItemType stone = ItemType.spiritStone(SpiritStoneGrade.LOW);

        assertThatThrownBy(() -> stone.update(createItemTypeSaveInfo("돌", ItemCategory.MATERIAL, 1)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void grades() {
        assertThat(BagGrade.LOW.koreanName()).isEqualTo("하급");
        assertThat(BagGrade.LOW.slots()).isEqualTo(20);
        assertThat(BagGrade.MIDDLE.slots()).isEqualTo(40);
        assertThat(BagGrade.HIGH.koreanName()).isEqualTo("상급");
        assertThat(BagGrade.HIGH.slots()).isEqualTo(80);

        assertThat(SpiritStoneGrade.values()).extracting(SpiritStoneGrade::itemName)
                .containsExactly("하품 영석", "중품 영석", "상품 영석", "극품 영석");
        assertThat(SpiritStoneGrade.values()).extracting(SpiritStoneGrade::value)
                .containsExactly(1L, 100L, 10_000L, 1_000_000L);
        assertThat(SpiritStoneGrade.fromWord("극품")).contains(SpiritStoneGrade.TOP);
        assertThat(SpiritStoneGrade.fromWord("하품 영석")).isEmpty();
        assertThat(SpiritStoneGrade.fromWord("가죽")).isEmpty();
    }
}
