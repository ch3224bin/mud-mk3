package mudmk3.domain.character;

import mudmk3.domain.shared.Realm;
import org.junit.jupiter.api.Test;

import static mudmk3.domain.character.CharacterFixture.createCharacter;
import static mudmk3.domain.character.CharacterFixture.spiritRoot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlayerCharacterTest {

    @Test
    void create() {
        PlayerCharacter character = createCharacter(1L, "한백", 10L);

        assertThat(character.getAccountId()).isEqualTo(1L);
        assertThat(character.getName()).isEqualTo(new CharacterName("한백"));
        assertThat(character.getRoomId()).isEqualTo(10L);
        assertThat(character.getRealm()).isEqualTo(Realm.MORTAL);
        assertThat(character.findSpiritRoot()).isEmpty();
    }

    @Test
    void createWithSpiritRoot() {
        PlayerCharacter character = createCharacter(1L, "한백", 10L, spiritRoot());

        assertThat(character.getRealm()).isEqualTo(Realm.MORTAL);
        assertThat(character.findSpiritRoot()).map(SpiritRoot::name).contains("목·화 진영근");
    }

    @Test
    void chooseSpiritRoot() {
        PlayerCharacter character = createCharacter();

        character.chooseSpiritRoot(SpiritRoot.parse("금 목 수").orElseThrow());

        assertThat(character.findSpiritRoot()).map(SpiritRoot::summary).contains("금 33, 목 33, 수 33");
    }

    @Test
    void chooseSpiritRootTwiceFail() {
        PlayerCharacter character = createCharacter(1L, "한백", 10L, spiritRoot());
        SpiritRoot other = SpiritRoot.parse("금").orElseThrow();

        assertThatThrownBy(() -> character.chooseSpiritRoot(other))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("영근은 이미 정했습니다");
        assertThat(character.findSpiritRoot()).map(SpiritRoot::name).contains("목·화 진영근");
    }

    @Test
    void changeRealm() {
        PlayerCharacter character = createCharacter();

        character.changeRealm(Realm.FOUNDATION_EARLY);

        assertThat(character.getRealm()).isEqualTo(Realm.FOUNDATION_EARLY);
        assertThatThrownBy(() -> character.changeRealm(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void createWithoutValuesFail() {
        CharacterName name = new CharacterName("한백");

        assertThatThrownBy(() -> PlayerCharacter.create(new CharacterCreateInfo(null, name, 10L, null)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> PlayerCharacter.create(new CharacterCreateInfo(1L, null, 10L, null)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> PlayerCharacter.create(new CharacterCreateInfo(1L, name, null, null)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void moveTo() {
        PlayerCharacter character = createCharacter();

        character.moveTo(20L);

        assertThat(character.getRoomId()).isEqualTo(20L);
    }

    @Test
    void moveToNullFail() {
        PlayerCharacter character = createCharacter();

        assertThatThrownBy(() -> character.moveTo(null)).isInstanceOf(NullPointerException.class);
    }
}
