package mudmk3.domain.character;

import mudmk3.domain.shared.Realm;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QiBloodTest {

    @Test
    void lostAfterDamageCapsAtMax() {
        double lost = QiBlood.lostAfterDamage(50, 20, 60);

        assertThat(lost).isEqualTo(60);
        assertThat(QiBlood.current(lost, 60)).isZero();
    }

    @Test
    void lostForOne() {
        assertThat(QiBlood.lostFor(1, 60)).isEqualTo(59);
    }

    @Test
    void lostForClampsToRange() {
        assertThat(QiBlood.lostFor(70, 60)).isZero();
        assertThat(QiBlood.lostFor(-5, 60)).isEqualTo(60);
    }

    @Test
    void demotionReducesLostByMaxDrop() {
        double lost = QiBlood.lostAfterDemotion(30, Realm.QI_7, Realm.QI_1);

        assertThat(lost).isZero();
        assertThat(QiBlood.current(lost, 60)).isEqualTo(60);
    }

    @Test
    void demotionKeepsLowCurrent() {
        double lost = QiBlood.lostAfterDemotion(100, Realm.QI_7, Realm.QI_1);

        assertThat(lost).isEqualTo(40);
        assertThat(QiBlood.current(lost, 60)).isEqualTo(20);
    }

    @Test
    void promotionRaisesCurrent() {
        double lost = QiBlood.lostAfterDemotion(10, Realm.QI_1, Realm.QI_7);

        assertThat(lost).isEqualTo(10);
        assertThat(QiBlood.current(lost, 120)).isEqualTo(110);
    }
}
