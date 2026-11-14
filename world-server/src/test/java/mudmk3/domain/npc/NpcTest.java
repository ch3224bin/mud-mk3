package mudmk3.domain.npc;

import mudmk3.domain.item.ItemCategory;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.item.ItemTypeFixture;
import mudmk3.domain.item.SpiritStoneGrade;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class NpcTest {

    @Test
    void create() {
        Npc npc = Npc.create(new NpcSaveInfo(" 왕씨  ", "  어서 오시오. ", " 글쎄, 잘 모르겠구려. ",
                List.of(new NpcTopic("단약", "단약은 귀한 물건이오.")), false));

        assertThat(npc.getName()).isEqualTo("왕씨");
        assertThat(npc.getGreeting()).isEqualTo("어서 오시오.");
        assertThat(npc.getDefaultAnswer()).isEqualTo("글쎄, 잘 모르겠구려.");
        assertThat(npc.getTopics()).containsExactly(new NpcTopic("단약", "단약은 귀한 물건이오."));
        assertThat(npc.isLlmEnabled()).isFalse();
        assertThat(npc.findRoomId()).isEmpty();
        assertThat(NpcFixture.createNpc().isLlmEnabled()).isFalse();
    }

    @Test
    void nameWithSpaceFail() {
        assertThatThrownBy(() -> Npc.create(NpcFixture.createNpcSaveInfo("왕 씨")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Npc.create(NpcFixture.createNpcSaveInfo("가나다라마바사아자차카타파")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void topicBracketsStripped() {
        NpcTopic topic = new NpcTopic(" [단약] ", "단약은 귀한 물건이오.");

        assertThat(topic.word()).isEqualTo("단약");
    }

    @Test
    void topicAnswerRule() {
        assertThatThrownBy(() -> new NpcTopic("단약", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("대답은 1~1000자로 정하십시오: 단약");
        assertThatThrownBy(() -> new NpcTopic("단약", "가".repeat(1001)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NpcTopic("가".repeat(21), "대답"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void duplicateTopicFail() {
        assertThatThrownBy(() -> Npc.create(NpcFixture.createNpcSaveInfo("왕씨", List.of(
                new NpcTopic("단약", "가"), new NpcTopic("단약", "나")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("같은 낱말이 두 번 있습니다: 단약");
        assertThatThrownBy(() -> Npc.create(NpcFixture.createNpcSaveInfo("왕씨", List.of(
                new NpcTopic("Herb", "가"), new NpcTopic("herb", "나")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("같은 낱말이 두 번 있습니다: herb");
    }

    @Test
    void topicWithSpaceFail() {
        assertThatThrownBy(() -> new NpcTopic("늑대 퇴치", "숲의 늑대를 잡아 주시오."))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("낱말은 띄어쓰기 없이 1~20자로 정하십시오: 늑대 퇴치");
    }

    @Test
    void answerToKnownWord() {
        Npc npc = NpcFixture.createNpc();

        assertThat(npc.answerTo("일거리")).isEqualTo("숲의 늑대를 잡아 주시오.");
        assertThat(npc.findTopic("일거리")).isPresent();
    }

    @Test
    void answerToUnknownWordIsDefault() {
        Npc npc = NpcFixture.createNpc();

        assertThat(npc.answerTo("영약")).isEqualTo("글쎄, 잘 모르겠구려.");
        assertThat(npc.findTopic("영약")).isEmpty();
    }

    @Test
    void answerIgnoresBracketsAndLatinCase() {
        Npc npc = Npc.create(NpcFixture.createNpcSaveInfo("왕씨", List.of(
                new NpcTopic("단약", "단약은 귀한 물건이오."), new NpcTopic("Herb", "약초요."))));

        assertThat(npc.answerTo("[단약]")).isEqualTo("단약은 귀한 물건이오.");
        assertThat(npc.answerTo("HERB")).isEqualTo("약초요.");
    }

    @Test
    void noPrefixMatchForWord() {
        Npc npc = NpcFixture.createNpc();

        assertThat(npc.answerTo("단")).isEqualTo("글쎄, 잘 모르겠구려.");
    }

    @Test
    void placeAndUnplace() {
        Npc npc = NpcFixture.createNpc();

        npc.placeIn(3L);
        assertThat(npc.findRoomId()).contains(3L);

        npc.unplace();
        assertThat(npc.findRoomId()).isEmpty();
    }

    @Test
    void updateReplacesTopics() {
        Npc npc = NpcFixture.createNpc();

        npc.update(new NpcSaveInfo("왕노인", "허허.", "모르겠네.", List.of(
                new NpcTopic("일거리", "없소."), new NpcTopic("단약", "비싸오."), new NpcTopic("길", "북쪽이오.")), true));

        assertThat(npc.getName()).isEqualTo("왕노인");
        assertThat(npc.getGreeting()).isEqualTo("허허.");
        assertThat(npc.getDefaultAnswer()).isEqualTo("모르겠네.");
        assertThat(npc.isLlmEnabled()).isTrue();
        assertThat(npc.getTopics()).extracting(NpcTopic::word).containsExactly("일거리", "단약", "길");
    }

    @Test
    void topicsUnmodifiable() {
        Npc npc = NpcFixture.createNpc();

        assertThatThrownBy(() -> npc.getTopics().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void nameMatching() {
        Npc npc = NpcFixture.createNpc("왕씨");

        assertThat(npc.isCalled("왕씨")).isTrue();
        assertThat(npc.nameStartsWith("왕")).isTrue();
        assertThat(npc.isCalled("왕")).isFalse();
        assertThat(npc.nameStartsWith("씨")).isFalse();

        Npc latin = NpcFixture.createNpc("Wang");
        assertThat(latin.isCalled("wANG")).isTrue();
        assertThat(latin.nameStartsWith("wa")).isTrue();
    }
    @Test
    void openShop() {
        Npc npc = NpcFixture.createNpc("왕씨");
        ItemType pill = itemType(1L, "회춘단", ItemCategory.PILL, 3);
        ItemType other = itemType(2L, "청심단", ItemCategory.PILL, 5);

        npc.openShop(ShopKind.PILL_SHOP, List.of(new ShopOffer(other, 12), new ShopOffer(pill, null)));

        assertThat(npc.hasShop()).isTrue();
        assertThat(npc.findShopKind()).contains(ShopKind.PILL_SHOP);
        assertThat(npc.shopName()).isEqualTo("왕씨 단약방");
        assertThat(npc.getShopItems()).extracting(ShopItem::itemTypeId, ShopItem::price)
                .containsExactly(tuple(2L, 12), tuple(1L, null));
        assertThatThrownBy(() -> npc.getShopItems().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void openShopReplaces() {
        Npc npc = NpcFixture.createNpc("왕씨");
        npc.openShop(ShopKind.PILL_SHOP, List.of(new ShopOffer(itemType(1L, "회춘단", ItemCategory.PILL, 3), null)));

        npc.openShop(ShopKind.MATERIAL_SHOP, List.of(new ShopOffer(itemType(3L, "늑대 가죽", ItemCategory.MATERIAL, 2), 5)));

        assertThat(npc.shopName()).isEqualTo("왕씨 재료상");
        assertThat(npc.getShopItems()).containsExactly(new ShopItem(3L, 5));
    }

    @Test
    void openEmptyShop() {
        Npc npc = NpcFixture.createNpc("왕씨");

        npc.openShop(ShopKind.BOOK_SHOP, List.of());

        assertThat(npc.hasShop()).isTrue();
        assertThat(npc.getShopItems()).isEmpty();
    }

    @Test
    void sellCategoryFail() {
        Npc npc = NpcFixture.createNpc("왕씨");

        assertThatThrownBy(() -> npc.openShop(ShopKind.PILL_SHOP,
                List.of(new ShopOffer(itemType(1L, "비검", ItemCategory.ARTIFACT, 50), null))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("단약방은 법기를 팔 수 없습니다.");
        assertThatThrownBy(() -> npc.openShop(ShopKind.GENERAL_STORE,
                List.of(new ShopOffer(itemType(1L, "늑대 가죽", ItemCategory.MATERIAL, 2), null))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잡화점은 재료를 팔 수 없습니다.");
        assertThat(npc.hasShop()).isFalse();
    }

    @Test
    void spiritStoneFail() {
        Npc npc = NpcFixture.createNpc("왕씨");
        ItemType stone = ItemType.spiritStone(SpiritStoneGrade.LOW);
        ReflectionTestUtils.setField(stone, "id", 1L);

        assertThatThrownBy(() -> npc.openShop(ShopKind.GENERAL_STORE, List.of(new ShopOffer(stone, 1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("영석은 상점에서 팔 수 없습니다.");
    }

    @Test
    void duplicateItemFail() {
        Npc npc = NpcFixture.createNpc("왕씨");
        ItemType pill = itemType(1L, "회춘단", ItemCategory.PILL, 3);

        assertThatThrownBy(() -> npc.openShop(ShopKind.PILL_SHOP,
                List.of(new ShopOffer(pill, null), new ShopOffer(pill, 10))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("같은 물건이 두 번 있습니다: 회춘단");
    }

    @Test
    void tooManyItemsFail() {
        Npc npc = NpcFixture.createNpc("왕씨");
        List<ShopOffer> offers = new ArrayList<>();
        for (long id = 1; id <= 51; id++) {
            offers.add(new ShopOffer(itemType(id, "단약" + id + "호", ItemCategory.PILL, 3), null));
        }

        assertThatThrownBy(() -> npc.openShop(ShopKind.PILL_SHOP, offers))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("상점에 둘 수 있는 물건은 50개까지입니다.");
        npc.openShop(ShopKind.PILL_SHOP, offers.subList(0, 50));
        assertThat(npc.getShopItems()).hasSize(50);
    }

    @Test
    void unfairPriceFail() {
        Npc npc = NpcFixture.createNpc("왕씨");
        ItemType pill = itemType(1L, "회춘단", ItemCategory.PILL, 3);

        assertThatThrownBy(() -> npc.openShop(ShopKind.PILL_SHOP, List.of(new ShopOffer(pill, 1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("판매가는 매입가의 2배보다 커야 합니다: 회춘단 (매입가 0.75, 판매가 1)");
        // 매입가 0.75 의 2배 1.5 보다 크면 된다
        npc.openShop(ShopKind.PILL_SHOP, List.of(new ShopOffer(pill, 2)));
        assertThat(npc.getShopItems()).containsExactly(new ShopItem(1L, 2));
    }

    @Test
    void unfairPriceLargeValues() {
        Npc npc = NpcFixture.createNpc("왕씨");
        ItemType hide = itemType(1L, "늑대 가죽", ItemCategory.MATERIAL, 5_000);
        // 매입가는 Numbers.decimal(쉼표 없음), 판매가는 Numbers.count(쉼표)로 쓴다

        assertThatThrownBy(() -> npc.openShop(ShopKind.MATERIAL_SHOP, List.of(new ShopOffer(hide, 4_000))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("판매가는 매입가의 2배보다 커야 합니다: 늑대 가죽 (매입가 2000, 판매가 4,000)");
    }

    @Test
    void zeroBaseNeedsPriceFail() {
        Npc npc = NpcFixture.createNpc("왕씨");
        ItemType pill = itemType(1L, "회춘단", ItemCategory.PILL, 0);

        assertThatThrownBy(() -> npc.openShop(ShopKind.PILL_SHOP, List.of(new ShopOffer(pill, null))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("기준가가 0인 물건은 값을 정해야 합니다: 회춘단");
        npc.openShop(ShopKind.PILL_SHOP, List.of(new ShopOffer(pill, 1)));
        assertThat(npc.getShopItems().getFirst().effectivePrice(pill)).isEqualTo(1);
    }

    @Test
    void nullPriceFollowsBase() {
        Npc npc = NpcFixture.createNpc("왕씨");
        ItemType pill = itemType(1L, "회춘단", ItemCategory.PILL, 3);

        npc.openShop(ShopKind.PILL_SHOP, List.of(new ShopOffer(pill, null)));

        ShopItem item = npc.getShopItems().getFirst();
        assertThat(item.effectivePrice(pill)).isEqualTo(3);
        pill.update(ItemTypeFixture.createItemTypeSaveInfo("회춘단", ItemCategory.PILL, 7));
        assertThat(item.effectivePrice(pill)).isEqualTo(7);
    }

    @Test
    void priceRangeFail() {
        assertThatThrownBy(() -> new ShopItem(1L, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("판매가는 1 이상 100,000,000 이하로 정하십시오.");
        assertThatThrownBy(() -> new ShopItem(1L, 100_000_001))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new ShopItem(1L, 100_000_000).price()).isEqualTo(100_000_000);
    }

    @Test
    void closeShop() {
        Npc npc = NpcFixture.createNpc("왕씨");
        npc.openShop(ShopKind.PILL_SHOP, List.of(new ShopOffer(itemType(1L, "회춘단", ItemCategory.PILL, 3), null)));

        npc.closeShop();

        assertThat(npc.hasShop()).isFalse();
        assertThat(npc.findShopKind()).isEmpty();
        assertThat(npc.getShopItems()).isEmpty();
        assertThatThrownBy(npc::shopName).isInstanceOf(IllegalStateException.class);
        // 상점이 없어도 닫을 수 있다
        npc.closeShop();
    }

    @Test
    void updateKeepsShop() {
        Npc npc = NpcFixture.createNpc("왕씨");
        npc.openShop(ShopKind.PILL_SHOP, List.of(new ShopOffer(itemType(1L, "회춘단", ItemCategory.PILL, 3), null)));

        npc.update(NpcFixture.createNpcSaveInfo("왕노인"));

        assertThat(npc.shopName()).isEqualTo("왕노인 단약방");
        assertThat(npc.getShopItems()).hasSize(1);
    }

    @Test
    void greetingTooLongFail() {
        assertThatThrownBy(() -> Npc.create(new NpcSaveInfo("왕씨", "가".repeat(501), "글쎄.", List.of(), false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("인사말은 1~500자로 정하십시오.");
        assertThatThrownBy(() -> Npc.create(new NpcSaveInfo("왕씨", "  ", "글쎄.", List.of(), false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("인사말은 1~500자로 정하십시오.");
        assertThatThrownBy(() -> Npc.create(new NpcSaveInfo("왕씨", "어서 오시오.", "가".repeat(501), List.of(), false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("기본 대답은 1~500자로 정하십시오.");
        assertThatThrownBy(() -> Npc.create(new NpcSaveInfo("왕씨", "어서 오시오.", "", List.of(), false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("기본 대답은 1~500자로 정하십시오.");
        assertThat(Npc.create(new NpcSaveInfo("왕씨", "가".repeat(500), "나".repeat(500), List.of(), false))
                .getGreeting()).hasSize(500);
    }

    @Test
    void topicsOver30Fail() {
        List<NpcTopic> topics = new ArrayList<>();
        for (int i = 1; i <= 31; i++) {
            topics.add(new NpcTopic("낱말" + i, "대답"));
        }

        assertThatThrownBy(() -> Npc.create(NpcFixture.createNpcSaveInfo("왕씨", topics)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("낱말은 30개까지 넣을 수 있습니다.");
        assertThat(Npc.create(NpcFixture.createNpcSaveInfo("왕씨", topics.subList(0, 30))).getTopics()).hasSize(30);
    }

    private static ItemType itemType(Long id, String name, ItemCategory category, long baseValue) {
        ItemType itemType = ItemTypeFixture.createItemType(name, category, baseValue);
        ReflectionTestUtils.setField(itemType, "id", id);
        return itemType;
    }
}
