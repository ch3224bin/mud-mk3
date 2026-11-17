package mudmk3.application.item;

import mudmk3.ApplicationServiceTest;
import mudmk3.application.item.provided.ItemTypeEditor;
import mudmk3.application.item.provided.ItemTypeFinder;
import mudmk3.domain.item.ItemCategory;
import mudmk3.domain.item.ItemType;
import org.junit.jupiter.api.Test;

import static mudmk3.domain.item.ItemTypeFixture.createItemTypeSaveRequest;
import static org.assertj.core.api.Assertions.assertThat;

/** 아이템 종류 목록 순서: 영석(하품 → 극품) 다음 이름순(영문 대소문자 무시) */
@ApplicationServiceTest
record ItemTypeQueryServiceTest(ItemTypeEditor itemTypeEditor, ItemTypeFinder itemTypeFinder) {

    @Test
    void itemTypesSortedIgnoringCase() {
        itemTypeEditor.create(createItemTypeSaveRequest("cherry", ItemCategory.MATERIAL, 1));
        itemTypeEditor.create(createItemTypeSaveRequest("Banana", ItemCategory.MATERIAL, 1));
        itemTypeEditor.create(createItemTypeSaveRequest("apple", ItemCategory.MATERIAL, 1));

        assertThat(itemTypeFinder.findAll()).extracting(ItemType::getName)
                .containsExactly("하품 영석", "중품 영석", "상품 영석", "극품 영석", "apple", "Banana", "cherry");
    }
}
