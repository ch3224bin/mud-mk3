package mudmk3.application.room.provided;

/** 반대 방향 출구가 이미 다른 방으로 이어져 있다 */
public class ExitConflictException extends RuntimeException {

    public ExitConflictException(String message) {
        super(message);
    }
}
