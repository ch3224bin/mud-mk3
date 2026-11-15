package mudmk3.application.game;

import lombok.RequiredArgsConstructor;
import mudmk3.application.cultivation.provided.CultivationFinder;
import mudmk3.application.game.required.AbilityTimersRepository;
import mudmk3.application.game.required.GameMessage;
import mudmk3.application.game.required.GameMessage.SystemMessage;
import mudmk3.application.game.required.MobInstanceRepository;
import mudmk3.application.game.required.PlayerCombatRepository;
import mudmk3.domain.combat.AbilityTimers;
import mudmk3.domain.combat.MobInstance;
import mudmk3.domain.combat.PlayerCombat;
import mudmk3.domain.cultivation.StatSheet;
import mudmk3.domain.game.Command;
import mudmk3.domain.game.OnlinePlayer;
import mudmk3.domain.item.ItemQuery;
import mudmk3.domain.technique.Ability;
import mudmk3.domain.technique.CostStat;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * {@code <신통> [대상]}(command.md {@code ### 신통 쓰기}): 이름 해석 → 검사(처음 걸린 것만 {@code error}, 소모·대기시간 없음) →
 * 발동({@link AbilityCaster}). {@link GameService} 가 게임 락과 트랜잭션 안에서 부른다
 */
@Component
@RequiredArgsConstructor
class AbilityCommandHandler {

    private final AbilityResolver abilityResolver;
    private final AbilityCaster abilityCaster;
    private final CombatActions combatActions;
    private final PlayerCombatRepository playerCombatRepository;
    private final MobInstanceRepository mobInstanceRepository;
    private final AbilityTimersRepository abilityTimersRepository;
    private final CultivationFinder cultivationFinder;
    private final AbilityTexts abilityTexts;

    /**
     * 이름 해석 2~4 → 검사 1 쓸 수 없는 꼴 → 3 대상 → 4 영력 0(오행 신통, F21: 연체가 아닌 신통) → 5 공통 대기 → 6 재사용 대기 →
     * 7 법력 → 발동. 검사 2(시전 중)와 시전 시간이 있는 신통의 시전은 Task 7 이 잇는다 — 지금은 시전 시간이 있는 신통을 검사 뒤
     * 쓸 수 없는 신통으로 거절한다
     */
    void use(OnlinePlayer player, Command.UseAbility command, Instant now, BiConsumer<String, GameMessage> send) {
        String sessionId = player.getSessionId();
        Long characterId = player.getCharacterId();
        Optional<LearnedAbility> found = abilityResolver.findLearned(characterId, command.name());
        if (found.isEmpty()) {
            String text = abilityResolver.definedName(command.name())
                    .map(abilityTexts::notLearned)
                    .orElse(GameService.UNKNOWN);
            send.accept(sessionId, SystemMessage.error(text));
            return;
        }
        LearnedAbility learned = found.get();
        Ability ability = learned.ability();
        if (!usable(learned)) {
            send.accept(sessionId, SystemMessage.error(abilityTexts.unusable(ability.getName())));
            return;
        }
        MobInstance target;
        switch (findTarget(player, command)) {
            case TargetLookup.Rejected rejected -> {
                send.accept(sessionId, SystemMessage.error(rejected.text()));
                return;
            }
            case TargetLookup.Found targetFound -> target = targetFound.mob();
        }
        Optional<String> rejected = check(characterId, learned, now);
        if (rejected.isPresent()) {
            send.accept(sessionId, SystemMessage.error(rejected.get()));
            return;
        }
        if (ability.getCastSeconds().signum() > 0) {
            // 시전(시전 시간 > 0)은 Task 7 이 잇는다
            send.accept(sessionId, SystemMessage.error(abilityTexts.unusable(ability.getName())));
            return;
        }
        abilityCaster.cast(player, learned, target, true, now, send);
    }

    /** 검사 1: 6차에 쓸 수 있는 꼴이고 지금 단계의 단계별 값이 있다 */
    private static boolean usable(LearnedAbility learned) {
        return learned.ability().usableInGame() && learned.ability().stagePower(learned.stage()).isPresent();
    }

    /**
     * 검사 3: 인자가 없으면 교전 중일 때만 노리는 상대에게, 있으면 5차 {@code 공격} 과 같은 찾기·거절 문구
     */
    private TargetLookup findTarget(OnlinePlayer player, Command.UseAbility command) {
        ItemQuery query = command.target();
        if (query != null) {
            return combatActions.findTarget(player, query);
        }
        return playerCombatRepository.find(player.getCharacterId())
                .flatMap(PlayerCombat::currentTarget)
                .flatMap(mobInstanceRepository::findById)
                .filter(mob -> combatActions.inCombatTogether(player, mob))
                .<TargetLookup>map(TargetLookup.Found::new)
                .orElseGet(() -> new TargetLookup.Rejected(abilityTexts.noTarget(command.name())));
    }

    /** 검사 4~7. 걸린 문구 */
    private Optional<String> check(Long characterId, LearnedAbility learned, Instant now) {
        Ability ability = learned.ability();
        StatSheet stats = cultivationFinder.stats(characterId);
        if (stats.spirit() == 0 && !ability.isBodyRefining()) {
            return Optional.of(AbilityTexts.NO_SPIRIT);
        }
        Optional<AbilityTimers> timers = abilityTimersRepository.find(characterId);
        Optional<Duration> global = timers.flatMap(t -> t.globalRemaining(now));
        if (global.isPresent()) {
            return Optional.of(abilityTexts.globalCooldown(global.get()));
        }
        Optional<Duration> cooldown = timers.flatMap(t -> t.cooldownRemaining(learned.key(), now));
        if (cooldown.isPresent()) {
            return Optional.of(abilityTexts.cooldown(ability.getName(), cooldown.get()));
        }
        if (ability.getCostStat() == CostStat.MANA
                && stats.manaCurrent() < ability.getCostAmount().doubleValue()) {
            return Optional.of(abilityTexts.notEnoughMana(ability.getCostAmount(), stats.manaCurrent()));
        }
        return Optional.empty();
    }
}
