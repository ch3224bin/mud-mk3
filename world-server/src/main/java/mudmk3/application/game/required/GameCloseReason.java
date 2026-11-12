package mudmk3.application.game.required;

/** 서버가 연결을 닫는 까닭. 어댑터가 닫기 코드로 바꾼다(QUIT 1000, REPLACED 4409, WORLD_NOT_READY 1013) */
public enum GameCloseReason {
    /** 종료 명령 */
    QUIT,
    /** 같은 계정이 다른 곳에서 접속해 밀려남 */
    REPLACED,
    /** 시작 방이 없어 세계가 열리지 않음 */
    WORLD_NOT_READY
}
