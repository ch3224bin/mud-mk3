package mudmk3.application.item.provided;

/** 쓰이는 아이템 종류를 지우거나 종류·저물대 등급을 바꾸려 했다 */
public class ItemTypeInUseException extends RuntimeException {

    public ItemTypeInUseException(String message) {
        super(message);
    }
}
