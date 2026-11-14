package mudmk3.application.character.provided;

import jakarta.persistence.EntityManager;
import mudmk3.ApplicationServiceTest;
import mudmk3.domain.character.PlayerCharacter;
import mudmk3.domain.shared.Realm;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import static java.util.Objects.requireNonNull;
import static mudmk3.domain.character.CharacterFixture.createCharacter;
import static mudmk3.domain.character.CharacterFixture.spiritRoot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ApplicationServiceTest
@RecordApplicationEvents
record CharacterRealmEditorTest(CharacterRealmEditor characterRealmEditor, CharacterFinder characterFinder,
                                EntityManager entityManager) {

    @Test
    void designatePublishesEvent(ApplicationEvents events) {
        Long han = prepareCharacter(Realm.QI_1);

        characterRealmEditor.designate(han, Realm.QI_7);
        flushAndClear();

        assertThat(characterFinder.find(han).getRealm()).isEqualTo(Realm.QI_7);
        assertThat(events.stream(RealmChangedEvent.class))
                .containsExactly(new RealmChangedEvent(han, Realm.QI_1, Realm.QI_7));
    }

    @Test
    void designateSameRealmNoEvent(ApplicationEvents events) {
        Long han = prepareCharacter(Realm.QI_1);

        characterRealmEditor.designate(han, Realm.QI_1);

        assertThat(characterFinder.find(han).getRealm()).isEqualTo(Realm.QI_1);
        assertThat(events.stream(RealmChangedEvent.class)).isEmpty();
    }

    @Test
    void designateUnknownFail() {
        assertThatThrownBy(() -> characterRealmEditor.designate(999L, Realm.QI_1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("캐릭터를 찾을 수 없습니다. id: 999");
    }

    @Test
    void initiateOnlyMortal(ApplicationEvents events) {
        Long mortal = prepareCharacter(Realm.MORTAL);
        characterRealmEditor.initiate(mortal);
        flushAndClear();

        assertThat(characterFinder.find(mortal).getRealm()).isEqualTo(Realm.QI_1);
        assertThat(events.stream(RealmChangedEvent.class)).isEmpty();
    }

    @Test
    void initiateNonMortalKeepsRealm() {
        PlayerCharacter character = createCharacter(2L, "여림", 10L, spiritRoot());
        character.changeRealm(Realm.QI_3);
        entityManager.persist(character);

        characterRealmEditor.initiate(requireNonNull(character.getId()));
        flushAndClear();

        assertThat(characterFinder.find(character.getId()).getRealm()).isEqualTo(Realm.QI_3);
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
