package mudmk3.domain.character;

import mudmk3.domain.shared.Realm;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ManaTest {

    @Test
    void spendCapsAtMax() {
        double lost = Mana.lostAfterSpend(22, 5, 25);

        assertThat(lost).isEqualTo(25);
        assertThat(Mana.current(lost, 25)).isZero();
        assertThat(Mana.lostAfterSpend(10, -3, 25)).isEqualTo(10);
    }

    @Test
    void recoverNotBelowZero() {
        assertThat(Mana.lostAfterRecover(0.1, 0.125)).isZero();
        assertThat(Mana.lostAfterRecover(10, 1)).isEqualTo(9);
        assertThat(Mana.lostAfterRecover(10, -1)).isEqualTo(10);
    }

    /** 연기 7층(영력 11, 경지 몫 55)에서 40 깎임 → 연기 1층(영력 5, 25): 경지 몫이 준 30만큼 줄여 10, 현재 15 */
    @Test
    void demotionReducesLost() {
        double lost = Mana.lostAfterDemotion(40, Realm.QI_7, Realm.QI_1);

        assertThat(lost).isEqualTo(10);
        assertThat(Mana.current(lost, 25)).isEqualTo(15);
        assertThat(Mana.lostAfterDemotion(20, Realm.QI_7, Realm.QI_1)).isZero();
    }

    @Test
    void demotionToMortalClears() {
        assertThat(Mana.lostAfterDemotion(3, Realm.QI_1, Realm.MORTAL)).isZero();
    }

    @Test
    void promotionKeepsLost() {
        assertThat(Mana.lostAfterDemotion(10, Realm.QI_1, Realm.QI_7)).isEqualTo(10);
        assertThat(Mana.lostAfterDemotion(10, Realm.QI_1, Realm.QI_1)).isEqualTo(10);
    }

    @Test
    void current() {
        assertThat(Mana.current(5.5, 25)).isEqualTo(19.5);
        assertThat(Mana.current(30, 25)).isZero();
        assertThat(Mana.REGEN_RATE_PER_SECOND).isEqualTo(0.005);
    }
}
