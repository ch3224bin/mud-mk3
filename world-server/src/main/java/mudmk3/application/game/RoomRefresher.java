package mudmk3.application.game;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 이번 처리(명령 하나·틱 하나)에서 대상 목록(몹·시체·바닥 물건, 짐이면 사람)이 바뀐 방을 표시한 순서대로 모은다.
 * {@link GameService} 가 처리 끝(커밋 전)에 방마다 한 번 조용한 방 상태({@code room} {@code quiet: true})를 보낸다
 * (command.md {@code ### 방 상태 조용히 보내기}). {@link GameService} 의 게임 락 안에서만 쓴다
 */
@Component
class RoomRefresher {

    private final Set<Long> marked = new LinkedHashSet<>();

    /** 그 방의 대상 목록이 바뀌었다 */
    void mark(Long roomId) {
        marked.add(roomId);
    }

    /** 표시한 방(표시한 순서)을 돌려주고 비운다 */
    Set<Long> drain() {
        Set<Long> drained = new LinkedHashSet<>(marked);
        marked.clear();
        return drained;
    }

    /** 표시를 모두 지운다(실패한 처리가 남긴 표시를 다음 처리가 시작할 때) */
    void clear() {
        marked.clear();
    }
}
