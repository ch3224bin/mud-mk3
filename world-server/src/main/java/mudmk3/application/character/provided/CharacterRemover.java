package mudmk3.application.character.provided;

/**
 * 캐릭터와 캐릭터를 참조하는 행(소지품·영근, {@link CharacterDeletedEvent} 로 익힌 공법·깨달음)을 함께 지운다. 계정은 남는다.
 * <p>게임 락을 모르고 접속 여부도 보지 않는다. 게임 락 밖에서 부르지 않는다 — 관리 툴은
 * {@code OfflineCharacterRemover}(게임 락 안에서 접속 판정 뒤 이것을 부름)를 쓴다
 */
public interface CharacterRemover {

    /** 없는 캐릭터면 {@code IllegalArgumentException("캐릭터를 찾을 수 없습니다. id: " + id)} */
    DeletedCharacter delete(Long characterId);
}
