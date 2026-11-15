package mudmk3.domain.mob;

import mudmk3.domain.item.ItemType;
import mudmk3.domain.item.Pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

import static java.util.Objects.requireNonNull;

/**
 * 몹이 쓰러질 때의 드롭 굴림(룰북 드롭 표). 순서와 난수 소비는 다음과 같다:
 * <ol>
 *   <li>고정 재료: 입력 순서로 모두(난수 없음).</li>
 *   <li>영석: 고정 재료가 하나라도 나왔으면 {@code nextDouble() < 0.3} 일 때 {@code nextDouble()} 하나(u)로
 *       하품 영석 {@code max(1, floor(매입가 합 × (1 + u)))} 개. 매입가 합 = Σ 개당 매입가 × 수.</li>
 *   <li>계층 드롭: 계층마다(고급 → 희귀 → 진귀) 목록이 비지 않았으면 p = 기본 확률 × 보상 배율, 확정 floor(p) 개 +
 *       {@code nextDouble() < p − floor(p)} 면 1개. 개수마다 {@code nextInt(목록 크기)} 로 고른다.</li>
 * </ol>
 * 없는 물건 종류 id(드롭 표가 고쳐진 뒤 지워짐)는 건너뛴다. 같은 물건 줄은 처음 나온 자리에 수를 합친다
 */
public final class DropRoll {

    private static final double STONE_CHANCE = 0.3;

    private DropRoll() {
    }

    /** {@code itemTypes} 는 드롭 표 물건 종류(id → 종류), {@code lowSpiritStone} 은 하품 영석 종류 */
    public static List<DropLine> roll(MobType type, Map<Long, ItemType> itemTypes, ItemType lowSpiritStone,
                                      RandomGenerator random) {
        requireNonNull(type);
        requireNonNull(itemTypes);
        requireNonNull(lowSpiritStone);
        requireNonNull(random);
        List<DropLine> lines = new ArrayList<>();

        BigDecimal buyTotal = BigDecimal.ZERO;
        boolean anyFixed = false;
        for (MobFixedDrop drop : type.getFixedDrops()) {
            ItemType item = itemTypes.get(drop.itemTypeId());
            if (item == null) continue;
            anyFixed = true;
            buyTotal = buyTotal.add(Pricing.unitBuyPrice(item).multiply(BigDecimal.valueOf(drop.count())));
            add(lines, item, drop.count());
        }

        if (anyFixed && random.nextDouble() < STONE_CHANCE) {
            BigDecimal factor = BigDecimal.ONE.add(BigDecimal.valueOf(random.nextDouble()));
            long stones = buyTotal.multiply(factor).setScale(0, RoundingMode.FLOOR).longValue();
            add(lines, lowSpiritStone, (int) Math.clamp(stones, 1, Integer.MAX_VALUE));
        }

        for (DropTier dropTier : DropTier.values()) {
            List<Long> candidates = type.tierItemIds(dropTier);
            if (candidates.isEmpty()) continue;
            BigDecimal chance = BigDecimal.valueOf(dropTier.baseChance())
                    .multiply(BigDecimal.valueOf(type.getTier().rewardMultiplier()));
            BigDecimal guaranteed = chance.setScale(0, RoundingMode.FLOOR);
            int count = guaranteed.intValueExact();
            if (random.nextDouble() < chance.subtract(guaranteed).doubleValue()) {
                count++;
            }
            for (int i = 0; i < count; i++) {
                ItemType item = itemTypes.get(candidates.get(random.nextInt(candidates.size())));
                if (item != null) {
                    add(lines, item, 1);
                }
            }
        }
        return List.copyOf(lines);
    }

    private static void add(List<DropLine> lines, ItemType item, int count) {
        Long itemTypeId = requireNonNull(item.getId(), "저장되지 않은 물건 종류입니다");
        for (int i = 0; i < lines.size(); i++) {
            DropLine line = lines.get(i);
            if (line.itemTypeId().equals(itemTypeId)) {
                lines.set(i, new DropLine(itemTypeId, line.name(), Math.addExact(line.count(), count)));
                return;
            }
        }
        lines.add(new DropLine(itemTypeId, item.getName(), count));
    }
}
