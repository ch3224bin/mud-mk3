package mudmk3.application.mob.provided;

/** 배치된 몹을 지우려 했다 */
public class MobTypeInUseException extends RuntimeException {

    public MobTypeInUseException(String message) {
        super(message);
    }
}
