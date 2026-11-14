package mudmk3.domain.npc;

/** 같은 이름의 NPC가 이미 있다(영문 대소문자 무시) */
public class DuplicateNpcNameException extends RuntimeException {

    public DuplicateNpcNameException(String message) {
        super(message);
    }
}
