package mudmk3.application.zone.provided;

/** 방이 남아 있는 지역을 지우려 했다 */
public class ZoneNotEmptyException extends RuntimeException {

    public ZoneNotEmptyException(String message) {
        super(message);
    }
}
