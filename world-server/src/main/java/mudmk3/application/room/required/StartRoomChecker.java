package mudmk3.application.room.required;

/** 방이 세계의 시작 방인지 알려 준다(world 슬라이스에 기대지 않으려고 둔 포트) */
public interface StartRoomChecker {

    boolean isStartRoom(Long roomId);
}
