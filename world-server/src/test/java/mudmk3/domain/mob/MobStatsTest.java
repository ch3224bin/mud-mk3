package mudmk3.domain.mob;

import mudmk3.domain.shared.Realm;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MobStatsTest {

    private record Row(String name, MobKind kind, Realm realm, MobTier tier, @Nullable String coefficient,
                       MobStats expected) {
    }

    /** 룰북 첫 지역 몹 표(first-region.md 몹)와 admin-tool.md 스탯 예 */
    private static final List<Row> TABLE = List.of(
            new Row("청모랑", MobKind.BEAST, Realm.QI_2, MobTier.NORMAL, null, new MobStats(49, 8, 8)),
            new Row("들토끼", MobKind.BEAST, Realm.QI_1, MobTier.NORMAL, null, new MobStats(42, 7, 7)),
            new Row("적안 독사", MobKind.BEAST, Realm.QI_5, MobTier.STRONG, null, new MobStats(100, 12, 15)),
            new Row("흑풍랑왕", MobKind.BEAST, Realm.QI_7, MobTier.ELITE, null, new MobStats(240, 14, 14)),
            new Row("석괴뢰", MobKind.PUPPET, Realm.QI_11, MobTier.STRONG, null, new MobStats(128, 24, 24)),
            new Row("쌍두 흑망", MobKind.BEAST, Realm.QI_12, MobTier.BOSS, "0.6", new MobStats(1_700, 24, 19)),
            new Row("떠도는 음혼", MobKind.GHOST, Realm.QI_10, MobTier.NORMAL, null, new MobStats(105, 8, 17)),
            new Row("산적 수선자", MobKind.CULTIVATOR, Realm.QI_6, MobTier.NORMAL, null, new MobStats(77, 11, 13))
    );

    @Test
    void firstRegionMobs() {
        for (Row row : TABLE) {
            String text = row.coefficient();
            BigDecimal coefficient = text == null ? null : new BigDecimal(text);
            assertThat(MobStats.of(row.kind(), row.realm(), row.tier(), coefficient)).as(row.name())
                    .isEqualTo(row.expected());
        }
    }

    /** 반올림은 정확한 값에서 한다: 연기 6층 두목 계수 0.5 의 위력 (3 + 18) × 0.5 = 10.5 → 11 */
    @Test
    void roundsHalfUp() {
        assertThat(MobStats.of(MobKind.CULTIVATOR, Realm.QI_6, MobTier.BOSS, new BigDecimal("0.5")).power())
                .isEqualTo(11);
        // 축기 중기 두목 계수 0.7: (3 + 72) × 0.7 = 52.5 → 53
        assertThat(MobStats.of(MobKind.CULTIVATOR, Realm.FOUNDATION_MIDDLE, MobTier.BOSS, new BigDecimal("0.7")).power())
                .isEqualTo(53);
    }

    @Test
    void coefficientIgnoredForNonBoss() {
        assertThat(MobStats.of(MobKind.BEAST, Realm.QI_2, MobTier.NORMAL, new BigDecimal("0.8")))
                .isEqualTo(new MobStats(49, 8, 8));
    }

    @Test
    void bossWithoutCoefficientFail() {
        assertThatThrownBy(() -> MobStats.of(MobKind.BEAST, Realm.QI_12, MobTier.BOSS, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
