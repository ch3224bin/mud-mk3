package mudmk3.application.game;

import mudmk3.domain.character.PlayerCharacter;
import mudmk3.domain.character.SpiritRoot;
import mudmk3.domain.cultivation.CharacterTechnique;
import mudmk3.domain.cultivation.StatSheet;
import mudmk3.domain.shared.Durations;
import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Numbers;
import mudmk3.domain.shared.Realm;
import mudmk3.domain.technique.Ability;
import mudmk3.domain.technique.CostStat;
import mudmk3.domain.technique.Technique;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;

/**
 * 수련 정보 명령({@code 상태}·{@code 공법}·{@code 신통})의 출력 줄을 그린다(D67, command.md {@code ### 상태}·{@code ### 공법 · 신통}).
 * 공법 정의는 지연 로딩이라 부르는 쪽 트랜잭션 안에서 부른다
 */
@Component
class CultivationTexts {

    static final String NO_TECHNIQUES = "익힌 공법이 없습니다.";
    static final String NO_ABILITIES = "배운 신통이 없습니다.";

    private static final String GAP = "  ";
    private static final String GAUGE_GAP = "     ";
    private static final String POINT_GAP = "   ";
    private static final String BODY_REFINING = "연체";

    /**
     * 이름·경지·영근 → 소모 스탯 → 포인트 스탯 → 수위(범인은 없음) → 깨달음. 현재 수치 = 최대치, 수위·깨달음은 4차에 늘 0
     */
    String status(PlayerCharacter character, StatSheet stats) {
        Realm realm = character.getRealm();
        SpiritRoot root = character.findSpiritRoot()
                .orElseThrow(() -> new IllegalStateException("영근을 아직 정하지 않은 캐릭터입니다. id: " + character.getId()));
        String aptitudes = root.entries().stream()
                .map(entry -> entry.element().koreanName() + " " + (int) Math.floor(stats.aptitude(entry.element())))
                .collect(Collectors.joining(", "));

        List<String> lines = new ArrayList<>();
        lines.add(character.getName().value() + " · " + realm.koreanName() + " · " + root.name() + " (" + aptitudes + ")");
        lines.add(gauge("기혈", stats.qiBlood()) + GAUGE_GAP + gauge("법력", stats.mana()));
        lines.add(gauge("의식", stats.sense()) + GAUGE_GAP + gauge("정혈", stats.essence()));
        lines.add("육신" + GAP + Numbers.count(stats.body()) + POINT_GAP + "근력" + GAP + Numbers.count(stats.strength())
                + POINT_GAP + gauge("영력", stats.spirit()));
        OptionalInt required = realm.requiredCultivation();
        if (required.isPresent()) {
            lines.add("수위" + GAP + "0 / " + Numbers.count(required.getAsInt()));
        }
        lines.add(insightLine());
        return String.join("\n", lines);
    }

    /**
     * 익힌 순서로 한 줄씩: {@code 장춘공  1/9성 · 목 · 오행  다음 2성: 목 깨달음 120 (지금 0)}.
     * 끝 단계면 성 뒤에 {@code  · 끝에 이름}, 지금 경지가 시작 경지보다 낮으면 끝에 {@code   (연기 7층 필요)}
     */
    String techniques(Realm current, List<CharacterTechnique> learned) {
        if (learned.isEmpty()) return NO_TECHNIQUES;
        return learned.stream()
                .map(each -> techniqueLine(current, each))
                .collect(Collectors.joining("\n"));
    }

    /**
     * 익힌 공법마다 머리 {@code [장춘공 1성]}, 그 아래 두 칸 들여 배운 신통(정의 순서)
     * {@code 화탄술  공격·화  법력 5  대기 3초}
     */
    String abilities(List<CharacterTechnique> learned) {
        List<String> lines = new ArrayList<>();
        for (CharacterTechnique each : learned) {
            List<Ability> abilities = each.getTechnique().abilitiesUpTo(each.getStage());
            if (abilities.isEmpty()) continue;
            lines.add("[" + each.getTechnique().getName() + " " + each.getStage() + "성]");
            abilities.forEach(ability -> lines.add(GAP + abilityLine(ability)));
        }
        return lines.isEmpty() ? NO_ABILITIES : String.join("\n", lines);
    }

    private static String gauge(String label, int max) {
        return label + GAP + Numbers.count(max) + " / " + Numbers.count(max);
    }

    private static String insightLine() {
        List<String> parts = new ArrayList<>();
        for (Element element : Element.values()) {
            parts.add(element.koreanName() + " 0");
        }
        parts.add(BODY_REFINING + " 0");
        return "깨달음" + GAP + String.join(" · ", parts);
    }

    private static String techniqueLine(Realm current, CharacterTechnique learned) {
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
                    .append(nextInsight(technique, technique.stage(stage + 1).insightRequired()));
        }
        if (current.isBelow(technique.getStartRealm())) {
            line.append(GAP).append('(').append(technique.getStartRealm().koreanName()).append(" 필요)");
        }
        return line.toString();
    }

    /** 여러 속성이면 필요 깨달음을 속성 수로 똑같이 나눈다(관리 툴이 나누어떨어지게 막는다) */
    private static String nextInsight(Technique technique, int insightRequired) {
        List<Element> elements = technique.getElements();
        if (elements.isEmpty()) {
            return insightPart(BODY_REFINING, insightRequired);
        }
        int share = insightRequired / elements.size();
        return elements.stream()
                .map(element -> insightPart(element.koreanName(), share))
                .collect(Collectors.joining(" · "));
    }

    private static String insightPart(String attribute, int required) {
        return attribute + " 깨달음 " + Numbers.count(required) + " (지금 0)";
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
