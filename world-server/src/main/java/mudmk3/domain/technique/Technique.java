package mudmk3.domain.technique;

import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import mudmk3.domain.AbstractEntity;
import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Realm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * 공법: 이름(공법끼리 유일, 영문 대소문자 무시 — 서비스가 본다), 묘사, 오행 속성(비면 연체술 계열), 시작 경지,
 * 단계 표(1성부터, 1~12단계)와 딸린 신통(정의 순서, 1~30개).
 * 만들기·고치기는 정해진 순서로 검사해 처음 걸린 규칙 하나만 {@code IllegalArgumentException} 으로 알린다
 */
@Entity
@Getter
@ToString(callSuper = true, exclude = {"elements", "stages", "abilities"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Technique extends AbstractEntity {

    public static final int DESCRIPTION_MAX_LENGTH = 500;
    public static final int STAGES_MAX = 12;
    public static final int ABILITIES_MAX = 30;
    public static final int INSIGHT_MAX = 100_000_000;
    public static final int APTITUDE_REQUIRED_MAX = 1_000;
    public static final int STAT_BONUS_MAX = 10_000;
    public static final int APTITUDE_GAIN_MAX = 100;

    private static final Pattern NAME_PATTERN = Pattern.compile("^\\S{1,12}$");

    private String name;

    private String description;

    @Getter(AccessLevel.NONE)
    private Set<Element> elements = new HashSet<>();

    private Realm startRealm;

    @Getter(AccessLevel.NONE)
    private List<TechniqueStage> stages = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    private List<Ability> abilities = new ArrayList<>();

    public static Technique create(TechniqueSaveInfo info) {
        Technique technique = new Technique();
        technique.apply(info);
        return technique;
    }

    /** 이름·묘사·속성·시작 경지를 바꾸고 단계 표와 신통을 통째로 바꾼다(신통은 지우고 새로 넣는다) */
    public void update(TechniqueSaveInfo info) {
        apply(info);
    }

    /** 오행 속성(오행 순, 바꿀 수 없음). 비면 연체술 공법이다 */
    public List<Element> getElements() {
        return elements.stream().sorted().toList();
    }

    /** 속성 이름을 {@code ·} 로 이은 것({@code 목}, {@code 화·토}). 연체술 공법이면 비어 있다 */
    public Optional<String> elementNames() {
        if (elements.isEmpty()) return Optional.empty();
        return Optional.of(getElements().stream().map(Element::koreanName).collect(Collectors.joining("·")));
    }

    /** 속성이 없으면 연체술 공법이다 */
    public boolean isBodyRefining() {
        return elements.isEmpty();
    }

    /** 계열 이름: {@code 오행} 또는 {@code 연체술} */
    public String lineName() {
        return isBodyRefining() ? "연체술" : "오행";
    }

    public int stageCount() {
        return stages.size();
    }

    /** n성(1부터)의 값 */
    public TechniqueStage stage(int n) {
        if (n < 1 || n > stages.size()) {
            throw new IllegalArgumentException("없는 단계입니다: " + n + "성");
        }
        return stages.get(n - 1);
    }

    /** 1성부터 차례로 된 단계 표(바꿀 수 없음) */
    public List<TechniqueStage> getStages() {
        return Collections.unmodifiableList(stages);
    }

    /** 정의 순서의 신통(바꿀 수 없음) */
    public List<Ability> getAbilities() {
        return Collections.unmodifiableList(abilities);
    }

    /** 그 단계까지 배우는 신통(정의 순서) */
    public List<Ability> abilitiesUpTo(int stage) {
        return abilities.stream()
                .filter(ability -> ability.getLearnStage() <= stage)
                .toList();
    }

    static void ensureNoDuplicateElement(List<Element> elements) {
        Set<Element> seen = EnumSet.noneOf(Element.class);
        for (Element element : elements) {
            if (!seen.add(requireNonNull(element))) {
                throw new IllegalArgumentException("같은 속성이 두 번 있습니다: " + element.koreanName());
            }
        }
    }

    private void apply(TechniqueSaveInfo info) {
        requireNonNull(info);
        String newName = info.name();
        if (!NAME_PATTERN.matcher(newName).matches()) {
            throw new IllegalArgumentException("공법 이름은 띄어쓰기 없이 1~12자로 정하십시오.");
        }
        if (info.description().length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException("공법 묘사는 500자 이내로 적으십시오.");
        }
        List<Element> newElements = info.elements();
        ensureNoDuplicateElement(newElements);
        List<TechniqueStage> newStages = info.stages();
        if (newStages.isEmpty() || newStages.size() > STAGES_MAX) {
            throw new IllegalArgumentException("단계 수는 1~12로 정하십시오.");
        }
        boolean bodyRefining = newElements.isEmpty();
        for (int i = 0; i < newStages.size(); i++) {
            ensureValidStage(i + 1, requireNonNull(newStages.get(i)), newElements.size(), bodyRefining);
        }
        List<Ability> newAbilities = createAbilities(info.abilities(), newStages.size(), bodyRefining);

        this.name = newName;
        this.description = info.description();
        this.elements.clear();
        this.elements.addAll(newElements);
        this.startRealm = requireNonNull(info.startRealm());
        this.stages.clear();
        this.stages.addAll(newStages);
        this.abilities.clear();
        this.abilities.addAll(newAbilities);
    }

    private static void ensureValidStage(int n, TechniqueStage stage, int elementCount, boolean bodyRefining) {
        if (n == 1) {
            if (stage.insightRequired() != 0) {
                throw new IllegalArgumentException("1성은 읽으면 이르므로 필요 깨달음을 둘 수 없습니다.");
            }
        } else {
            if (stage.insightRequired() < 1 || stage.insightRequired() > INSIGHT_MAX) {
                throw new IllegalArgumentException("필요 깨달음은 1~100,000,000으로 정하십시오. (" + n + "성)");
            }
            if (elementCount > 1 && stage.insightRequired() % elementCount != 0) {
                throw new IllegalArgumentException(
                        "필요 깨달음은 속성 수(" + elementCount + ")로 나누어떨어져야 합니다. (" + n + "성)");
            }
        }
        if (bodyRefining && (stage.aptitudeRequired() != 0 || stage.aptitudeGain() != 0)) {
            throw new IllegalArgumentException("연체술 공법에는 영근 자질 조건·증가를 둘 수 없습니다.");
        }
        if (!bodyRefining && (stage.bodyBonus() != 0 || stage.strengthBonus() != 0)) {
            throw new IllegalArgumentException("육신·근력은 연체술 공법만 올립니다.");
        }
        ensureInRange(n, "자질 조건", stage.aptitudeRequired(), APTITUDE_REQUIRED_MAX);
        ensureInRange(n, "법력", stage.manaBonus(), STAT_BONUS_MAX);
        ensureInRange(n, "의식", stage.senseBonus(), STAT_BONUS_MAX);
        ensureInRange(n, "영력", stage.spiritBonus(), STAT_BONUS_MAX);
        ensureInRange(n, "정혈", stage.essenceBonus(), STAT_BONUS_MAX);
        ensureInRange(n, "육신", stage.bodyBonus(), STAT_BONUS_MAX);
        ensureInRange(n, "근력", stage.strengthBonus(), STAT_BONUS_MAX);
        ensureInRange(n, "영근 자질", stage.aptitudeGain(), APTITUDE_GAIN_MAX);
    }

    private static void ensureInRange(int n, String fieldName, int value, int max) {
        if (value < 0 || value > max) {
            throw new IllegalArgumentException("단계 값이 범위를 벗어났습니다. (" + n + "성 " + fieldName + ")");
        }
    }

    private List<Ability> createAbilities(List<AbilitySaveInfo> infos, int stageCount, boolean bodyRefining) {
        if (infos.isEmpty() || infos.size() > ABILITIES_MAX) {
            throw new IllegalArgumentException("신통은 1~30개로 정하십시오.");
        }
        List<Ability> result = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (AbilitySaveInfo info : infos) {
            requireNonNull(info);
            Ability.ensureValidName(info.name());
            if (!names.add(info.name().toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("같은 신통 이름이 두 번 있습니다: " + info.name());
            }
            result.add(Ability.create(this, info, stageCount, bodyRefining));
        }
        if (result.stream().noneMatch(ability -> ability.getLearnStage() == 1)) {
            throw new IllegalArgumentException("1성에 배우는 신통이 하나 이상 있어야 합니다.");
        }
        return result;
    }
}
