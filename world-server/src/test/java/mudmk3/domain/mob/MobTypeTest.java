package mudmk3.domain.mob;

import mudmk3.domain.shared.Realm;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;

import static mudmk3.domain.mob.MobFixture.boss;
import static mudmk3.domain.mob.MobFixture.info;
import static mudmk3.domain.mob.MobFixture.named;
import static mudmk3.domain.mob.MobFixture.rabbit;
import static mudmk3.domain.mob.MobFixture.withDrops;
import static mudmk3.domain.mob.MobFixture.wolf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MobTypeTest {

    private static final Map<Long, String> ITEM_NAMES = Map.of(
            1L, "토끼 가죽", 2L, "토끼 고기", 3L, "청모랑 가죽", 4L, "늑대 이빨", 5L, "1급 요단", 6L, "회춘단");

    @Test
    void create() {
        MobType mob = MobType.create(withDrops(rabbit(),
                List.of(new MobFixedDrop(1L, 1), new MobFixedDrop(2L, 2)),
                List.of(new MobTierDrop(DropTier.RARE, 5L), new MobTierDrop(DropTier.ADVANCED, 6L),
                        new MobTierDrop(DropTier.ADVANCED, 5L))), ITEM_NAMES);

        assertThat(mob.getName()).isEqualTo("들토끼");
        assertThat(mob.getDescription()).isEqualTo("들토끼의 묘사다.");
        assertThat(mob.getKind()).isEqualTo(MobKind.BEAST);
        assertThat(mob.getRealm()).isEqualTo(Realm.QI_1);
        assertThat(mob.getTier()).isEqualTo(MobTier.NORMAL);
        assertThat(mob.getAttackName()).isEqualTo("뒷발차기");
        assertThat(mob.getAttackCoefficient()).isNull();
        assertThat(mob.getRespawnSeconds()).isNull();
        assertThat(mob.stats()).isEqualTo(new MobStats(42, 7, 7));
        assertThat(mob.getFixedDrops()).containsExactly(new MobFixedDrop(1L, 1), new MobFixedDrop(2L, 2));
        assertThat(mob.getTierDrops()).hasSize(3);
        assertThat(mob.tierItemIds(DropTier.ADVANCED)).containsExactly(6L, 5L);
        assertThat(mob.tierItemIds(DropTier.RARE)).containsExactly(5L);
        assertThat(mob.tierItemIds(DropTier.TREASURE)).isEmpty();
    }

    @Test
    void stripsNameAndText() {
        MobType mob = MobFixture.create(new MobTypeSaveInfo("  적안 독사 ", null, MobKind.BEAST, Realm.QI_5,
                MobTier.STRONG, " 독니 ", null, null, List.of(), List.of()));

        assertThat(mob.getName()).isEqualTo("적안 독사");
        assertThat(mob.getDescription()).isEmpty();
        assertThat(mob.getAttackName()).isEqualTo("독니");
    }

    @Test
    void update() {
        MobType mob = MobType.create(withDrops(rabbit(), List.of(new MobFixedDrop(1L, 1)),
                List.of(new MobTierDrop(DropTier.ADVANCED, 6L))), ITEM_NAMES);

        mob.update(withDrops(wolf(), List.of(new MobFixedDrop(3L, 1), new MobFixedDrop(4L, 1)), List.of()),
                ITEM_NAMES);

        assertThat(mob.getName()).isEqualTo("청모랑");
        assertThat(mob.getRealm()).isEqualTo(Realm.QI_2);
        assertThat(mob.getAttackName()).isEqualTo("물어뜯기");
        assertThat(mob.getFixedDrops()).containsExactly(new MobFixedDrop(3L, 1), new MobFixedDrop(4L, 1));
        assertThat(mob.getTierDrops()).isEmpty();
    }

    @Test
    void rankName() {
        assertThat(MobFixture.create(wolf()).rankName()).isEqualTo("1급");
        assertThat(MobFixture.create(boss()).rankName()).isEqualTo("2급");
        assertThat(MobFixture.create(info("떠도는 음혼", MobKind.GHOST, Realm.QI_10, MobTier.NORMAL, "음풍", null, null))
                .rankName()).isEqualTo("연기 10층");
        assertThat(MobFixture.create(info("요왕", MobKind.BEAST, Realm.FOUNDATION_EARLY, MobTier.NORMAL, "할퀴기",
                null, null)).rankName()).isEqualTo("3급");
    }

    @Test
    void effectiveRespawnSeconds() {
        assertThat(MobFixture.create(wolf()).effectiveRespawnSeconds()).isEqualTo(90);
        assertThat(MobFixture.create(tiered(MobTier.STRONG, null)).effectiveRespawnSeconds()).isEqualTo(180);
        assertThat(MobFixture.create(tiered(MobTier.ELITE, null)).effectiveRespawnSeconds()).isEqualTo(600);
        assertThat(MobFixture.create(tiered(MobTier.NORMAL, 30)).effectiveRespawnSeconds()).isEqualTo(30);
        assertThat(MobFixture.create(boss()).effectiveRespawnSeconds()).isEqualTo(2_700);
    }

    @Test
    void stats() {
        assertThat(MobFixture.create(boss()).stats()).isEqualTo(new MobStats(1_700, 24, 19));
    }

    @Test
    void nameBlankFail() {
        assertFails(named(wolf(), "   "), "몹 이름은 1~20자로 정하십시오.");
    }

    @Test
    void nameTooLongFail() {
        assertFails(named(wolf(), "가".repeat(21)), "몹 이름은 1~20자로 정하십시오.");
        assertThat(MobFixture.create(named(wolf(), "가".repeat(20))).getName()).hasSize(20);
    }

    @Test
    void nameOrdinalFail() {
        assertFails(named(wolf(), "2.늑대"), "몹 이름은 '2.' 처럼 수와 점으로 시작할 수 없습니다.");
        assertFails(named(wolf(), " 12.늑대"), "몹 이름은 '2.' 처럼 수와 점으로 시작할 수 없습니다.");
        assertThat(MobFixture.create(named(wolf(), "2급 늑대")).getName()).isEqualTo("2급 늑대");
    }

    @Test
    void descriptionTooLongFail() {
        MobTypeSaveInfo info = wolf();
        assertFails(new MobTypeSaveInfo(info.name(), "가".repeat(501), info.kind(), info.realm(), info.tier(),
                info.attackName(), null, null, List.of(), List.of()), "몹 묘사는 500자 이내로 적으십시오.");
    }

    @Test
    void mortalRealmFail() {
        assertFails(info("들개", MobKind.BEAST, Realm.MORTAL, MobTier.NORMAL, "물기", null, null),
                "몹의 기준 경지는 연기 1층 이상으로 정하십시오.");
    }

    @Test
    void attackNameFail() {
        assertFails(info("청모랑", MobKind.BEAST, Realm.QI_2, MobTier.NORMAL, "  ", null, null),
                "공격 이름은 1~12자로 정하십시오.");
        assertFails(info("청모랑", MobKind.BEAST, Realm.QI_2, MobTier.NORMAL, "가".repeat(13), null, null),
                "공격 이름은 1~12자로 정하십시오.");
    }

    @Test
    void bossCoefficientMissingFail() {
        assertFails(bossWith(null, 2_700), "두목의 공격 계수는 0.4~0.8로 정하십시오.");
    }

    @Test
    void bossCoefficientRangeFail() {
        assertFails(bossWith("0.3", 2_700), "두목의 공격 계수는 0.4~0.8로 정하십시오.");
        assertFails(bossWith("0.9", 2_700), "두목의 공격 계수는 0.4~0.8로 정하십시오.");
        assertThat(MobFixture.create(bossWith("0.4", 2_700)).getAttackCoefficient()).isEqualByComparingTo("0.4");
        assertThat(MobFixture.create(bossWith("0.80", 2_700)).getAttackCoefficient()).isEqualByComparingTo("0.8");
    }

    @Test
    void coefficientOnNonBossFail() {
        assertFails(info("청모랑", MobKind.BEAST, Realm.QI_2, MobTier.ELITE, "물어뜯기", new BigDecimal("0.6"), null),
                "공격 계수는 두목만 정합니다.");
    }

    @Test
    void coefficientScaleFail() {
        assertFails(bossWith("0.55", 2_700), "공격 계수는 소수 첫째 자리까지 적으십시오.");
        // 두목이 아님이 소수 자리보다 먼저다
        assertFails(info("청모랑", MobKind.BEAST, Realm.QI_2, MobTier.NORMAL, "물어뜯기", new BigDecimal("0.55"), null),
                "공격 계수는 두목만 정합니다.");
    }

    @Test
    void bossRespawnFail() {
        assertFails(bossWith("0.6", null), "두목의 재등장은 1,800~3,600초로 정하십시오.");
        assertFails(bossWith("0.6", 1_799), "두목의 재등장은 1,800~3,600초로 정하십시오.");
        assertFails(bossWith("0.6", 3_601), "두목의 재등장은 1,800~3,600초로 정하십시오.");
        assertThat(MobFixture.create(bossWith("0.6", 1_800)).effectiveRespawnSeconds()).isEqualTo(1_800);
        assertThat(MobFixture.create(bossWith("0.6", 3_600)).effectiveRespawnSeconds()).isEqualTo(3_600);
    }

    @Test
    void respawnRangeFail() {
        assertFails(tiered(MobTier.NORMAL, 9), "재등장은 10~86,400초로 정하십시오.");
        assertFails(tiered(MobTier.ELITE, 86_401), "재등장은 10~86,400초로 정하십시오.");
        assertThat(MobFixture.create(tiered(MobTier.NORMAL, 10)).effectiveRespawnSeconds()).isEqualTo(10);
        assertThat(MobFixture.create(tiered(MobTier.STRONG, 86_400)).effectiveRespawnSeconds()).isEqualTo(86_400);
    }

    @Test
    void fixedDropsTooManyFail() {
        List<MobFixedDrop> six = LongStream.rangeClosed(1, 6).mapToObj(id -> new MobFixedDrop(id, 1)).toList();

        assertFails(withDrops(rabbit(), six, List.of()), "고정 재료는 5종까지 둘 수 있습니다.");
        assertThat(MobType.create(withDrops(rabbit(), six.subList(0, 5), List.of()), ITEM_NAMES).getFixedDrops())
                .hasSize(5);
    }

    @Test
    void fixedDropCountFail() {
        assertFails(withDrops(rabbit(), List.of(new MobFixedDrop(1L, 0)), List.of()),
                "고정 재료의 수는 1~99로 정하십시오: 토끼 가죽");
        assertFails(withDrops(rabbit(), List.of(new MobFixedDrop(2L, 1), new MobFixedDrop(1L, 100)), List.of()),
                "고정 재료의 수는 1~99로 정하십시오: 토끼 가죽");
        assertThat(MobType.create(withDrops(rabbit(), List.of(new MobFixedDrop(1L, 99)), List.of()), ITEM_NAMES)
                .getFixedDrops()).containsExactly(new MobFixedDrop(1L, 99));
    }

    @Test
    void duplicateFixedDropFail() {
        assertFails(withDrops(rabbit(), List.of(new MobFixedDrop(1L, 1), new MobFixedDrop(2L, 1),
                new MobFixedDrop(1L, 2)), List.of()), "같은 물건이 두 번 있습니다: 토끼 가죽");
    }

    @Test
    void tierDropsTooManyFail() {
        List<MobTierDrop> eleven = LongStream.rangeClosed(1, 11)
                .mapToObj(id -> new MobTierDrop(DropTier.ADVANCED, id)).toList();
        Map<Long, String> names = LongStream.rangeClosed(1, 11).boxed()
                .collect(java.util.stream.Collectors.toMap(id -> id, id -> "물건" + id));

        assertThatThrownBy(() -> MobType.create(withDrops(rabbit(), List.of(), eleven), names))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("계층마다 물건은 10개까지 둘 수 있습니다. (고급)");
        // 계층마다 따로 센다
        List<MobTierDrop> tenAndTen = new java.util.ArrayList<>(eleven.subList(0, 10));
        LongStream.rangeClosed(1, 10).forEach(id -> tenAndTen.add(new MobTierDrop(DropTier.TREASURE, id)));
        assertThat(MobType.create(withDrops(rabbit(), List.of(), tenAndTen), names).getTierDrops()).hasSize(20);
    }

    @Test
    void duplicateTierDropFail() {
        assertFails(withDrops(rabbit(), List.of(), List.of(new MobTierDrop(DropTier.RARE, 6L),
                new MobTierDrop(DropTier.RARE, 6L))), "같은 물건이 두 번 있습니다: 회춘단");
        // 다른 계층이나 고정 재료와는 겹쳐도 된다
        MobType mob = MobType.create(withDrops(rabbit(), List.of(new MobFixedDrop(6L, 1)),
                List.of(new MobTierDrop(DropTier.ADVANCED, 6L), new MobTierDrop(DropTier.RARE, 6L))), ITEM_NAMES);
        assertThat(mob.getTierDrops()).hasSize(2);
    }

    /** 처음 걸린 규칙 하나만: 이름 → 묘사 → 기준 경지 → 공격 이름 → 공격 계수 → 재등장 → 고정 재료 → 계층 드롭 */
    @Test
    void checkOrder() {
        assertFails(new MobTypeSaveInfo("", "가".repeat(501), MobKind.BEAST, Realm.MORTAL, MobTier.NORMAL, "", null,
                5, List.of(), List.of()), "몹 이름은 1~20자로 정하십시오.");
        assertFails(new MobTypeSaveInfo("청모랑", null, MobKind.BEAST, Realm.QI_2, MobTier.NORMAL, "물어뜯기", null,
                5, List.of(new MobFixedDrop(1L, 0)), List.of()), "재등장은 10~86,400초로 정하십시오.");
        assertFails(new MobTypeSaveInfo("청모랑", null, MobKind.BEAST, Realm.MORTAL, MobTier.NORMAL, "", null,
                null, List.of(), List.of()), "몹의 기준 경지는 연기 1층 이상으로 정하십시오.");
        assertFails(withDrops(rabbit(), List.of(new MobFixedDrop(1L, 0)),
                List.of(new MobTierDrop(DropTier.RARE, 6L), new MobTierDrop(DropTier.RARE, 6L))),
                "고정 재료의 수는 1~99로 정하십시오: 토끼 가죽");
    }

    @Test
    void failedUpdateKeepsValues() {
        MobType mob = MobType.create(withDrops(wolf(), List.of(new MobFixedDrop(3L, 1)), List.of()), ITEM_NAMES);

        assertThatThrownBy(() -> mob.update(withDrops(named(rabbit(), "들토끼"), List.of(new MobFixedDrop(1L, 0)),
                List.of()), ITEM_NAMES)).isInstanceOf(IllegalArgumentException.class);

        assertThat(mob.getName()).isEqualTo("청모랑");
        assertThat(mob.getFixedDrops()).containsExactly(new MobFixedDrop(3L, 1));
    }

    @Test
    void requiredValuesFail() {
        assertThatThrownBy(() -> new MobTypeSaveInfo("청모랑", null, null, Realm.QI_2, MobTier.NORMAL, "물기",
                null, null, List.of(), List.of())).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new MobTypeSaveInfo("청모랑", null, MobKind.BEAST, Realm.QI_2, MobTier.NORMAL, "물기",
                null, null, Collections.singletonList(null), List.of())).isInstanceOf(NullPointerException.class);
    }

    private static MobTypeSaveInfo tiered(MobTier tier, @Nullable Integer respawnSeconds) {
        return info("적안 독사", MobKind.BEAST, Realm.QI_5, tier, "독니", null, respawnSeconds);
    }

    private static MobTypeSaveInfo bossWith(@Nullable String coefficient, @Nullable Integer respawnSeconds) {
        return info("쌍두 흑망", MobKind.BEAST, Realm.QI_12, MobTier.BOSS, "두 머리 물기",
                coefficient == null ? null : new BigDecimal(coefficient), respawnSeconds);
    }

    private static void assertFails(MobTypeSaveInfo info, String message) {
        assertThatThrownBy(() -> MobType.create(info, ITEM_NAMES))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(message);
    }
}
