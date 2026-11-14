package mudmk3.domain.technique;

/** 같은 이름의 공법이 이미 있다(영문 대소문자 무시) */
public class DuplicateTechniqueNameException extends RuntimeException {

    public DuplicateTechniqueNameException(String message) {
        super(message);
    }
}
