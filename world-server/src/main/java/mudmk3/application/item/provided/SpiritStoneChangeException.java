package mudmk3.application.item.provided;

/** 엔진이 정한 영석 종류를 고치거나 지우려 했다 */
public class SpiritStoneChangeException extends RuntimeException {

    public SpiritStoneChangeException(String message) {
        super(message);
    }
}
