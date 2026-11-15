package mudmk3.domain.mob;

import static java.util.Objects.requireNonNull;

/** 계층 드롭 목록의 물건 하나: 계층과 물건 종류 id(FK 없음, {@link MobFixedDrop} 과 같은 까닭) */
public record MobTierDrop(DropTier tier, Long itemTypeId) {

    public MobTierDrop {
        requireNonNull(tier);
        requireNonNull(itemTypeId);
    }
}
