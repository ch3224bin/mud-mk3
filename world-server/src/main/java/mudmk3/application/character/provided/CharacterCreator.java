package mudmk3.application.character.provided;

import mudmk3.domain.character.PlayerCharacter;

/** 캐릭터를 만드는 기능을 제공한다 */
public interface CharacterCreator {

    /**
     * 그 계정의 캐릭터를 시작 방에 만든다.
     * 검사 순서: 이름 규칙(IllegalArgumentException), 계정에 이미 있음(CharacterAlreadyExistsException),
     * 이름 중복 — 캐릭터나 NPC 이름(DuplicateCharacterNameException), 시작 방 없음(WorldNotReadyException)
     */
    PlayerCharacter create(Long accountId, String name);
}
