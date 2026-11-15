package mudmk3.domain.shared;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RealmTest {

    /** platform.md 경지 몫 스탯 표: 경지, 육신, 근력, 영력, 의식·정혈, 필요 수위(0 = 없음) */
    private static final List<Object[]> TABLE = List.of(
            new Object[]{Realm.MORTAL, 1, 1, 0, 10, 0},
            new Object[]{Realm.QI_1, 6, 3, 5, 20, 190},
            new Object[]{Realm.QI_2, 7, 4, 6, 22, 290},
            new Object[]{Realm.QI_3, 8, 4, 7, 24, 420},
            new Object[]{Realm.QI_4, 9, 5, 8, 26, 610},
            new Object[]{Realm.QI_5, 10, 5, 9, 28, 880},
            new Object[]{Realm.QI_6, 11, 6, 10, 30, 1_200},
            new Object[]{Realm.QI_7, 12, 6, 11, 32, 1_700},
            new Object[]{Realm.QI_8, 13, 7, 12, 34, 2_400},
            new Object[]{Realm.QI_9, 14, 7, 13, 36, 3_300},
            new Object[]{Realm.QI_10, 15, 8, 14, 38, 4_500},
            new Object[]{Realm.QI_11, 16, 8, 15, 40, 6_100},
            new Object[]{Realm.QI_12, 17, 9, 16, 42, 8_300},
            new Object[]{Realm.QI_13, 18, 9, 17, 44, 11_000},
            new Object[]{Realm.FOUNDATION_EARLY, 36, 18, 34, 88, 47_000},
            new Object[]{Realm.FOUNDATION_MIDDLE, 42, 21, 40, 103, 67_000},
            new Object[]{Realm.FOUNDATION_LATE, 48, 24, 45, 117, 90_000},
            new Object[]{Realm.FOUNDATION_PEAK, 54, 27, 51, 132, 120_000}
    );

    @Test
    void mortal() {
        assertThat(Realm.MORTAL.body()).isEqualTo(1);
        assertThat(Realm.MORTAL.strength()).isEqualTo(1);
        assertThat(Realm.MORTAL.spirit()).isZero();
        assertThat(Realm.MORTAL.senseEssence()).isEqualTo(10);
        assertThat(Realm.MORTAL.requiredCultivation()).isEmpty();
    }

    @Test
    void qiAndFoundation() {
        assertStats(Realm.QI_1, 6, 3, 5, 20, 190);
        assertStats(Realm.QI_2, 7, 4, 6, 22, 290);
        assertStats(Realm.QI_13, 18, 9, 17, 44, 11_000);
        assertStats(Realm.FOUNDATION_EARLY, 36, 18, 34, 88, 47_000);
        assertStats(Realm.FOUNDATION_MIDDLE, 42, 21, 40, 103, 67_000);
        assertStats(Realm.FOUNDATION_LATE, 48, 24, 45, 117, 90_000);
        assertStats(Realm.FOUNDATION_PEAK, 54, 27, 51, 132, 120_000);
    }

    @Test
    void allRealmsMatchTable() {
        assertThat(Realm.values()).hasSize(18);
        assertThat(TABLE).extracting(row -> row[0]).containsExactly((Object[]) Realm.values());
        for (Object[] row : TABLE) {
            Realm realm = (Realm) row[0];
            assertThat(realm.body()).as(realm + " 육신").isEqualTo(row[1]);
            assertThat(realm.strength()).as(realm + " 근력").isEqualTo(row[2]);
            assertThat(realm.spirit()).as(realm + " 영력").isEqualTo(row[3]);
            assertThat(realm.senseEssence()).as(realm + " 의식·정혈").isEqualTo(row[4]);
            int required = (int) row[5];
            assertThat(realm.requiredCultivation()).as(realm + " 필요 수위")
                    .isEqualTo(required == 0 ? OptionalInt.empty() : OptionalInt.of(required));
        }
    }

    @Test
    void koreanName() {
        assertThat(Realm.MORTAL.koreanName()).isEqualTo("범인");
        assertThat(Realm.QI_1.koreanName()).isEqualTo("연기 1층");
        assertThat(Realm.QI_13.koreanName()).isEqualTo("연기 13층");
        assertThat(Realm.FOUNDATION_EARLY.koreanName()).isEqualTo("축기 초기");
        assertThat(Realm.FOUNDATION_MIDDLE.koreanName()).isEqualTo("축기 중기");
        assertThat(Realm.FOUNDATION_LATE.koreanName()).isEqualTo("축기 후기");
        assertThat(Realm.FOUNDATION_PEAK.koreanName()).isEqualTo("축기 대원만");
    }

    @Test
    void opensBracelet() {
        assertThat(Realm.MORTAL.opensBracelet()).isFalse();
        assertThat(Realm.QI_13.opensBracelet()).isFalse();
        assertThat(Realm.FOUNDATION_EARLY.opensBracelet()).isTrue();
        assertThat(Realm.FOUNDATION_PEAK.opensBracelet()).isTrue();
    }

    @Test
    void isBelow() {
        assertThat(Realm.MORTAL.isBelow(Realm.QI_1)).isTrue();
        assertThat(Realm.QI_13.isBelow(Realm.FOUNDATION_EARLY)).isTrue();
        assertThat(Realm.QI_1.isBelow(Realm.QI_1)).isFalse();
        assertThat(Realm.FOUNDATION_EARLY.isBelow(Realm.QI_13)).isFalse();
    }

    @Test
    void majorIndex() {
        assertThat(Realm.MORTAL.majorIndex()).isZero();
        assertThat(Realm.QI_1.majorIndex()).isEqualTo(1);
        assertThat(Realm.QI_13.majorIndex()).isEqualTo(1);
        assertThat(Realm.FOUNDATION_EARLY.majorIndex()).isEqualTo(2);
        assertThat(Realm.FOUNDATION_PEAK.majorIndex()).isEqualTo(2);
    }

    @Test
    void beastGrade() {
        assertThat(Realm.QI_1.beastGrade()).isEqualTo(1);
        assertThat(Realm.QI_7.beastGrade()).isEqualTo(1);
        assertThat(Realm.QI_8.beastGrade()).isEqualTo(2);
        assertThat(Realm.QI_13.beastGrade()).isEqualTo(2);
        assertThat(Realm.FOUNDATION_EARLY.beastGrade()).isEqualTo(3);
        assertThat(Realm.FOUNDATION_PEAK.beastGrade()).isEqualTo(3);
    }

    @Test
    void beastGradeOfMortalFail() {
        assertThatThrownBy(Realm.MORTAL::beastGrade).isInstanceOf(IllegalStateException.class);
    }

    private static void assertStats(Realm realm, int body, int strength, int spirit, int senseEssence, int required) {
        assertThat(realm.body()).isEqualTo(body);
        assertThat(realm.strength()).isEqualTo(strength);
        assertThat(realm.spirit()).isEqualTo(spirit);
        assertThat(realm.senseEssence()).isEqualTo(senseEssence);
        assertThat(realm.requiredCultivation()).hasValue(required);
    }
}
