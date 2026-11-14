package mudmk3.domain.npc;

import mudmk3.domain.item.ItemCategory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
import static mudmk3.domain.item.ItemCategory.SCROLL;
import static mudmk3.domain.item.ItemCategory.TALISMAN;
import static mudmk3.domain.item.ItemCategory.TOOL;

/**
 * 상점 종류(룰북 game-structure.md 상점 표). 파는 것·사 주는 것은 NPC마다 고르지 않고 종류가 정한다.
 * 선언 순서가 표 순서다
 */
public enum ShopKind {
    PILL_SHOP("단약방", List.of(PILL), List.of(PILL, ELIXIR)),
    ARTIFACT_SHOP("법기점", List.of(ARTIFACT, PUPPET, TOOL), List.of(ARTIFACT, PUPPET, TOOL)),
    BOOK_SHOP("서방", List.of(SCROLL), List.of(SCROLL)),
    TALISMAN_SHOP("부적방", List.of(TALISMAN), List.of(TALISMAN, FORMATION_DISK)),
    MATERIAL_SHOP("재료상", List.of(MATERIAL), List.of(MATERIAL)),
    GENERAL_STORE("잡화점", List.of(BAG, BEAST_FOOD, PASS), List.of(BAG, BEAST_FOOD, PASS)),
    BEAST_SHOP("영수점", List.of(BEAST_EGG, PILL), List.of());

    private final String koreanName;
    private final List<ItemCategory> sellCategories;
    private final List<ItemCategory> buyCategories;

    ShopKind(String koreanName, List<ItemCategory> sellCategories, List<ItemCategory> buyCategories) {
        this.koreanName = koreanName;
        this.sellCategories = sellCategories;
        this.buyCategories = buyCategories;
    }

    public String koreanName() {
        return koreanName;
    }

    /** 이 종류의 상점이 그 종류의 물건을 팔 수 있는지 */
    public boolean sells(ItemCategory category) {
        return sellCategories.contains(category);
    }

    /** 이 종류의 상점이 그 종류의 물건을 사 주는지 */
    public boolean buys(ItemCategory category) {
        return buyCategories.contains(category);
    }

    /** 사 주는 종류(표 순서, 바꿀 수 없음) */
    public List<ItemCategory> buyCategories() {
        return buyCategories;
    }

    /** 그 종류를 사 주는 상점 종류 중 표 순서 첫째. 아무도 사지 않으면 비어 있다 */
    public static Optional<ShopKind> buyerOf(ItemCategory category) {
        return Arrays.stream(values()).filter(kind -> kind.buys(category)).findFirst();
    }
}
