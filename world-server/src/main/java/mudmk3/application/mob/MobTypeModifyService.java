package mudmk3.application.mob;

import lombok.RequiredArgsConstructor;
import mudmk3.application.item.provided.ItemTypeFinder;
import mudmk3.application.mob.provided.DeletedMobType;
import mudmk3.application.mob.provided.MobTypeEditor;
import mudmk3.application.mob.provided.MobTypeFinder;
import mudmk3.application.mob.provided.MobTypeSaveRequest;
import mudmk3.application.mob.provided.MobTypeView;
import mudmk3.application.mob.required.MobPlacementRepository;
import mudmk3.application.mob.required.MobTypeRepository;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.mob.DuplicateMobNameException;
import mudmk3.domain.mob.MobFixedDrop;
import mudmk3.domain.mob.MobTierDrop;
import mudmk3.domain.mob.MobType;
import mudmk3.domain.mob.MobTypeSaveInfo;
import mudmk3.support.stereotype.ValidatedApplicationService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/** 검사 순서: 없는 물건 종류(400) → 이름 중복(409) → 도메인 규칙(400) */
@ValidatedApplicationService
@RequiredArgsConstructor
public class MobTypeModifyService implements MobTypeEditor {

    private final MobTypeFinder mobTypeFinder;
    private final MobTypeRepository mobTypeRepository;
    private final MobPlacementRepository mobPlacementRepository;
    private final ItemTypeFinder itemTypeFinder;

    @Override
    public MobTypeView create(MobTypeSaveRequest request) {
        MobTypeSaveInfo info = request.toInfo();
        Map<Long, String> itemNames = findItemNames(info);
        if (mobTypeRepository.existsByNameIgnoreCase(info.name())) {
            throw duplicateName(info.name());
        }
        MobType mobType = mobTypeRepository.save(MobType.create(info, itemNames));
        return mobTypeFinder.view(requireNonNull(mobType.getId()));
    }

    @Override
    public MobTypeView update(Long mobTypeId, MobTypeSaveRequest request) {
        MobType mobType = mobTypeFinder.find(mobTypeId);
        MobTypeSaveInfo info = request.toInfo();
        Map<Long, String> itemNames = findItemNames(info);
        if (mobTypeRepository.existsByNameIgnoreCaseAndIdNot(info.name(), mobTypeId)) {
            throw duplicateName(info.name());
        }
        mobType.update(info, itemNames);
        mobTypeRepository.save(mobType);
        return mobTypeFinder.view(mobTypeId);
    }

    /** 배치를 먼저 지워 {@code mob_placement.mob_type_id} 외래 키에 걸리지 않게 한다. 드롭 표는 몹 유형과 함께 지워진다 */
    @Override
    public DeletedMobType delete(Long mobTypeId) {
        MobType mobType = mobTypeFinder.find(mobTypeId);
        long placementCount = mobPlacementRepository.deleteByMobTypeId(mobTypeId);
        mobTypeRepository.delete(mobType);
        return new DeletedMobType(mobTypeId, mobType.getName(), Math.toIntExact(placementCount));
    }

    /**
     * 드롭 표 물건 종류 id → 이름(도메인 문구용). 없는 종류가 있으면 드롭 표 순서(고정 재료 → 계층 드롭)로 첫째를
     * 3차 문구 {@code 물건 종류를 찾을 수 없습니다. id: N}(400)으로 알린다
     */
    private Map<Long, String> findItemNames(MobTypeSaveInfo info) {
        List<Long> ids = new ArrayList<>();
        info.fixedDrops().stream().map(MobFixedDrop::itemTypeId).forEach(ids::add);
        info.tierDrops().stream().map(MobTierDrop::itemTypeId).forEach(ids::add);
        Map<Long, ItemType> found = itemTypeFinder.findAllById(ids);
        Map<Long, String> names = new LinkedHashMap<>();
        for (Long id : ids) {
            ItemType itemType = found.get(id);
            if (itemType == null) {
                throw new IllegalArgumentException("물건 종류를 찾을 수 없습니다. id: " + id);
            }
            names.put(id, itemType.getName());
        }
        return names;
    }

    private static DuplicateMobNameException duplicateName(String name) {
        return new DuplicateMobNameException("이미 있는 몹 이름입니다: " + name);
    }
}
