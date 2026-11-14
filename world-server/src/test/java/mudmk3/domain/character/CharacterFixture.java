package mudmk3.domain.character;

import org.jspecify.annotations.Nullable;

public class CharacterFixture {

    /** 테스트에서 흔히 쓰는 영근: 목·화 진영근(목 50, 화 50) */
    public static SpiritRoot spiritRoot() {
        return SpiritRoot.parse("목 화").orElseThrow();
    }

    public static CharacterCreateInfo createCharacterCreateInfo(Long accountId, String name, Long roomId) {
        return createCharacterCreateInfo(accountId, name, roomId, null);
    }

    public static CharacterCreateInfo createCharacterCreateInfo(Long accountId, String name, Long roomId,
                                                                @Nullable SpiritRoot spiritRoot) {
        return new CharacterCreateInfo(accountId, new CharacterName(name), roomId, spiritRoot);
    }

    public static PlayerCharacter createCharacter() {
        return createCharacter(1L, "한백", 10L);
    }

    public static PlayerCharacter createCharacter(Long accountId, String name, Long roomId) {
        return PlayerCharacter.create(createCharacterCreateInfo(accountId, name, roomId));
    }

    public static PlayerCharacter createCharacter(Long accountId, String name, Long roomId, SpiritRoot spiritRoot) {
        return PlayerCharacter.create(createCharacterCreateInfo(accountId, name, roomId, spiritRoot));
    }
}
