package mudmk3.application.mob.provided;

import mudmk3.domain.mob.DropTier;
import mudmk3.domain.mob.MobTierDrop;

/** 계층 드롭 한 줄 요청. 계층·물건 종류 id가 없으면 만들 때 {@code IllegalArgumentException}({@link MobFixedDropRequest} 와 같음) */
public record MobTierDropRequest(DropTier tier, Long itemTypeId) {

    public MobTierDropRequest {
        MobTypeSaveRequest.required(tier, "tier");
        MobTypeSaveRequest.required(itemTypeId, "itemTypeId");
    }

    public MobTierDrop toDrop() {
        return new MobTierDrop(tier, itemTypeId);
    }
}
