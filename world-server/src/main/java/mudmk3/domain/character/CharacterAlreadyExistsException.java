package mudmk3.domain.character;

/** 계정에 이미 캐릭터가 있다(계정당 하나) */
public class CharacterAlreadyExistsException extends RuntimeException {

    public CharacterAlreadyExistsException(String message) {
        super(message);
    }
}
