package mudmk3.application.mob.provided;

import mudmk3.domain.mob.DropTier;
import mudmk3.domain.mob.MobKind;
import mudmk3.domain.mob.MobStats;
import mudmk3.domain.mob.MobTier;
import mudmk3.domain.mob.MobType;
import mudmk3.domain.shared.Realm;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * 몹 보기 값(admin-tool.md {@code MobDetailResponse} 칸): 서비스가 트랜잭션 안에서 만들어 트랜잭션 밖(REST 응답)에서도 읽을 수 있다.
 * {@code rankName} 은 요수 {@code 1급}, 나머지 {@code 연기 6층}. 배치된 방은 이름순이 아니라 배치 id 순이다
 */
public record MobTypeView(
        Long id,
        String name,
        String description,
        MobKind kind,
        String kindName,
        Realm realm,
        String realmName,
        MobTier tier,
        String tierName,
        String rankName,
        String attackName,
        @Nullable BigDecimal attackCoefficient,
        @Nullable Integer respawnSeconds,
        int effectiveRespawnSeconds,
        MobStats stats,
        List<FixedDropView> fixedDrops,
        List<TierDropView> tierDrops,
        List<PlacementView> placements
) {
    public MobTypeView {
        fixedDrops = List.copyOf(fixedDrops);
        tierDrops = List.copyOf(tierDrops);
        placements = List.copyOf(placements);
    }

    /** {@code itemNames} 는 드롭 표 물건 종류 id → 이름(서비스가 아이템 종류에서 읽는다) */
    public static MobTypeView of(MobType mobType, Map<Long, String> itemNames, List<PlacementView> placements) {
        return new MobTypeView(
                requireNonNull(mobType.getId()),
                mobType.getName(),
                mobType.getDescription(),
                mobType.getKind(),
                mobType.getKind().koreanName(),
                mobType.getRealm(),
                mobType.getRealm().koreanName(),
                mobType.getTier(),
                mobType.getTier().koreanName(),
                mobType.rankName(),
                mobType.getAttackName(),
                mobType.getAttackCoefficient(),
                mobType.getRespawnSeconds(),
                mobType.effectiveRespawnSeconds(),
                mobType.stats(),
                mobType.getFixedDrops().stream()
                        .map(drop -> new FixedDropView(drop.itemTypeId(), itemName(itemNames, drop.itemTypeId()),
                                drop.count()))
                        .toList(),
                mobType.getTierDrops().stream()
                        .map(drop -> new TierDropView(drop.tier(), drop.tier().koreanName(), drop.itemTypeId(),
                                itemName(itemNames, drop.itemTypeId())))
                        .toList(),
                placements);
    }

    public int placementCount() {
        return placements.size();
    }

    /** 드롭 표의 물건은 쓰이는 물건이라 지울 수 없으므로 이름이 늘 있다 */
    private static String itemName(Map<Long, String> itemNames, Long itemTypeId) {
        String itemName = itemNames.get(itemTypeId);
        if (itemName == null) {
            throw new IllegalStateException("드롭 표의 물건 종류가 없습니다. id: " + itemTypeId);
        }
        return itemName;
    }

    /** 고정 재료 한 줄(입력 순서) */
    public record FixedDropView(Long itemTypeId, String itemTypeName, int count) {
    }

    /** 계층 드롭 한 줄(입력 순서) */
    public record TierDropView(DropTier tier, String tierName, Long itemTypeId, String itemTypeName) {
    }

    /** 이 몹을 둔 배치 한 줄 */
    public record PlacementView(Long id, Long roomId, String roomName, int count) {
    }
}
