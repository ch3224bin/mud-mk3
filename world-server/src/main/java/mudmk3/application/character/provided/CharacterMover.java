package mudmk3.application.character.provided;

import java.util.List;

/** 캐릭터의 위치를 옮기고 저장하는 기능을 제공한다 */
public interface CharacterMover {

    /** 캐릭터를 그 방으로 옮기고 바로 저장한다. 방이 있는지는 부르는 쪽이 확인한다 */
    void moveTo(Long characterId, Long roomId);

    /** 한 방에 있는 캐릭터를 모두 다른 방으로 옮기고 옮긴 캐릭터 id들을 돌려준다 */
    List<Long> relocateAll(Long fromRoomId, Long toRoomId);
}
