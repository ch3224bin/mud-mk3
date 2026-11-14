package mudmk3.application.technique.provided;

/** 익힌 캐릭터·담긴 물건이 있는 공법을 지우거나, 익힌 단계 아래로 단계 수를 줄이려 했다 */
public class TechniqueInUseException extends RuntimeException {

    public TechniqueInUseException(String message) {
        super(message);
    }
}
