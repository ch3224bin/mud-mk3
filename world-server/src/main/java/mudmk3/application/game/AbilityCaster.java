package mudmk3.application.game;

import lombok.RequiredArgsConstructor;
import mudmk3.application.character.provided.CharacterFinder;
import mudmk3.application.character.provided.CharacterVitalsEditor;
import mudmk3.application.cultivation.provided.CultivationFinder;
import mudmk3.application.cultivation.provided.InsightEditor;
import mudmk3.application.game.required.AbilityTimersRepository;
import mudmk3.application.game.required.GameMessage;
import mudmk3.application.game.required.GameMessage.SystemMessage;
import mudmk3.application.game.required.MobInstanceRepository;
import mudmk3.application.game.required.PlayerCombatRepository;
import mudmk3.application.game.required.PresenceRepository;
import mudmk3.domain.combat.AbilityPower;
import mudmk3.domain.combat.AbilityTimers;
import mudmk3.domain.combat.AttackNature;
import mudmk3.domain.combat.Casting;
import mudmk3.domain.combat.CombatRules;
import mudmk3.domain.combat.MobHit;
import mudmk3.domain.combat.MobInstance;
import mudmk3.domain.combat.PlayerCombat;
import mudmk3.domain.combat.SpiritOutput;
import mudmk3.domain.cultivation.InsightGain;
import mudmk3.domain.cultivation.InsightKind;
import mudmk3.domain.cultivation.StatSheet;
import mudmk3.domain.game.OnlinePlayer;
import mudmk3.domain.shared.Realm;
import mudmk3.domain.technique.Ability;
import mudmk3.domain.technique.AbilityStagePower;
import mudmk3.domain.technique.CostStat;
import mudmk3.domain.technique.EffectLine;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.random.RandomGenerator;

/**
 * 검사를 통과한 신통을 발동한다(command.md {@code ### 신통 쓰기} 성공 1~8, D111). 시전이 끝난 신통의 발동({@link #fire})도 같은
 * 길을 지난다. {@link GameService} 의 게임 락과 트랜잭션 안에서만 불린다
 */
@Component
@RequiredArgsConstructor
class AbilityCaster {

    private final CombatActions combatActions;
    private final AbilityResolver abilityResolver;
    private final PlayerCombatRepository playerCombatRepository;
    private final MobInstanceRepository mobInstanceRepository;
    private final AbilityTimersRepository abilityTimersRepository;
    private final PresenceRepository presenceRepository;
    private final CultivationFinder cultivationFinder;
    private final CharacterFinder characterFinder;
    private final CharacterVitalsEditor characterVitalsEditor;
    private final InsightEditor insightEditor;
    private final AbilityTexts abilityTexts;
    private final RandomGenerator random;

    /**
     * 신통을 {@code target} 에게 발동한다: 1 교전 시작(아무와도 교전하지 않던 때면 첫 자동 공격은 3초 뒤)·노리는 상대 →
     * 2 소모 전 법력으로 발휘 비율 → 3 법력 소모·재사용 대기({@code startGlobal} 이면 공통 대기도) → 4 위력 → 5 피해 →
     * 6 이펙트 줄 → 7 깨달음(한 상대 상한) → 8 쓰러졌으면 처치(5차, 상한 지우기는 그 끝에서 — F1)
     */
    void cast(OnlinePlayer caster, LearnedAbility learned, MobInstance target, boolean startGlobal, Instant now,
              BiConsumer<String, GameMessage> send) {
        Long characterId = caster.getCharacterId();
        Ability ability = learned.ability();
        AbilityStagePower stagePower = ability.stagePower(learned.stage())
                .orElseThrow(() -> new IllegalStateException("그 단계의 단계별 값이 없습니다: " + ability.getName()));

        engage(caster, target, now, send);

        StatSheet stats = cultivationFinder.stats(characterId);
        double spiritRatio = SpiritOutput.ratio(stats.manaCurrent(), stats.mana());

        boolean usesMana = ability.getCostStat() == CostStat.MANA;
        double manaAfter = usesMana
                ? characterVitalsEditor.spendMana(characterId, ability.getCostAmount().doubleValue(), stats.mana())
                : stats.manaCurrent();
        AbilityTimers timers = abilityTimersRepository.find(characterId).orElseGet(() -> new AbilityTimers(characterId));
        if (startGlobal) {
            timers.startGlobal(now);
        }
        timers.startCooldown(learned.key(), ability.getCooldownSeconds(), now);
        abilityTimersRepository.save(timers);

        double power = ability.isBodyRefining()
                ? AbilityPower.bodyRefining(stagePower.basePower(), stagePower.coefficient(), stats.strength())
                : AbilityPower.elemental(stagePower.basePower(), stagePower.coefficient(), stats.spirit(), spiritRatio,
                AbilityPower.aptitudeMultiplier(ability.getElements(), stats));
        Set<Long> opponents = target.opponentIds();
        MobHit hit = combatActions.strike(characterId, target, power,
                new AttackNature(ability.getElements(), ability.isBodyRefining()), now);

        String line = EffectLine.render(pickLine(ability.getEffectLines()), target.getProfile().name(), caster.getName());
        String suffix = usesMana
                ? abilityTexts.effectSuffix(hit.damage(), manaAfter, stats.mana())
                : abilityTexts.effectSuffix(hit.damage());
        send.accept(caster.getSessionId(), SystemMessage.combat(abilityTexts.effectLine(line, suffix)));

        gainInsight(caster, learned, target, send);

        if (hit.died()) {
            combatActions.onMobKilled(target, opponents, characterId, now, send);
        }
    }

