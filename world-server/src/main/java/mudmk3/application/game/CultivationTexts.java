package mudmk3.application.game;

import mudmk3.application.cultivation.provided.ReadResult;
import mudmk3.domain.character.PlayerCharacter;
import mudmk3.domain.character.SpiritRoot;
import mudmk3.domain.combat.AbilityKey;
import mudmk3.domain.combat.AbilityTimers;
import mudmk3.domain.combat.Damage;
import mudmk3.domain.cultivation.CharacterTechnique;
import mudmk3.domain.cultivation.Insight;
import mudmk3.domain.cultivation.InsightKind;
import mudmk3.domain.cultivation.StatSheet;
import mudmk3.domain.shared.Durations;
import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Josa;
import mudmk3.domain.shared.Numbers;
import mudmk3.domain.shared.Realm;
import mudmk3.domain.technique.Ability;
import mudmk3.domain.technique.CostStat;
import mudmk3.domain.technique.Technique;
import mudmk3.domain.technique.TechniqueStage;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;

/**
 * 수련 정보 명령({@code 상태}·{@code 공법}·{@code 신통})·{@code 읽기}·경지 알림의 출력 줄을 그린다
 * (D67, command.md {@code ### 상태}·{@code ### 공법 · 신통}·{@code ### 읽기}·{@code ### 경지가 바뀔 때}).
 * 공법 정의는 지연 로딩이라 부르는 쪽 트랜잭션 안에서 부른다
 */
@Component
class CultivationTexts {

    static final String NO_TECHNIQUES = "익힌 공법이 없습니다.";
    static final String NO_ABILITIES = "배운 신통이 없습니다.";
    static final String READ_LEARNED = "옥간을 이마에 대자 구결이 머릿속으로 흘러든다. ";
    static final String NOT_SCROLL = "읽을 수 있는 물건이 아닙니다.";
    static final String NO_TECHNIQUE = "아무것도 적혀 있지 않습니다.";
    static final String CANNOT_LEARN = "지금은 이 공법을 익힐 수 없습니다. ";

    private static final String GAP = "  ";
    private static final String GAUGE_GAP = "     ";
    private static final String POINT_GAP = "   ";
    private static final String BODY_REFINING = "연체";

    /**
     * 이름·경지·영근 → 소모 스탯 → 포인트 스탯 → 수위(범인은 없음) → 깨달음. 기혈·법력은 현재 값(깎인 양을 뺌),
     * 나머지 현재 수치 = 최대치, 수위는 늘 0, 깨달음은 갈래마다 소수점을 버린 쌓인 양
     */
    String status(PlayerCharacter character, StatSheet stats, Insight insight) {
        Realm realm = character.getRealm();
        SpiritRoot root = character.findSpiritRoot()
                .orElseThrow(() -> new IllegalStateException("영근을 아직 정하지 않은 캐릭터입니다. id: " + character.getId()));
        String aptitudes = root.entries().stream()
                .map(entry -> entry.element().koreanName() + " " + stats.aptitudeShown(entry.element()))
                .collect(Collectors.joining(", "));

        List<String> lines = new ArrayList<>();
        lines.add(character.getName().value() + " · " + realm.koreanName() + " · " + root.name() + " (" + aptitudes + ")");
        lines.add(gauge("기혈", Damage.gauge(stats.qiBloodCurrent()), stats.qiBlood())
                + GAUGE_GAP + gauge("법력", Damage.gauge(stats.manaCurrent()), stats.mana()));
        lines.add(gauge("의식", stats.sense()) + GAUGE_GAP + gauge("정혈", stats.essence()));
        lines.add("육신" + GAP + Numbers.count(stats.body()) + POINT_GAP + "근력" + GAP + Numbers.count(stats.strength())
                + POINT_GAP + gauge("영력", stats.spirit()));
        OptionalInt required = realm.requiredCultivation();
        if (required.isPresent()) {
            lines.add("수위" + GAP + "0 / " + Numbers.count(required.getAsInt()));
        }
        lines.add(insightLine(insight));
        return String.join("\n", lines);
    }

    /**
     * 익힌 순서로 한 줄씩: {@code 장춘공  1/9성 · 목 · 오행  다음 2성: 목 깨달음 120 (지금 2)}.
     * {@code 지금} 은 그 속성(연체술 공법은 연체)의 소수점을 버린 깨달음. 끝 단계면 성 뒤에 {@code  · 끝에 이름}, 지금 경지가 시작 경지보다 낮으면 끝에 {@code   (연기 7층 필요)}
     */
    String techniques(Realm current, List<CharacterTechnique> learned, Insight insight) {
        if (learned.isEmpty()) return NO_TECHNIQUES;
        return learned.stream()
                .map(each -> techniqueLine(current, each, insight))
                .collect(Collectors.joining("\n"));
    }

