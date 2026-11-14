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

    /**
     * 칸을 넘은 소지품: 저물탁 하나·저물대 둘을 차고 {@code filler}(겹침 1) 130개를 넣었다. 저물탁이 열리면 142칸,
     * 닫히면 42칸이라 130/42칸이 된다(경지가 내려간 경우)
     */
    public static Inventory createOverCapacityInventory(ItemType bracelet, ItemType bag, ItemType filler) {
        Inventory inventory = createInventory();
        inventory.add(bracelet, 1, true);
        inventory.add(bag, 2, true);
        inventory.add(filler, 130, true);
        return inventory;
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
