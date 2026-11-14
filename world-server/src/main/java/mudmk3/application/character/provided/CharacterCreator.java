package mudmk3.application.character.provided;

import mudmk3.domain.character.PlayerCharacter;
import mudmk3.domain.character.SpiritRoot;

/** 캐릭터를 만들고 영근을 정하는 기능을 제공한다 */
public interface CharacterCreator {

    /**
     * 그 계정의 캐릭터를 영근 없이 시작 방에 만든다(영근은 다음 접속 때 고른다).
     * 검사 순서는 {@link #create(Long, String, SpiritRoot)} 와 같다
     */
    PlayerCharacter create(Long accountId, String name);

    /**
     * 그 계정의 캐릭터를 범인·그 영근으로 시작 방에 만든다.
     * 검사 순서: 이름 규칙(IllegalArgumentException), 계정에 이미 있음(CharacterAlreadyExistsException),
     * 이름 중복 — 캐릭터나 NPC 이름(DuplicateCharacterNameException), 시작 방 없음(WorldNotReadyException)
     */
    PlayerCharacter create(Long accountId, String name, SpiritRoot spiritRoot);

    /**
     * 영근이 없는 캐릭터의 영근을 정한다.
     * 없는 캐릭터면 IllegalArgumentException, 이미 영근이 있으면 IllegalStateException("영근은 이미 정했습니다")
     */
    void chooseSpiritRoot(Long characterId, SpiritRoot spiritRoot);
}
