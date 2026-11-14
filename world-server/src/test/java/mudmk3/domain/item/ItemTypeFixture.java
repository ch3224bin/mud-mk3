package mudmk3.domain.item;

import mudmk3.application.item.provided.ItemTypeSaveRequest;
import org.jspecify.annotations.Nullable;

public class ItemTypeFixture {

    public static ItemTypeSaveInfo createItemTypeSaveInfo(String name, ItemCategory category, long baseValue) {
        return new ItemTypeSaveInfo(name, name + "의 묘사다.", category, baseValue, null);
    }

    public static ItemTypeSaveInfo createBagSaveInfo(String name, BagGrade grade) {
        return new ItemTypeSaveInfo(name, name + "의 묘사다.", ItemCategory.BAG, 10, grade);
    }

    public static ItemType createItemType(String name, ItemCategory category, long baseValue) {
        return ItemType.create(createItemTypeSaveInfo(name, category, baseValue));
    }

    /** 회춘단(연기 초): 단약, 기준가 3 */
    public static ItemType createPill() {
        return createItemType("회춘단(연기 초)", ItemCategory.PILL, 3);
    }

    /** 늑대 가죽: 재료, 기준가 2 */
    public static ItemType createWolfHide() {
        return createItemType("늑대 가죽", ItemCategory.MATERIAL, 2);
    }

    public static ItemType createBag(BagGrade grade) {
        return ItemType.create(createBagSaveInfo(grade.koreanName() + " 저물대", grade));
    }

    public static ItemTypeSaveRequest createItemTypeSaveRequest(String name, ItemCategory category, long baseValue) {
        return createItemTypeSaveRequest(name, category, baseValue, null);
    }

    public static ItemTypeSaveRequest createItemTypeSaveRequest(String name, ItemCategory category, long baseValue,
                                                                @Nullable BagGrade bagGrade) {
        return new ItemTypeSaveRequest(name, name + "의 묘사다.", category, baseValue, bagGrade);
    }
}
