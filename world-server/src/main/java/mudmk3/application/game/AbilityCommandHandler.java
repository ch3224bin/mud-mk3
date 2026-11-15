package mudmk3.application.game;

import lombok.RequiredArgsConstructor;
import mudmk3.application.cultivation.provided.CultivationFinder;
import mudmk3.application.game.required.AbilityTimersRepository;
import mudmk3.application.game.required.GameMessage;
import mudmk3.application.game.required.GameMessage.SystemMessage;
import mudmk3.application.game.required.MobInstanceRepository;
import mudmk3.application.game.required.PlayerCombatRepository;
import mudmk3.domain.combat.AbilityTimers;
import mudmk3.domain.combat.Casting;
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
 * 발동({@link AbilityCaster}) 또는 시전 시작, 그리고 {@code 중지}(command.md {@code ### 시전과 `중지`}).
 * {@link GameService} 가 게임 락과 트랜잭션 안에서 부른다
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
     * 이름 해석 2~4 → 검사 1 쓸 수 없는 꼴 → 2 시전 중 → 3 대상 → 4 영력 0(오행 신통, F21: 연체가 아닌 신통) → 5 공통 대기 →
     * 6 재사용 대기 → 7 법력 → 시전 시간이 0이면 바로 발동, 아니면 시전 시작(D113)
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
        if (isCasting(characterId)) {
            send.accept(sessionId, SystemMessage.error(AbilityTexts.CASTING));
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
            startCasting(player, learned, target, now, send);
            return;
        }
        abilityCaster.cast(player, learned, target, true, now, send);
    }

    /**
     * {@code 중지}: 시전 중이면 지우고 {@code 펼치던 술법을 거두었습니다.}({@code combat}), 아니면 {@code 펼치는 술법이 없습니다.}
     * ({@code error}). 공통 대기와 상관없고, 이미 걸린 공통 대기는 남는다
     */
    void stop(OnlinePlayer player, BiConsumer<String, GameMessage> send) {
        Optional<AbilityTimers> timers = abilityTimersRepository.find(player.getCharacterId())
                .filter(t -> t.casting().isPresent());
        if (timers.isEmpty()) {
            send.accept(player.getSessionId(), SystemMessage.error(AbilityTexts.NOT_CASTING));
            return;
        }
        timers.get().clearCasting();
        abilityTimersRepository.save(timers.get());
        send.accept(player.getSessionId(), SystemMessage.combat(AbilityTexts.STOPPED));
    }

    /** 시전 중인가(검사 2, 이동 막힘) */
    boolean isCasting(Long characterId) {
        return abilityTimersRepository.find(characterId).flatMap(AbilityTimers::casting).isPresent();
    }

    /**
     * 시전 시작: 공통 대기시간을 지금 걸고, 시전 시간 뒤에 발동할 시전(공법 id·신통 이름·몹 id·시전 시작 순번)을 둔다.
     * 소모·재사용 대기·교전 시작은 발동할 때다({@link AbilityCaster#fire})
     */
    private void startCasting(OnlinePlayer player, LearnedAbility learned, MobInstance target, Instant now,
                              BiConsumer<String, GameMessage> send) {
        Long characterId = player.getCharacterId();
        Ability ability = learned.ability();
        AbilityTimers timers = abilityTimersRepository.find(characterId).orElseGet(() -> new AbilityTimers(characterId));
        timers.startGlobal(now);
        timers.startCasting(Casting.start(learned.key(), target.getId(), ability.getCastSeconds(), now,
                combatActions.nextCastingSeq()));
        abilityTimersRepository.save(timers);
        send.accept(player.getSessionId(),
                SystemMessage.combat(abilityTexts.castStarted(ability.getName(), ability.getCastSeconds())));
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
