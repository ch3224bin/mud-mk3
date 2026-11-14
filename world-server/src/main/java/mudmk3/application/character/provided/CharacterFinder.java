package mudmk3.application.character.provided;

import mudmk3.domain.character.PlayerCharacter;

import java.util.List;
import java.util.Optional;

/** 캐릭터를 찾는 기능을 제공한다 */
public interface CharacterFinder {

    /** 그 id의 캐릭터. 없으면 IllegalArgumentException */
    PlayerCharacter find(Long characterId);

    /** 그 계정의 캐릭터. 아직 만들지 않았으면 비어 있다 */
    Optional<PlayerCharacter> findByAccountId(Long accountId);

    /** 모든 캐릭터(이름순). 영근까지 읽어 둔다 */
    List<PlayerCharacter> findAllByName();

    /** 그 이름을 캐릭터나 NPC가 이미 쓰는가(NPC는 영문 대소문자 무시). 캐릭터 이름 규칙에 맞지 않는 값이면 false */
    boolean isNameTaken(String name);
}
