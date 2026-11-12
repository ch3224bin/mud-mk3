package mudmk3.domain.character;

public class CharacterFixture {

    public static CharacterCreateInfo createCharacterCreateInfo(Long accountId, String name, Long roomId) {
        return new CharacterCreateInfo(accountId, new CharacterName(name), roomId);
    }

    public static PlayerCharacter createCharacter() {
        return createCharacter(1L, "한백", 10L);
    }

    public static PlayerCharacter createCharacter(Long accountId, String name, Long roomId) {
        return PlayerCharacter.create(createCharacterCreateInfo(accountId, name, roomId));
    }
}
