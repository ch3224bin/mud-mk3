package mudmk3.domain.inventory;

import mudmk3.domain.item.ItemType;
import mudmk3.domain.item.SpiritStoneGrade;

import java.util.EnumMap;
import java.util.Map;

public class InventoryFixture {

    /** 테스트끼리 같은 인스턴스를 쓰는 영석 네 종류(저장하지 않음) */
    private static final Map<SpiritStoneGrade, ItemType> STONES = createStones();

    public static Inventory createInventory() {
        return Inventory.empty(1L);
    }

    public static ItemType stone(SpiritStoneGrade grade) {
        return STONES.get(grade);
    }

    /** 영석 네 종류(새 Map이라 고쳐도 픽스처에 영향이 없다) */
    public static Map<SpiritStoneGrade, ItemType> stones() {
        return new EnumMap<>(STONES);
    }

    private static Map<SpiritStoneGrade, ItemType> createStones() {
        Map<SpiritStoneGrade, ItemType> stones = new EnumMap<>(SpiritStoneGrade.class);
        for (SpiritStoneGrade grade : SpiritStoneGrade.values()) {
            stones.put(grade, ItemType.spiritStone(grade));
        }
        return stones;
    }
}
