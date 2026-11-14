package mudmk3.application.faction.provided;

/** 소유한 지역이 있는 성·종문을 지우려 했다 */
public class FactionInUseException extends RuntimeException {

    public FactionInUseException(String message) {
        super(message);
    }
}
