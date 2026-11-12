package mudmk3.application.character.provided;

import mudmk3.domain.character.PlayerCharacter;

import java.util.Optional;

/** 캐릭터를 찾는 기능을 제공한다 */
public interface CharacterFinder {

    /** 그 계정의 캐릭터. 아직 만들지 않았으면 비어 있다 */
    Optional<PlayerCharacter> findByAccountId(Long accountId);

    /** 그 이름의 캐릭터가 이미 있는가. 이름 규칙에 맞지 않는 값이면 false */
    boolean isNameTaken(String name);
}
