package mudmk3.application.game.provided;

/** 접속 중인(그 계정의 게임 연결이 있는) 캐릭터를 지우려 했다 */
public class CharacterOnlineException extends RuntimeException {

    public CharacterOnlineException(String message) {
        super(message);
    }
}
