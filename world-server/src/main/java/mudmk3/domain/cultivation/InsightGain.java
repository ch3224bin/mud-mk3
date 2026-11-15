package mudmk3.domain.cultivation;

import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Realm;
import mudmk3.domain.technique.Ability;
import mudmk3.domain.technique.CostStat;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** 신통을 한 번 발동해 얻는 깨달음 계산(룰북 깨달음 절, command.md {@code ### 깨달음 쌓기}) */
public final class InsightGain {

    private static final double REFERENCE_COOLDOWN_SECONDS = 3;
    private static final double MANA_PER_SPIRIT = 5;
    private static final double REFERENCE_DIVISOR = 5;
    private static final double TECHNIQUE_SHARE = 0.5;

    private InsightGain() {
    }

    /**
     * 1회 기본 양: 법력을 쓰는 신통은 소모량, 아니면 기준 소모 = 기준 경지 법력 기준값(경지 몫 영력 × 5) ÷ 5 ×
     * (재사용 대기시간 ÷ 3초). 기준 경지는 공법 시작 경지, 범인이면 연기 1층
     */
    public static double baseAmount(Ability ability, Realm techniqueStartRealm) {
        if (ability.getCostStat() == CostStat.MANA) {
            return ability.getCostAmount().doubleValue();
        }
        Realm reference = techniqueStartRealm == Realm.MORTAL ? Realm.QI_1 : techniqueStartRealm;
        double referenceMana = reference.spirit() * MANA_PER_SPIRIT;
        return referenceMana / REFERENCE_DIVISOR
                * (ability.getCooldownSeconds().doubleValue() / REFERENCE_COOLDOWN_SECONDS);
    }

    /**
     * 상황 계수: 대상의 대경지가 낮으면 0.1, 같은 대경지에서 소경지가 3 이상 낮으면 0.5, 아니면 1.0
     * (쓰는 사람은 발동 순간 늘 전투 상태다)
     */
    public static double situationFactor(Realm caster, Realm target) {
        if (target.majorIndex() < caster.majorIndex()) return 0.1;
        if (target.majorIndex() == caster.majorIndex() && caster.minorIndex() - target.minorIndex() >= 3) return 0.5;
        return 1.0;
    }

    /**
     * 갈래마다 나눈 양: 연체 신통이면 연체에 g, 그 밖은 신통 속성마다 g ÷ 속성 수. 공법 속성 가운데 신통 속성에 없는
     * 것마다 g × 0.5 ÷ 그 수(연체 신통이면 공법 속성 전부). 0 인 갈래는 넣지 않는다
     */
    public static Map<InsightKind, Double> split(double g, Ability ability, List<Element> techniqueElements) {
        EnumMap<InsightKind, Double> result = new EnumMap<>(InsightKind.class);
        List<Element> abilityElements = ability.isBodyRefining() ? List.of() : ability.getElements();
        if (ability.isBodyRefining()) {
            add(result, InsightKind.BODY, g);
        } else {
            abilityElements.forEach(element -> add(result, InsightKind.of(element), g / abilityElements.size()));
        }
        List<Element> missing = techniqueElements.stream()
                .filter(element -> !abilityElements.contains(element))
                .toList();
        missing.forEach(element -> add(result, InsightKind.of(element), g * TECHNIQUE_SHARE / missing.size()));
        return Collections.unmodifiableMap(result);
    }

    /** 한 상대 상한: 합이 남은 상한보다 크면 갈래마다 같은 비율로 줄인다. 남은 상한이 0 이하면 빈 맵 */
    public static Map<InsightKind, Double> capped(Map<InsightKind, Double> gains, double remaining) {
        if (remaining <= 0) return Map.of();
        double sum = gains.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum <= remaining) return gains;
        double ratio = remaining / sum;
        EnumMap<InsightKind, Double> result = new EnumMap<>(InsightKind.class);
        gains.forEach((kind, amount) -> add(result, kind, amount * ratio));
        return Collections.unmodifiableMap(result);
    }

    private static void add(Map<InsightKind, Double> result, InsightKind kind, double amount) {
        if (amount > 0) {
            result.merge(kind, amount, Double::sum);
        }
    }
}
