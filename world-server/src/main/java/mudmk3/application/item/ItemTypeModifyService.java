package mudmk3.application.item;

import lombok.RequiredArgsConstructor;
import mudmk3.application.item.provided.ItemTypeEditor;
import mudmk3.application.item.provided.ItemTypeFinder;
import mudmk3.application.item.provided.ItemTypeInUseException;
import mudmk3.application.item.provided.ItemTypeSaveRequest;
import mudmk3.application.item.provided.SpiritStoneChangeException;
import mudmk3.application.item.required.ItemTypeRepository;
import mudmk3.domain.item.DuplicateItemTypeNameException;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.item.ItemTypeSaveInfo;
import mudmk3.domain.item.SpiritStoneGrade;
import mudmk3.support.stereotype.ValidatedApplicationService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@ValidatedApplicationService
@RequiredArgsConstructor
public class ItemTypeModifyService implements ItemTypeEditor {

    private final ItemTypeFinder itemTypeFinder;
    private final ItemTypeRepository itemTypeRepository;
    private final Clock clock;

    @Override
    public ItemType create(ItemTypeSaveRequest request) {
        ItemTypeSaveInfo info = request.toInfo();
        if (itemTypeRepository.existsByNameIgnoreCase(info.name())) {
            throw duplicateName(info.name());
        }
        return itemTypeRepository.save(ItemType.create(info));
    }

    @Override
    public ItemType update(Long itemTypeId, ItemTypeSaveRequest request) {
        ItemType itemType = itemTypeFinder.find(itemTypeId);
        ensureNotSpiritStone(itemType);
        ItemTypeSaveInfo info = request.toInfo();
        if (itemTypeRepository.existsByNameIgnoreCaseAndIdNot(info.name(), itemTypeId)) {
            throw duplicateName(info.name());
        }
        boolean slotRuleChanges = itemType.getCategory() != info.category()
                || itemType.findBagGrade().orElse(null) != info.bagGrade();
        if (slotRuleChanges && isInUse(itemTypeId, clock.instant())) {
            throw new ItemTypeInUseException("쓰이는 물건은 종류·저물대 등급을 바꿀 수 없습니다: " + itemType.getName());
        }
        itemType.update(info);
        return itemTypeRepository.save(itemType);
    }

    @Override
    public void delete(Long itemTypeId) {
        ItemType itemType = itemTypeFinder.find(itemTypeId);
        ensureNotSpiritStone(itemType);
        Instant now = clock.instant();
        if (isInUse(itemTypeId, now)) {
            throw new ItemTypeInUseException("쓰이는 물건은 지울 수 없습니다: " + itemType.getName());
        }
        itemTypeRepository.deleteExpiredDroppedItems(itemTypeId, now);
        itemTypeRepository.delete(itemType);
    }

    /** 서버가 뜰 때 영석 네 등급 중 없는 것을 심는다 */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedSpiritStones() {
        for (SpiritStoneGrade grade : SpiritStoneGrade.values()) {
            if (itemTypeRepository.findBySpiritStoneGrade(grade).isEmpty()) {
                itemTypeRepository.save(ItemType.spiritStone(grade));
            }
        }
    }

    /**
     * 소지품·바닥 배치·사라지지 않은 버린 물건·상점·시작 소지품 어디서든 쓰이는지. 리포지토리 count 들의 합으로 보며,
     * 쓰는 슬라이스가 생기는 태스크가 count를 하나씩 더한다(지금은 소지품·시작 소지품·바닥 배치·버린 물건)
     */
    private boolean isInUse(Long itemTypeId, Instant now) {
        long uses = itemTypeRepository.countInInventories(itemTypeId)
                + itemTypeRepository.countInStartingItems(itemTypeId)
                + itemTypeRepository.countInFloorPlacements(itemTypeId)
                + itemTypeRepository.countInDroppedItems(itemTypeId, now);
        return uses > 0;
    }

    private static void ensureNotSpiritStone(ItemType itemType) {
        if (itemType.isSpiritStone()) {
            throw new SpiritStoneChangeException("영석은 엔진이 정한 물건이라 고치거나 지울 수 없습니다.");
        }
    }

    private static DuplicateItemTypeNameException duplicateName(String name) {
        return new DuplicateItemTypeNameException("이미 있는 물건 이름입니다: " + name);
    }
}
