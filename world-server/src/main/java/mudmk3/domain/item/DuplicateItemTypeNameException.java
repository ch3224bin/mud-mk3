package mudmk3.domain.item;

/** 같은 이름(영문 대소문자 무시)의 아이템 종류가 이미 있다 */
public class DuplicateItemTypeNameException extends RuntimeException {

    public DuplicateItemTypeNameException(String message) {
        super(message);
    }
}
