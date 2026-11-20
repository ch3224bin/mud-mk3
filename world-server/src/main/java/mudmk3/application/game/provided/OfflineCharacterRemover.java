package mudmk3.application.game.provided;

import mudmk3.application.character.provided.DeletedCharacter;

/**
 * 접속하지 않은 캐릭터를 게임 락 안에서 DB와 메모리에서 지운다.
 * <p>{@link GameConnector} 처럼 트랜잭션 밖에서 부른다. 구현은 게임 락을 잡은 뒤 자기 트랜잭션을 열어 접속 판정과 지우기를
 * 함께 하고, 돌아오면 커밋된 뒤다
 */
public interface OfflineCharacterRemover {

    /**
     * 없는 캐릭터면 {@code IllegalArgumentException("캐릭터를 찾을 수 없습니다. id: " + id)}, 그 계정의 게임 연결이 있으면
     * (영근을 고르는 중 포함) {@link CharacterOnlineException} 이고 아무것도 지우지 않는다
     */
    DeletedCharacter delete(Long characterId);
}
