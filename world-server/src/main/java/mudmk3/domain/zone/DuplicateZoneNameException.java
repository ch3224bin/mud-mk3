package mudmk3.domain.zone;

/** 같은 이름의 지역이 이미 있다 */
public class DuplicateZoneNameException extends RuntimeException {

    public DuplicateZoneNameException(String message) {
        super(message);
    }
}
