package mudmk3.domain.technique;

import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import mudmk3.domain.AbstractEntity;
import mudmk3.domain.shared.Element;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * 신통: 공법 하나에 딸린다. 이름은 한 공법 안에서만 유일하다. 속성은 오행 0~5개 또는 연체(이때 오행은 비어 있음).
 * 만들기와 검사는 {@link Technique} 를 거친다(정의 순서·공법 단계 수·계열이 필요하다).
 * 소모 양·시간은 저장한 값과 같은 모양이 되게 scale 1 로 둔다({@code 5.0})
 */
@Entity
@Getter
@ToString(callSuper = true, exclude = {"technique", "elements"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ability extends AbstractEntity {

    private static final Pattern NAME_PATTERN = Pattern.compile("^\\S{1,12}$");
    private static final BigDecimal CAST_MAX = BigDecimal.valueOf(60);
    private static final BigDecimal COOLDOWN_MIN = new BigDecimal("0.1");
    private static final BigDecimal COOLDOWN_MAX = BigDecimal.valueOf(3600);

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

    /** 이름 규칙(띄어쓰기 없이 1~12자). 한 공법 안 중복보다 먼저 본다 */
    static void ensureValidName(String name) {
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("신통 이름은 띄어쓰기 없이 1~12자로 정하십시오.");
        }
    }

    /**
     * 이름 규칙·중복 다음의 규칙을 정해진 순서로 검사하고 공법에 딸린 신통을 만든다:
     * 예약어 → 배우는 단계 → 같은 오행 두 번 → 연체 신통에 오행 → 연체술 공법의 법력 소모 → 소수 자리 →
     * 소모 없음인데 양 → 소모가 있는데 양 → 시전 시간 → 재사용 대기시간
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
        if (info.castSeconds().signum() < 0 || info.castSeconds().compareTo(CAST_MAX) > 0) {
            throw new IllegalArgumentException("시전 시간은 0~60초로 정하십시오: " + name);
        }
        if (info.cooldownSeconds().compareTo(COOLDOWN_MIN) < 0 || info.cooldownSeconds().compareTo(COOLDOWN_MAX) > 0) {
            throw new IllegalArgumentException("재사용 대기시간은 0.1~3,600초로 정하십시오: " + name);
        }

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
        return ability;
    }

    /** 오행 속성(오행 순, 바꿀 수 없음). 연체 신통이면 비어 있다 */
    public List<Element> getElements() {
        return elements.stream().sorted().toList();
    }

    private static boolean hasMoreThanOneDecimal(BigDecimal value) {
        return value.stripTrailingZeros().scale() > 1;
    }
}
