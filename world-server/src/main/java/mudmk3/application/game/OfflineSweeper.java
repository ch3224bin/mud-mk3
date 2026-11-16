package mudmk3.application.game;

import lombok.RequiredArgsConstructor;
import mudmk3.application.game.required.AbilityTimersRepository;
import mudmk3.application.game.required.PlayerCombatRepository;
import mudmk3.application.game.required.PresenceRepository;
import mudmk3.domain.combat.AbilityTimers;
import mudmk3.domain.combat.PlayerCombat;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 틱 끝 정리(L-D4, platform.md 5차 메모리): 세계에 없는 캐릭터의 교전 기록은 교전 상대·다시 차기 예약이 없고 다음 공격
 * 시각이 지나면, 대기시간 기록은 시전이 없고 공통·재사용 대기시간이 모두 끝나면 지운다. 다시 들어오면 새 기록과 같다.
 * {@link CombatEngine#tick} 이 마지막에 부른다. {@link GameService} 의 게임 락과 트랜잭션 안에서만 불린다
 */
@Component
@RequiredArgsConstructor
class OfflineSweeper {

    private final PlayerCombatRepository playerCombatRepository;
    private final AbilityTimersRepository abilityTimersRepository;
    private final PresenceRepository presenceRepository;

    void sweep(Instant now) {
        for (PlayerCombat combat : playerCombatRepository.findAll()) {
            if (offline(combat.getCharacterId()) && combat.isIdle(now)) {
                playerCombatRepository.delete(combat.getCharacterId());
            }
        }
        for (AbilityTimers timers : abilityTimersRepository.findAll()) {
            if (offline(timers.getCharacterId()) && timers.isIdle(now)) {
                abilityTimersRepository.delete(timers.getCharacterId());
            }
        }
    }

    private boolean offline(Long characterId) {
        return presenceRepository.findByCharacterId(characterId).isEmpty();
    }
}