    /**
     * 익힌 공법마다 머리 {@code [장춘공 1성]}, 그 아래 두 칸 들여 배운 신통(정의 순서)
     * {@code 화탄술  공격·화  법력 5  대기 3초}. 재사용 대기시간이 남은 신통은 끝에 두 칸 띄고 {@code (1.8초 남음)}(D112 올림, D114).
     * 공통 대기는 보이지 않는다
     */
    String abilities(List<CharacterTechnique> learned, @Nullable AbilityTimers timers, Instant now) {
        List<String> lines = new ArrayList<>();
        for (CharacterTechnique each : learned) {
            Technique technique = each.getTechnique();
            List<Ability> abilities = technique.abilitiesUpTo(each.getStage());
            if (abilities.isEmpty()) continue;
            lines.add("[" + technique.getName() + " " + each.getStage() + "성]");
            for (Ability ability : abilities) {
                lines.add(GAP + abilityLine(ability) + cooldownTail(technique, ability, timers, now));
            }
        }
        return lines.isEmpty() ? NO_ABILITIES : String.join("\n", lines);
    }

    /**
     * 읽기 성공(command.md {@code ### 읽기}): 첫 줄 → 1성 스탯 줄(0이 아닌 것만, 있으면) → 신통 줄 또는 입문 줄.
     * 스탯 줄 순서는 법력 · 의식 · 영력 · 정혈 · 육신 · 근력 · 영근 자질(공법 속성마다 오행 순)
     */
    String readSuccess(ReadResult.Learned learned) {
        List<String> lines = new ArrayList<>();
        lines.add(READ_LEARNED + learned.techniqueName() + " 1성에 이르렀습니다!");
        List<String> bonuses = stageBonuses(learned.firstStage(), learned.elements());
        if (!bonuses.isEmpty()) {
            lines.add(String.join(" · ", bonuses));
        }
        String abilities = String.join(", ", learned.newAbilityNames());
        if (learned.initiated()) {
            lines.add("단전에 따뜻한 기운이 맺힌다. " + Realm.QI_1.koreanName() + "이 되었습니다."
                    + (abilities.isEmpty() ? "" : " 새 신통: " + abilities));
        } else if (!abilities.isEmpty()) {
            lines.add("새 신통을 깨쳤습니다: " + abilities);
        }
        return String.join("\n", lines);
    }

    /** 읽기 거절 문구(command.md {@code ### 읽기} 3~8) */
    String readFailure(ReadResult result) {
        return switch (result) {
            case ReadResult.NotScroll _ -> NOT_SCROLL;
            case ReadResult.NoTechnique _ -> NO_TECHNIQUE;
            case ReadResult.AlreadyLearned already -> "이미 깨친 내용입니다. (" + already.techniqueName() + " "
                    + already.stage() + "/" + already.stageCount() + "성)";
            case ReadResult.RealmTooLow tooLow -> CANNOT_LEARN + "(" + tooLow.required().koreanName() + " 필요)";
            case ReadResult.MissingRoot missing -> CANNOT_LEARN + "(" + rootName(missing.element()) + " 필요)";
            case ReadResult.LowAptitude low -> CANNOT_LEARN + "(" + rootName(low.element()) + " 자질 "
                    + Numbers.count(low.required()) + " 필요, 지금 " + Numbers.count(low.current()) + ")";
            case ReadResult.Learned _ -> throw new IllegalArgumentException("거절이 아닙니다: " + result);
        };
    }

    /**
     * 관리 툴 경지 지정 알림: {@code 경지가 연기 7층으로 바뀌었습니다. (육신 +6, 근력 +3, 영력 +6, 의식 +12, 정혈 +12)}.
     * 괄호 안은 경지 몫 스탯 차이(육신 · 근력 · 영력 · 의식 · 정혈, 0이 아닌 것만, 내리면 {@code -}). 모두 0이면 괄호를 뺀다
     */
    String realmChanged(Realm from, Realm to) {
        List<String> diffs = new ArrayList<>();
        addSigned(diffs, "육신", to.body() - from.body());
        addSigned(diffs, "근력", to.strength() - from.strength());
        addSigned(diffs, "영력", to.spirit() - from.spirit());
        addSigned(diffs, "의식", to.senseEssence() - from.senseEssence());
        addSigned(diffs, "정혈", to.senseEssence() - from.senseEssence());
        String text = "경지가 " + Josa.withEuroRo(to.koreanName()) + " 바뀌었습니다.";
        return diffs.isEmpty() ? text : text + " (" + String.join(", ", diffs) + ")";
    }

