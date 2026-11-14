package mudmk3.domain.faction;

/** 같은 이름의 성·종문이 이미 있다(성과 종문을 통틀어) */
public class DuplicateFactionNameException extends RuntimeException {

    public DuplicateFactionNameException(String message) {
        super(message);
    }
}
