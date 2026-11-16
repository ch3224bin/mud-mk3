package mudmk3.application.character.provided;

import jakarta.persistence.EntityManager;
import mudmk3.ApplicationServiceTest;
import mudmk3.domain.character.PlayerCharacter;
import mudmk3.domain.shared.Realm;
import org.junit.jupiter.api.Test;

import static java.util.Objects.requireNonNull;
import static mudmk3.domain.character.CharacterFixture.createCharacter;
import static mudmk3.domain.character.CharacterFixture.spiritRoot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ApplicationServiceTest
record CharacterVitalsEditorTest(CharacterVitalsEditor characterVitalsEditor, CharacterRealmEditor characterRealmEditor,
                                 CharacterFinder characterFinder, EntityManager entityManager) {

    private static final String UNKNOWN = "캐릭터를 찾을 수 없습니다. id: 999";

    @Test
    void takeDamagePersists() {
        Long han = prepareCharacter(Realm.QI_1);

        assertThat(characterVitalsEditor.takeDamage(han, 12.5, 60)).isEqualTo(47.5);
        flushAndClear();

        assertThat(characterVitalsEditor.qiBloodLost(han)).isEqualTo(12.5);
        assertThat(characterFinder.find(han).getQiBloodLostAtLoad()).isEqualTo(12.5);
    }

    @Test
    void takeDamageCapsAtMax() {
        Long han = prepareCharacter(Realm.QI_1);
        characterVitalsEditor.takeDamage(han, 50, 60);

        assertThat(characterVitalsEditor.takeDamage(han, 20, 60)).isZero();
        assertThat(characterVitalsEditor.qiBloodLost(han)).isEqualTo(60);
    }

    @Test
    void setQiBlood() {
        Long han = prepareCharacter(Realm.QI_1);

        characterVitalsEditor.setQiBlood(han, 1, 60);
        flushAndClear();

        assertThat(characterVitalsEditor.qiBloodLost(han)).isEqualTo(59);
    }

    @Test
    void restoreReturnsFalseWhenFull() {
        Long han = prepareCharacter(Realm.QI_1);

        assertThat(characterVitalsEditor.restoreQiBlood(han)).isFalse();

        characterVitalsEditor.takeDamage(han, 0.4, 60);
        assertThat(characterVitalsEditor.restoreQiBlood(han)).isTrue();
        flushAndClear();

        assertThat(characterVitalsEditor.qiBloodLost(han)).isZero();
        assertThat(characterVitalsEditor.restoreQiBlood(han)).isFalse();
    }

    @Test
    void unknownCharacterFail() {
        assertThatThrownBy(() -> characterVitalsEditor.qiBloodLost(999L))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(UNKNOWN);
        assertThatThrownBy(() -> characterVitalsEditor.takeDamage(999L, 1, 60))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(UNKNOWN);
        assertThatThrownBy(() -> characterVitalsEditor.setQiBlood(999L, 1, 60))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(UNKNOWN);
        assertThatThrownBy(() -> characterVitalsEditor.restoreQiBlood(999L))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(UNKNOWN);
    }

    @Test
    void spendManaPersists() {
        Long han = prepareCharacter(Realm.QI_1);

        assertThat(characterVitalsEditor.spendMana(han, 5.5, 25)).isEqualTo(19.5);
        assertThat(characterVitalsEditor.spendMana(han, 30, 25)).isZero();
        flushAndClear();

        assertThat(characterVitalsEditor.manaLost(han)).isEqualTo(25);
    }

    @Test
    void setMana() {
        Long han = prepareCharacter(Realm.QI_1);

        characterVitalsEditor.spendMana(han, 3, 25);
        characterVitalsEditor.setMana(han, 23, 25);
        assertThat(characterVitalsEditor.manaLost(han)).isEqualTo(2);
        characterVitalsEditor.setMana(han, 30, 25);
        flushAndClear();

        assertThat(characterVitalsEditor.manaLost(han)).isZero();
    }

    /** 법력 쓰기와 기혈 쓰기는 서로의 열을 건드리지 않는다 */
    @Test
    void manaWriteDoesNotTouchQiBlood() {
        Long han = prepareCharacter(Realm.QI_1);
        characterVitalsEditor.takeDamage(han, 12.5, 60);

        characterVitalsEditor.spendMana(han, 5, 25);
        characterVitalsEditor.setMana(han, 21, 25);
        flushAndClear();

        assertThat(characterVitalsEditor.qiBloodLost(han)).isEqualTo(12.5);
        assertThat(characterVitalsEditor.manaLost(han)).isEqualTo(4);

        characterVitalsEditor.restoreQiBlood(han);
        assertThat(characterVitalsEditor.manaLost(han)).isEqualTo(4);
    }

    @Test
    void unknownCharacterManaFail() {
        assertThatThrownBy(() -> characterVitalsEditor.manaLost(999L))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(UNKNOWN);
        assertThatThrownBy(() -> characterVitalsEditor.spendMana(999L, 1, 25))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(UNKNOWN);
        assertThatThrownBy(() -> characterVitalsEditor.setMana(999L, 1, 25))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(UNKNOWN);
    }

    /** 경지 지정으로 바뀐 엔티티가 있어도 기혈 쓰기가 경지를 되돌리지 않고, 엔티티 저장이 깎인 기혈을 지우지 않는다 */
    @Test
    void vitalsWriteDoesNotTouchRealm() {
        Long han = prepareCharacter(Realm.QI_1);
        characterFinder.find(han);

        characterRealmEditor.designate(han, Realm.QI_7);
        characterVitalsEditor.takeDamage(han, 30, 120);
        flushAndClear();

        assertThat(characterFinder.find(han).getRealm()).isEqualTo(Realm.QI_7);
        assertThat(characterVitalsEditor.qiBloodLost(han)).isEqualTo(30);
    }

    /** 깎이기 전에 읽어 둔 엔티티로 경지를 올려도 깎인 기혈은 그대로다 */
    @Test
    void designateDoesNotOverwriteLost() {
        Long han = prepareCharacter(Realm.QI_1);
        PlayerCharacter loaded = characterFinder.find(han);
        assertThat(loaded.getQiBloodLostAtLoad()).isNull();

        characterVitalsEditor.takeDamage(han, 30, 60);
        characterRealmEditor.designate(han, Realm.QI_7);
        flushAndClear();

        assertThat(characterFinder.find(han).getRealm()).isEqualTo(Realm.QI_7);
        assertThat(characterVitalsEditor.qiBloodLost(han)).isEqualTo(30);
    }

    private Long prepareCharacter(Realm realm) {
        PlayerCharacter character = createCharacter(1L, "한백", 10L, spiritRoot());
        character.changeRealm(realm);
        entityManager.persist(character);
        flushAndClear();
        return requireNonNull(character.getId());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
