package mudmk3.application.game;

import java.util.List;

/** 서버 틱 하나의 결과: 이번 틱에 진 캐릭터(진 순서). 시작 방으로 옮기기는 {@link GameService} 가 한다(D86·D88) */
record CombatOutcome(List<Long> defeatedCharacterIds) {

    CombatOutcome {
        defeatedCharacterIds = List.copyOf(defeatedCharacterIds);
    }
}