    /**
     * 시전이 끝난 신통을 발동한다(D113). 그 캐릭터가 아직 이 시전 중일 때만(그 틱에 앞서 지워졌으면 아무것도 하지 않음):
     * 시전을 지우고, 세계에 없으면 조용히 끝, 신통이 없어졌거나 쓸 수 없는 꼴·단계별 값 없음, 대상이 없음·쓰러짐·다른 방이면
     * {@code 펼치던 술법이 갈 곳을 잃고 흩어진다.}, 영력 0(시전 중 범인까지 내림)에 오행 신통이면
     * {@code 영력이 없어 술법을 펼칠 수 없습니다.}(시작 검사 4와 같음, 연체 신통은 걸리지 않음), 법력이 모자라면 거절 문구 —
     * 모두 소모·재사용 대기·교전 시작 없음.
     * 아니면 {@link #cast}(공통 대기는 시작 때 걸렸으므로 다시 걸지 않음, 발휘 비율은 발동 순간의 법력)
     */
    void fire(Long characterId, Casting casting, Instant now, BiConsumer<String, GameMessage> send) {
        if (!abilityTimersRepository.clearCasting(characterId, casting::equals)) return;
        Optional<OnlinePlayer> found = presenceRepository.findByCharacterId(characterId);
        if (found.isEmpty()) return;
        OnlinePlayer caster = found.get();
        Optional<LearnedAbility> learned = abilityResolver.findLearned(characterId, casting.key())
                .filter(ability -> ability.ability().usableInGame()
                        && ability.ability().stagePower(ability.stage()).isPresent());
        Optional<MobInstance> target = mobInstanceRepository.findById(casting.mobId())
                .filter(mob -> mob.isAlive() && mob.getRoomId().equals(caster.getRoomId()));
        if (learned.isEmpty() || target.isEmpty()) {
            send.accept(caster.getSessionId(), SystemMessage.combat(AbilityTexts.SCATTERED));
            return;
        }
        Ability ability = learned.get().ability();
        StatSheet stats = cultivationFinder.stats(characterId);
        if (stats.spirit() == 0 && !ability.isBodyRefining()) {
            send.accept(caster.getSessionId(), SystemMessage.error(AbilityTexts.NO_SPIRIT));
            return;
        }
        if (ability.getCostStat() == CostStat.MANA) {
            double manaCurrent = stats.manaCurrent();
            if (manaCurrent < ability.getCostAmount().doubleValue()) {
                send.accept(caster.getSessionId(),
                        SystemMessage.error(abilityTexts.notEnoughMana(ability.getCostAmount(), manaCurrent)));
                return;
            }
        }
        cast(caster, learned.get(), target.get(), false, now, send);
    }

    /**
     * 성공 1: 대상 몹과 교전하지 않으면 교전을 시작하고(아무와도 교전하지 않던 때면 첫 자동 공격은 3초 뒤 — 이미 있던 다음 공격
     * 시각이 더 늦으면 그대로), 노리는 상대를 이 몹으로 바꾼다
     */
    private void engage(OnlinePlayer caster, MobInstance target, Instant now, BiConsumer<String, GameMessage> send) {
        Long characterId = caster.getCharacterId();
        PlayerCombat combat = playerCombatRepository.find(characterId).orElseGet(() -> new PlayerCombat(characterId));
        boolean wasFighting = combat.isFighting();
        if (!combat.getMobIds().contains(target.getId())) {
            combatActions.startEngagement(caster, combat, target, now, send);
            if (!wasFighting) {
                combat.delayAttackUntil(now.plus(CombatRules.ATTACK_INTERVAL));
            }
        }
        combat.target(target.getId());
        playerCombatRepository.save(combat);
    }

    /** 이펙트 문장 가운데 같은 확률로 하나. 하나뿐이면 난수를 쓰지 않는다 */
    private String pickLine(List<String> lines) {
        return lines.size() == 1 ? lines.getFirst() : lines.get(random.nextInt(lines.size()));
    }

    /**
     * 성공 7(command.md {@code ### 깨달음 쌓기}): G = 기본 양 × 상황 계수 → 갈래로 나눔 → 한 상대 상한까지만 → DB에 더함.
     * 상한에 처음 닿으면 {@code 같은 상대에게서는 더 깨닫는 것이 없습니다.}
     */
    private void gainInsight(OnlinePlayer caster, LearnedAbility learned, MobInstance target,
                             BiConsumer<String, GameMessage> send) {
        Long characterId = caster.getCharacterId();
        Realm casterRealm = characterFinder.find(characterId).getRealm();
        double g = InsightGain.baseAmount(learned.ability(), learned.technique().getStartRealm())
                * InsightGain.situationFactor(casterRealm, target.getProfile().realm());
        Map<InsightKind, Double> gains = InsightGain.split(g, learned.ability(), learned.technique().getElements());
        double requested = gains.values().stream().mapToDouble(Double::doubleValue).sum();
        double granted = target.grantInsight(characterId, requested);
        insightEditor.add(characterId, InsightGain.capped(gains, granted));
        boolean capReached = target.insightCapReachedFirstTime(characterId);
        mobInstanceRepository.save(target);
        if (capReached) {
            send.accept(caster.getSessionId(), SystemMessage.info(AbilityTexts.INSIGHT_CAPPED));
        }
    }
}
