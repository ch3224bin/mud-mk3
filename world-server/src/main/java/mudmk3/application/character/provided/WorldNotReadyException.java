package mudmk3.application.character.provided;

/** 시작 방이 정해지지 않아 캐릭터가 세계에 들어갈 수 없다 */
public class WorldNotReadyException extends RuntimeException {

    public WorldNotReadyException(String message) {
        super(message);
    }
}
