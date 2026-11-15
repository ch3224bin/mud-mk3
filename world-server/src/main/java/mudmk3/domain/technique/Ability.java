package mudmk3.domain.technique;

import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import mudmk3.domain.AbstractEntity;
import mudmk3.domain.shared.Element;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * 신통: 공법 하나에 딸린다. 이름은 한 공법 안에서만 유일하다. 속성은 오행 0~5개 또는 연체(이때 오행은 비어 있음).
 * 만들기와 검사는 {@link Technique} 를 거친다(정의 순서·공법 단계 수·계열이 필요하다).
 * 소모 양·시간은 저장한 값과 같은 모양이 되게 scale 1 로 둔다({@code 5.0}).
 * 6차: 공격 신통은 배우는 단계부터 공법의 마지막 단계까지 단계별 값(기본 위력 scale 2, 계수 scale 3)을 가지고,
 * 모든 신통은 이펙트 문장 1~5개를 가진다
 */
@Entity
@Getter
@ToString(callSuper = true, exclude = {"technique", "elements", "effectLines", "stagePowers"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ability extends AbstractEntity {

    private static final Pattern NAME_PATTERN = Pattern.compile("^\\S{1,12}$");
    private static final BigDecimal COST_AMOUNT_MAX = BigDecimal.valueOf(10_000);
    private static final BigDecimal CAST_MAX = BigDecimal.valueOf(60);
    private static final BigDecimal COOLDOWN_MIN = new BigDecimal("0.1");
    private static final BigDecimal COOLDOWN_MAX = BigDecimal.valueOf(3600);
    private static final BigDecimal BASE_POWER_MAX = BigDecimal.valueOf(10_000);
    private static final BigDecimal COEFFICIENT_MAX = BigDecimal.valueOf(100);
    private static final int EFFECT_LINES_MAX = 5;
    private static final int EFFECT_LINE_MAX_LENGTH = 100;

    /** 이 신통이 딸린 공법 */
    private Technique technique;

    private String name;

    private int learnStage;

    private AbilityKind kind;

    @Getter(AccessLevel.NONE)
    private Set<Element> elements = new HashSet<>();

    private boolean bodyRefining;

    private AbilityTarget target;

    private CostStat costStat;

    private BigDecimal costAmount;

    private boolean costPerTick;

    private BigDecimal castSeconds;

    private BigDecimal cooldownSeconds;

    private UsableWhen usableWhen;

    /** 쓸 때 하나가 무작위로 보이는 문장(입력 순서) */
    @Getter(AccessLevel.NONE)
    private List<String> effectLines = new ArrayList<>();

    /** 공격 신통의 단계별 값(단계 오름차순). 공격이 아니면 비어 있다 */
    @Getter(AccessLevel.NONE)
    private List<AbilityStagePower> stagePowers = new ArrayList<>();

    /** 이름 규칙(띄어쓰기 없이 1~12자). 한 공법 안 중복보다 먼저 본다 */
    static void ensureValidName(String name) {
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("신통 이름은 띄어쓰기 없이 1~12자로 정하십시오.");
        }
    }

    /**
     * 이름 규칙·중복 다음의 규칙을 정해진 순서로 검사하고 공법에 딸린 신통을 만든다:
     * 예약어 → 배우는 단계 → 같은 오행 두 번 → 연체 신통에 오행 → 연체술 공법의 법력 소모 → 소수 자리 →
     * 소모 없음인데 양 → 소모가 있는데 양 → 소모 양 상한(10,000) → 시전 시간 → 재사용 대기시간 →
     * 단계별 값(공격이 아님 → 행 맞음 → 단계 낮은 것부터 소수 자리 → 기본 위력 범위 → 계수 범위) →
     * 이펙트 문장(수 → 검사마다 모든 줄을 입력 순서로: 길이 → 자리표시 → {@code {대상}} → 같은 문장)
     */
    static Ability create(Technique technique, AbilitySaveInfo info, int stageCount, boolean bodyRefiningTechnique) {
        String name = info.name();
        if (ReservedWords.contains(name)) {
            throw new IllegalArgumentException("명령어와 같은 이름은 쓸 수 없습니다: " + name);
        }
        if (info.learnStage() < 1 || info.learnStage() > stageCount) {
            throw new IllegalArgumentException("배우는 단계는 1성~" + stageCount + "성 가운데로 정하십시오: " + name);
        }
        Technique.ensureNoDuplicateElement(info.elements());
        if (info.bodyRefining() && !info.elements().isEmpty()) {
            throw new IllegalArgumentException("연체 신통에는 오행 속성을 함께 둘 수 없습니다: " + name);
        }
        if (bodyRefiningTechnique && info.costStat() == CostStat.MANA) {
            throw new IllegalArgumentException("연체술 공법의 신통은 법력을 쓰지 않습니다: " + name);
        }
        if (hasMoreThanOneDecimal(info.costAmount()) || hasMoreThanOneDecimal(info.castSeconds())
                || hasMoreThanOneDecimal(info.cooldownSeconds())) {
            throw new IllegalArgumentException("소모 양·시간은 소수 첫째 자리까지 적으십시오: " + name);
        }
        if (info.costStat() == CostStat.NONE && info.costAmount().signum() != 0) {
            throw new IllegalArgumentException("소모가 없으면 양을 0으로 두십시오: " + name);
        }
        if (info.costStat() != CostStat.NONE && info.costAmount().signum() <= 0) {
            throw new IllegalArgumentException("소모가 있으면 양을 0보다 크게 정하십시오: " + name);
        }
        if (info.costAmount().compareTo(COST_AMOUNT_MAX) > 0) {
            throw new IllegalArgumentException("소모 양은 0.1~10,000으로 정하십시오: " + name);
        }
        if (info.castSeconds().signum() < 0 || info.castSeconds().compareTo(CAST_MAX) > 0) {
            throw new IllegalArgumentException("시전 시간은 0~60초로 정하십시오: " + name);
        }
        if (info.cooldownSeconds().compareTo(COOLDOWN_MIN) < 0 || info.cooldownSeconds().compareTo(COOLDOWN_MAX) > 0) {
            throw new IllegalArgumentException("재사용 대기시간은 0.1~3,600초로 정하십시오: " + name);
        }
        ensureValidStagePowers(info, stageCount);
        ensureValidEffectLines(info);

        Ability ability = new Ability();
        ability.technique = requireNonNull(technique);
        ability.name = name;
        ability.learnStage = info.learnStage();
        ability.kind = info.kind();
        ability.elements.addAll(info.elements());
        ability.bodyRefining = info.bodyRefining();
        ability.target = info.target();
        ability.costStat = info.costStat();
        ability.costAmount = info.costAmount().setScale(1);
        ability.costPerTick = info.costPerTick();
        ability.castSeconds = info.castSeconds().setScale(1);
        ability.cooldownSeconds = info.cooldownSeconds().setScale(1);
        ability.usableWhen = info.usableWhen();
        ability.effectLines.addAll(info.effectLines());
        info.stagePowers().forEach(power -> ability.stagePowers.add(new AbilityStagePower(power.stage(),
                power.basePower().setScale(2), power.coefficient().setScale(3))));
        return ability;
    }

    /** 이펙트 문장(입력 순서, 바꿀 수 없음) */
    public List<String> getEffectLines() {
        return Collections.unmodifiableList(effectLines);
    }

    /** 단계별 값(단계 오름차순, 바꿀 수 없음). 공격이 아니면 비어 있다 */
    public List<AbilityStagePower> getStagePowers() {
        return Collections.unmodifiableList(stagePowers);
    }

    /** 그 단계(공법의 n성)의 값. 공격이 아니거나 배우는 단계보다 낮으면 빈 값 */
    public Optional<AbilityStagePower> stagePower(int stage) {
        return stagePowers.stream().filter(power -> power.stage() == stage).findFirst();
    }

    /** 6차에 게임에서 쓸 수 있는 꼴: 공격 · 한 대상 · 소모 없음 또는 법력(틱마다 아님) */
    public boolean usableInGame() {
        return kind == AbilityKind.ATTACK && target == AbilityTarget.SINGLE
                && (costStat == CostStat.NONE || costStat == CostStat.MANA) && !costPerTick;
    }

    private static void ensureValidStagePowers(AbilitySaveInfo info, int stageCount) {
        String name = info.name();
        List<AbilityStagePower> powers = info.stagePowers();
        if (info.kind() != AbilityKind.ATTACK) {
            if (!powers.isEmpty()) {
                throw new IllegalArgumentException("단계별 값은 공격 신통에만 둡니다: " + name);
            }
            return;
        }
        boolean rowsMatch = powers.size() == stageCount - info.learnStage() + 1;
        for (int i = 0; rowsMatch && i < powers.size(); i++) {
            rowsMatch = powers.get(i).stage() == info.learnStage() + i;
        }
        if (!rowsMatch) {
            throw new IllegalArgumentException("단계별 값은 배우는 단계부터 마지막 단계까지 하나씩 정하십시오: " + name);
        }
        for (AbilityStagePower power : powers) {
            String where = name + " (" + power.stage() + "성)";
            if (power.basePower().stripTrailingZeros().scale() > 2
                    || power.coefficient().stripTrailingZeros().scale() > 3) {
                throw new IllegalArgumentException("기본 위력은 소수 둘째 자리, 계수는 소수 셋째 자리까지 적으십시오: " + where);
            }
            if (power.basePower().signum() < 0 || power.basePower().compareTo(BASE_POWER_MAX) > 0) {
                throw new IllegalArgumentException("기본 위력은 0~10,000으로 정하십시오: " + where);
            }
            if (power.coefficient().signum() < 0 || power.coefficient().compareTo(COEFFICIENT_MAX) > 0) {
                throw new IllegalArgumentException("신통 계수는 0~100으로 정하십시오: " + where);
            }
        }
    }

    private static void ensureValidEffectLines(AbilitySaveInfo info) {
        String name = info.name();
        List<String> lines = info.effectLines();
        if (lines.isEmpty() || lines.size() > EFFECT_LINES_MAX) {
            throw new IllegalArgumentException("이펙트 문장은 1~5개로 정하십시오: " + name);
        }
        for (String line : lines) {
            int length = line.codePointCount(0, line.length());
            if (line.indexOf('\n') >= 0 || line.indexOf('\r') >= 0 || length < 1 || length > EFFECT_LINE_MAX_LENGTH) {
                throw new IllegalArgumentException("이펙트 문장은 한 줄 1~100자로 적으십시오: " + name);
            }
        }
        for (String line : lines) {
            Optional<String> bad = EffectLine.firstBadToken(line);
            if (bad.isPresent()) {
                throw new IllegalArgumentException("쓸 수 없는 자리표시가 있습니다: " + name + " (" + bad.get() + ")");
            }
        }
        if (info.target() != AbilityTarget.SINGLE && lines.stream().anyMatch(line -> line.contains(EffectLine.TARGET))) {
            throw new IllegalArgumentException("{대상}은 한 대상에게 쓰는 신통에만 쓸 수 있습니다: " + name);
        }
        if (new HashSet<>(lines).size() != lines.size()) {
            throw new IllegalArgumentException("같은 이펙트 문장이 두 번 있습니다: " + name);
        }
    }

    /** 오행 속성(오행 순, 바꿀 수 없음). 연체 신통이면 비어 있다 */
    public List<Element> getElements() {
        return elements.stream().sorted().toList();
    }

    private static boolean hasMoreThanOneDecimal(BigDecimal value) {
        return value.stripTrailingZeros().scale() > 1;
    }
}
