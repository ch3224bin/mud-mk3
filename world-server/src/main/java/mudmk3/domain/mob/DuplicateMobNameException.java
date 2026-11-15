package mudmk3.domain.mob;

/** 같은 이름의 몹이 이미 있다(영문 대소문자 무시) */
public class DuplicateMobNameException extends RuntimeException {

    public DuplicateMobNameException(String message) {
        super(message);
    }
}
