package mudmk3.application.item.provided;

import jakarta.validation.Valid;
import mudmk3.domain.item.ItemType;

/** 아이템 종류를 만들고 고치고 지운다 */
public interface ItemTypeEditor {

    ItemType create(@Valid ItemTypeSaveRequest request);

    ItemType update(Long itemTypeId, @Valid ItemTypeSaveRequest request);

    void delete(Long itemTypeId);
}
