package mudmk3.domain.character;

/** 같은 이름의 캐릭터가 이미 있다 */
public class DuplicateCharacterNameException extends RuntimeException {

    public DuplicateCharacterNameException(String message) {
        super(message);
    }
}
