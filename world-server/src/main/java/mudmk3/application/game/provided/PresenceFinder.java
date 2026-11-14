package mudmk3.application.game.provided;

import java.util.Set;

/** 지금 세계에 접속해 있는 캐릭터를 본다 */
public interface PresenceFinder {

    /** 세계에 들어와 있는 캐릭터 id. 이름·영근을 고르는 중인 연결은 아직 세계에 없으므로 들지 않는다 */
    Set<Long> onlineCharacterIds();
}