    private static List<String> stageBonuses(TechniqueStage stage, List<Element> elements) {
        List<String> parts = new ArrayList<>();
        addSigned(parts, "법력", stage.manaBonus());
        addSigned(parts, "의식", stage.senseBonus());
        addSigned(parts, "영력", stage.spiritBonus());
        addSigned(parts, "정혈", stage.essenceBonus());
        addSigned(parts, "육신", stage.bodyBonus());
        addSigned(parts, "근력", stage.strengthBonus());
        if (stage.aptitudeGain() != 0) {
            elements.forEach(element -> addSigned(parts, rootName(element) + " 자질", stage.aptitudeGain()));
        }
        return parts;
    }

    /** 0이 아니면 {@code 법력 +2}·{@code 육신 -6} 을 더한다 */
    private static void addSigned(List<String> parts, String label, int amount) {
        if (amount != 0) parts.add(label + " " + signed(amount));
    }

    private static String signed(int amount) {
        return amount > 0 ? "+" + Numbers.count(amount) : "-" + Numbers.count(-amount);
    }

    /** {@code 화영근} */
    private static String rootName(Element element) {
        return element.koreanName() + "영근";
    }

    private static String gauge(String label, int max) {
        return gauge(label, max, max);
    }

    private static String gauge(String label, int current, int max) {
        return label + GAP + Numbers.count(current) + " / " + Numbers.count(max);
    }

    private static String insightLine(Insight insight) {
        List<String> parts = new ArrayList<>();
        for (InsightKind kind : InsightKind.values()) {
            parts.add(kind.koreanName() + " " + Numbers.count(insight.floor(kind)));
        }
        return "깨달음" + GAP + String.join(" · ", parts);
    }

    private static String techniqueLine(Realm current, CharacterTechnique learned, Insight insight) {
        Technique technique = learned.getTechnique();
        int stage = learned.getStage();
        int stageCount = technique.stageCount();
        boolean last = stage >= stageCount;

        StringBuilder line = new StringBuilder()
                .append(technique.getName()).append(GAP).append(stage).append('/').append(stageCount).append("성");
        if (last) line.append(" · 끝에 이름");
        line.append(technique.elementNames()
                .map(names -> " · " + names + " · " + technique.lineName())
                .orElse(" · " + BODY_REFINING));
        if (!last) {
            line.append(GAP).append("다음 ").append(stage + 1).append("성: ")
                    .append(nextInsight(technique, technique.stage(stage + 1).insightRequired(), insight));
        }
        if (current.isBelow(technique.getStartRealm())) {
            line.append(GAP).append('(').append(technique.getStartRealm().koreanName()).append(" 필요)");
        }
        return line.toString();
    }

    /** 여러 속성이면 필요 깨달음을 속성 수로 똑같이 나눈다(관리 툴이 나누어떨어지게 막는다) */
    private static String nextInsight(Technique technique, int insightRequired, Insight insight) {
        List<Element> elements = technique.getElements();
        if (elements.isEmpty()) {
            return insightPart(InsightKind.BODY, insightRequired, insight);
        }
        int share = insightRequired / elements.size();
        return elements.stream()
                .map(element -> insightPart(InsightKind.of(element), share, insight))
                .collect(Collectors.joining(" · "));
    }

    private static String insightPart(InsightKind kind, int required, Insight insight) {
        return kind.koreanName() + " 깨달음 " + Numbers.count(required)
                + " (지금 " + Numbers.count(insight.floor(kind)) + ")";
    }

    /** 재사용 대기시간이 남았으면 {@code   (1.8초 남음)}, 아니면 빈 문자열 */
    private static String cooldownTail(Technique technique, Ability ability, @Nullable AbilityTimers timers, Instant now) {
        Long techniqueId = technique.getId();
        if (timers == null || techniqueId == null) return "";
        return timers.cooldownRemaining(new AbilityKey(techniqueId, ability.getName()), now)
                .map(remaining -> GAP + "(" + AbilityTexts.remaining(remaining) + " 남음)")
                .orElse("");
    }

    private static String abilityLine(Ability ability) {
        StringBuilder line = new StringBuilder()
                .append(ability.getName()).append(GAP).append(ability.getKind().koreanName());
        if (ability.isBodyRefining()) {
            line.append('·').append(BODY_REFINING);
        } else {
            ability.getElements().forEach(element -> line.append('·').append(element.koreanName()));
        }
        line.append(GAP).append(cost(ability));
        if (ability.getCastSeconds().signum() > 0) {
            line.append(GAP).append("시전 ").append(Durations.korean(ability.getCastSeconds()));
        }
        line.append(GAP).append("대기 ").append(Durations.korean(ability.getCooldownSeconds()));
        return line.toString();
    }

    /** {@code 법력 5}, {@code 의식 1 (틱마다)}, 소모가 없으면 {@code 법력 없음}(룰북 법기 줄 문구) */
    private static String cost(Ability ability) {
        if (ability.getCostStat() == CostStat.NONE) return "법력 없음";
        return ability.getCostStat().koreanName() + " " + Numbers.decimal(ability.getCostAmount())
                + (ability.isCostPerTick() ? " (틱마다)" : "");
    }
}
