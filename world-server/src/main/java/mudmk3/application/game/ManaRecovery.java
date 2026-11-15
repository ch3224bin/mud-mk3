package mudmk3.application.game;

import lombok.RequiredArgsConstructor;
import mudmk3.application.character.provided.CharacterVitalsEditor;
import mudmk3.application.cultivation.provided.CultivationFinder;
import mudmk3.application.game.required.PlayerCombatRepository;
import mudmk3.application.game.required.PresenceRepository;
import mudmk3.domain.character.Mana;
import mudmk3.domain.combat.PlayerCombat;
import mudmk3.domain.game.OnlinePlayer;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 법력 자연 회복(command.md {@code ### 법력 회복}, D104). {@link CombatEngine#tick} 이 배치 맞추기 다음·행동 앞에서 부른다.
 * 접속해 세계에 있는 캐릭터마다 회복 기준 시각을 먼저 지금으로 옮기고(F16, 흐른 초는 0~1초로 자름), 깎인 법력이 있을 때만
 * 법력 최대치 × 0.5% × 흐른 초를 채운다. 교전 중에도 채우고, 최대치가 0(범인)이면 아무것도 하지 않는다. 알림은 없다
 */
@Component
@RequiredArgsConstructor
class ManaRecovery {

    private final PresenceRepository presenceRepository;
    private final PlayerCombatRepository playerCombatRepository;
    private final CharacterVitalsEditor characterVitalsEditor;
    private final CultivationFinder cultivationFinder;

    void recover(Instant now) {
        for (OnlinePlayer player : presenceRepository.findAll()) {
            Long characterId = player.getCharacterId();
            PlayerCombat combat = playerCombatRepository.find(characterId)
                    .orElseGet(() -> new PlayerCombat(characterId));
            double seconds = combat.manaRegenSeconds(now);
            playerCombatRepository.save(combat);
            if (seconds <= 0 || characterVitalsEditor.manaLost(characterId) <= 0) continue;
            int max = cultivationFinder.stats(characterId).mana();
            if (max == 0) continue;
            characterVitalsEditor.recoverMana(characterId, max * Mana.REGEN_RATE_PER_SECOND * seconds);
        }
    }
}
