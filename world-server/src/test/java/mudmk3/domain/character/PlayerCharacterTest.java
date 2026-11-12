package mudmk3.domain.character;

import org.junit.jupiter.api.Test;

import static mudmk3.domain.character.CharacterFixture.createCharacter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlayerCharacterTest {

    @Test
    void create() {
        PlayerCharacter character = createCharacter(1L, "한백", 10L);

        assertThat(character.getAccountId()).isEqualTo(1L);
        assertThat(character.getName()).isEqualTo(new CharacterName("한백"));
        assertThat(character.getRoomId()).isEqualTo(10L);
    }

    @Test
    void createWithoutValuesFail() {
        CharacterName name = new CharacterName("한백");

        assertThatThrownBy(() -> PlayerCharacter.create(new CharacterCreateInfo(null, name, 10L)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> PlayerCharacter.create(new CharacterCreateInfo(1L, null, 10L)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> PlayerCharacter.create(new CharacterCreateInfo(1L, name, null)))
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
