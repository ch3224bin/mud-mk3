package mudmk3.domain.mob;

import mudmk3.ScriptedRandom;
import mudmk3.domain.item.ItemCategory;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.item.ItemTypeFixture;
import mudmk3.domain.item.SpiritStoneGrade;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DropRollTest {

    /** 재료 기준가 1 → 매입가 0.40 */
    private static final ItemType HIDE = item(11L, "청모랑 가죽", 1);
    /** 재료 기준가 2 → 매입가 0.80 */
    private static final ItemType TOOTH = item(12L, "늑대 이빨", 2);
    private static final ItemType ADV_1 = item(21L, "고급 하나", 10);
    private static final ItemType ADV_2 = item(22L, "고급 둘", 10);
    private static final ItemType ADV_3 = item(23L, "고급 셋", 10);
    private static final ItemType RARE_1 = item(31L, "희귀 하나", 100);
    private static final ItemType TREASURE_1 = item(41L, "진귀 하나", 1000);
    private static final ItemType STONE = stone();

    private static final Map<Long, ItemType> ITEMS = byId(HIDE, TOOTH, ADV_1, ADV_2, ADV_3, RARE_1, TREASURE_1);

    private final ScriptedRandom random = new ScriptedRandom();

    @Test
    void fixedAlways() {
        MobType wolf = mob(MobFixture.wolf(), List.of(fixed(HIDE, 2), fixed(TOOTH, 1)), List.of());

        List<DropLine> lines = DropRoll.roll(wolf, ITEMS, STONE, random);

        assertThat(lines).containsExactly(line(HIDE, 2), line(TOOTH, 1));
        assertThat(random.doubleCalls()).isEqualTo(1);
        assertThat(random.intCalls()).isZero();
    }

    @Test
    void stonesWhenRollUnderThirtyPercent() {
        MobType wolf = mob(MobFixture.wolf(), List.of(fixed(HIDE, 1)), List.of());
        random.enqueueDoubles(0.29, 0.5);

        List<DropLine> lines = DropRoll.roll(wolf, ITEMS, STONE, random);

        // 매입가 합 0.4 × 1.5 = 0.6 → floor 0 → 최소 1개
        assertThat(lines).containsExactly(line(HIDE, 1), line(STONE, 1));
        assertThat(random.doubleCalls()).isEqualTo(2);
    }

    @Test
    void stonesScaleWithBuyPrice() {
        MobType wolf = mob(MobFixture.wolf(), List.of(fixed(TOOTH, 3)), List.of());
        random.enqueueDoubles(0.0, 0.5);

        List<DropLine> lines = DropRoll.roll(wolf, ITEMS, STONE, random);

        // 매입가 합 0.8 × 3 = 2.4, × 1.5 = 3.6 → 3개
        assertThat(lines).containsExactly(line(TOOTH, 3), line(STONE, 3));
    }

    @Test
    void noStonesAtThirtyPercent() {
        MobType wolf = mob(MobFixture.wolf(), List.of(fixed(HIDE, 1)), List.of());
        random.enqueueDoubles(0.3);

        assertThat(DropRoll.roll(wolf, ITEMS, STONE, random)).containsExactly(line(HIDE, 1));
        assertThat(random.doubleCalls()).isEqualTo(1);
    }

    @Test
    void noStonesWithoutFixed() {
        MobType wolf = mob(MobFixture.wolf(), List.of(), List.of());

        assertThat(DropRoll.roll(wolf, ITEMS, STONE, random)).isEmpty();
        assertThat(random.doubleCalls()).isZero();
        assertThat(random.intCalls()).isZero();
    }

    @Test
    void tierChanceTimesReward() {
        MobType boss = mob(MobFixture.boss(), List.of(), List.of(tier(DropTier.ADVANCED, ADV_1), tier(DropTier.ADVANCED, ADV_2)));
        // 고급 0.08 × 20 = 1.6 → 확정 1 + 0.6 굴림
        random.enqueueDoubles(0.59).enqueueInts(1, 0);

        assertThat(DropRoll.roll(boss, ITEMS, STONE, random)).containsExactly(line(ADV_2, 1), line(ADV_1, 1));
        assertThat(random.doubleCalls()).isEqualTo(1);
        assertThat(random.intCalls()).isEqualTo(2);
    }

    @Test
    void tierFractionMissKeepsGuaranteed() {
        MobType boss = mob(MobFixture.boss(), List.of(), List.of(tier(DropTier.ADVANCED, ADV_1), tier(DropTier.ADVANCED, ADV_2)));
        random.enqueueDoubles(0.6).enqueueInts(1);

        assertThat(DropRoll.roll(boss, ITEMS, STONE, random)).containsExactly(line(ADV_2, 1));
        assertThat(random.intCalls()).isEqualTo(1);
    }

    @Test
    void emptyTierConsumesNoRandom() {
        MobType wolf = mob(MobFixture.wolf(), List.of(), List.of(tier(DropTier.RARE, RARE_1)));
        random.enqueueDoubles(0.004);

        assertThat(DropRoll.roll(wolf, ITEMS, STONE, random)).containsExactly(line(RARE_1, 1));
        assertThat(random.doubleCalls()).isEqualTo(1);
        assertThat(random.intCalls()).isEqualTo(1);
    }

    @Test
    void pickByNextInt() {
        MobType wolf = mob(MobFixture.wolf(), List.of(),
                List.of(tier(DropTier.ADVANCED, ADV_1), tier(DropTier.ADVANCED, ADV_2), tier(DropTier.ADVANCED, ADV_3)));
        random.enqueueDoubles(0.079).enqueueInts(2);

        assertThat(DropRoll.roll(wolf, ITEMS, STONE, random)).containsExactly(line(ADV_3, 1));
    }

    @Test
    void tierMissDropsNothing() {
        MobType wolf = mob(MobFixture.wolf(), List.of(), List.of(tier(DropTier.ADVANCED, ADV_1)));
        random.enqueueDoubles(0.08);

        assertThat(DropRoll.roll(wolf, ITEMS, STONE, random)).isEmpty();
        assertThat(random.intCalls()).isZero();
    }

    @Test
    void skipsUnknownItem() {
        MobType wolf = mob(MobFixture.wolf(), List.of(fixed(HIDE, 1), fixed(TOOTH, 2)),
                List.of(tier(DropTier.ADVANCED, ADV_1)));
        Map<Long, ItemType> withoutTooth = new LinkedHashMap<>(ITEMS);
        withoutTooth.remove(TOOTH.getId());
        withoutTooth.remove(ADV_1.getId());
        random.enqueueDoubles(0.0, 0.0, 0.0);

        // 지워진 이빨은 매입가 합에도 들지 않는다: 0.4 × 1 → 1개
        assertThat(DropRoll.roll(wolf, withoutTooth, STONE, random)).containsExactly(line(HIDE, 1), line(STONE, 1));
    }

    @Test
    void mergesSameItem() {
        MobType boss = mob(MobFixture.boss(), List.of(fixed(HIDE, 1)),
                List.of(tier(DropTier.ADVANCED, HIDE), tier(DropTier.ADVANCED, ADV_1)));
        random.enqueueDoubles(0.999, 0.0).enqueueInts(0, 1);

        assertThat(DropRoll.roll(boss, ITEMS, STONE, random)).containsExactly(line(HIDE, 2), line(ADV_1, 1));
    }

    @Test
    void orderFixedStonesAdvancedRareTreasure() {
        MobType boss = mob(MobFixture.boss(), List.of(fixed(HIDE, 1)), List.of(
                tier(DropTier.TREASURE, TREASURE_1), tier(DropTier.RARE, RARE_1), tier(DropTier.ADVANCED, ADV_1)));
        // 영석 굴림·개수 u → 고급 나머지 → 희귀 → 진귀
        random.enqueueDoubles(0.1, 0.0, 0.0, 0.0, 0.0);

        assertThat(DropRoll.roll(boss, ITEMS, STONE, random)).containsExactly(
                line(HIDE, 1), line(STONE, 1), line(ADV_1, 2), line(RARE_1, 1), line(TREASURE_1, 1));
        assertThat(random.doubleCalls()).isEqualTo(5);
    }

    private static ItemType item(Long id, String name, long baseValue) {
        ItemType type = ItemTypeFixture.createItemType(name, ItemCategory.MATERIAL, baseValue);
        ReflectionTestUtils.setField(type, "id", id);
        return type;
    }

    private static ItemType stone() {
        ItemType type = ItemType.spiritStone(SpiritStoneGrade.LOW);
        ReflectionTestUtils.setField(type, "id", 99L);
        return type;
    }

    private static Map<Long, ItemType> byId(ItemType... types) {
        return List.of(types).stream().collect(Collectors.toMap(ItemType::getId, t -> t, (a, b) -> a, LinkedHashMap::new));
    }

    private static MobFixedDrop fixed(ItemType type, int count) {
        return new MobFixedDrop(type.getId(), count);
    }

    private static MobTierDrop tier(DropTier dropTier, ItemType type) {
        return new MobTierDrop(dropTier, type.getId());
    }

    private static DropLine line(ItemType type, int count) {
        return new DropLine(type.getId(), type.getName(), count);
    }

    private static MobType mob(MobTypeSaveInfo info, List<MobFixedDrop> fixedDrops, List<MobTierDrop> tierDrops) {
        Map<Long, String> names = ITEMS.values().stream().collect(Collectors.toMap(ItemType::getId, ItemType::getName));
        return MobType.create(MobFixture.withDrops(info, fixedDrops, tierDrops), names);
    }
}
