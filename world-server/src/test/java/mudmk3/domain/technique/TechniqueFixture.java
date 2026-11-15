package mudmk3.domain.technique;

import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Realm;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** 룰북 공법 예시(장춘공·강체결·청목결)로 만든 공법·입력·요청 */
public final class TechniqueFixture {

    private TechniqueFixture() {
    }

    public static Technique changchun() {
        return Technique.create(changchunInfo());
    }

    public static Technique ironBody() {
        return Technique.create(ironBodyInfo());
    }

    public static Technique greenWood() {
        return Technique.create(greenWoodInfo());
    }

    /** 장춘공: 목, 범인부터, 9성(technique.md 장춘공 표), 신통 화탄술(1성)·천안술(2성)·어풍결(3성) */
    public static TechniqueSaveInfo changchunInfo() {
        return changchunInfo("장춘공");
    }

    public static TechniqueSaveInfo changchunInfo(String name) {
        return new TechniqueSaveInfo(name, "범인도 수련을 시작할 수 있는 목 속성 입문 공법.", List.of(Element.WOOD),
                Realm.MORTAL, changchunStages(), changchunAbilities());
    }

    public static List<TechniqueStage> changchunStages() {
        return List.of(
                stage(0, 0, 0, 0, 0),
                stage(120, 0, 2, 1, 0, 1),
                stage(190, 0, 2, 0, 0, 1),
                stage(280, 0, 2, 1, 0, 1),
                stage(400, 0, 2, 0, 1, 1),
                stage(560, 0, 2, 1, 0, 1),
                stage(800, 0, 2, 0, 0, 1),
                stage(1_100, 0, 2, 1, 0, 1),
                stage(1_540, 0, 2, 0, 1, 1));
    }

    public static List<AbilitySaveInfo> changchunAbilities() {
        return List.of(fireBall(), heavenEye(), windRiding());
    }

    /** 화탄술: 1성, 공격, 화, 한 대상, 법력 5, 시전 0, 대기 3, 전투 중만 */
    public static AbilitySaveInfo fireBall() {
        return withStagePowers(withEffectLines(
                ability("화탄술", 1, AbilityKind.ATTACK, List.of(Element.FIRE), AbilityTarget.SINGLE,
                        CostStat.MANA, "5", "0", "3", UsableWhen.COMBAT_ONLY),
                FIRE_BALL_LINES), fireBallStagePowers(9));
    }

    /** 화탄술 이펙트 문장(admin-tool.md 예 셋) */
    public static final List<String> FIRE_BALL_LINES = List.of(
            "손끝에서 붉은 불덩이가 피어올라 {대상}을 덮친다!",
            "{시전자}의 손바닥에서 화탄이 튀어 나가 {대상}에게 터진다!",
            "작은 불덩이가 꼬리를 그으며 {대상}에게 날아든다!");

    /** 화탄술 단계별 값 1성~{@code lastStage}성: 기본 위력 2 + 0.2 × (s − 1), 계수 1.2 */
    public static List<AbilityStagePower> fireBallStagePowers(int lastStage) {
        return stagePowers(1, lastStage, "2", "0.2", "1.2");
    }

    /**
     * 낙뢰술(시전 신통 픽스처): 1성, 공격, 화, 한 대상, 법력 5, 시전 2, 대기 3, 전투 중만, 이펙트 문장 하나.
     * 단계별 값은 화탄술과 같다(F14)
     */
    public static AbilitySaveInfo lightning() {
        return withStagePowers(withEffectLines(
                ability("낙뢰술", 1, AbilityKind.ATTACK, List.of(Element.FIRE), AbilityTarget.SINGLE,
                        CostStat.MANA, "5", "2", "3", UsableWhen.COMBAT_ONLY),
                List.of("하늘에서 내리친 뇌기가 {대상}을 꿰뚫는다!")), fireBallStagePowers(9));
    }

    /** 천안술: 2성, 탐지, 목, 자신, 법력 3, 대기 10, 전투 밖에도 */
    public static AbilitySaveInfo heavenEye() {
        return ability("천안술", 2, AbilityKind.DETECTION, List.of(Element.WOOD), AbilityTarget.SELF,
                CostStat.MANA, "3", "0", "10", UsableWhen.ANYTIME);
    }

    /** 어풍결: 3성, 둔술, 목, 자신, 법력 10, 대기 10, 전투 밖에도 */
    public static AbilitySaveInfo windRiding() {
        return ability("어풍결", 3, AbilityKind.ESCAPE, List.of(Element.WOOD), AbilityTarget.SELF,
                CostStat.MANA, "10", "0", "10", UsableWhen.ANYTIME);
    }

    /** 강체결: 연체술, 범인부터, 9성(technique-list.md 강체결 표), 신통 철권(1성) */
    public static TechniqueSaveInfo ironBodyInfo() {
        return new TechniqueSaveInfo("강체결", "몸을 단련하는 연체술 입문 공법.", List.of(), Realm.MORTAL,
                List.of(
                        bodyStage(0, 0, 0),
                        bodyStage(530, 0, 1),
                        bodyStage(800, 1, 0),
                        bodyStage(1_170, 0, 1),
                        bodyStage(1_720, 1, 0),
                        bodyStage(2_440, 0, 1),
                        bodyStage(3_450, 1, 0),
                        bodyStage(4_770, 0, 1),
                        bodyStage(6_890, 1, 0)),
                List.of(ironFist()));
    }

    /** 철권: 1성, 공격, 연체, 한 대상, 소모 없음, 대기 4 */
    public static AbilitySaveInfo ironFist() {
        return new AbilitySaveInfo("철권", 1, AbilityKind.ATTACK, List.of(), true, AbilityTarget.SINGLE,
                CostStat.NONE, BigDecimal.ZERO, false, BigDecimal.ZERO, new BigDecimal("4"), UsableWhen.COMBAT_ONLY,
                List.of("단단히 쥔 주먹이 {대상}의 몸에 꽂힌다!"), stagePowers(1, 9, "5", "0.5", "2.2"));
    }

    /** 청목결: 목, 연기 7층부터, 8성(technique-list.md 오행 연기 후반 표), 신통 청목전(1성)·목령갑(4성) */
    public static TechniqueSaveInfo greenWoodInfo() {
        return new TechniqueSaveInfo("청목결", "연기 후반의 목 속성 공법.", List.of(Element.WOOD), Realm.QI_7,
                List.of(
                        stage(0, 15, 0, 0, 0, 0),
                        stage(1_800, 15, 3, 1, 0, 0),
                        stage(2_600, 15, 3, 0, 0, 0),
                        stage(3_500, 15, 3, 1, 0, 0),
                        stage(4_800, 20, 3, 0, 0, 0),
                        stage(6_500, 20, 3, 1, 0, 0),
                        stage(8_700, 20, 3, 0, 0, 0),
                        stage(12_000, 20, 3, 1, 1, 0)),
                List.of(
                        withStagePowers(withEffectLines(
                                ability("청목전", 1, AbilityKind.ATTACK, List.of(Element.WOOD), AbilityTarget.SINGLE,
                                        CostStat.MANA, "12", "0", "3", UsableWhen.COMBAT_ONLY),
                                List.of("푸른 나뭇잎이 칼날처럼 {대상}을 벤다!")), stagePowers(1, 8, "3", "0", "1.2")),
                        ability("목령갑", 4, AbilityKind.DEFENSE, List.of(Element.WOOD), AbilityTarget.SELF,
                                CostStat.MANA, "18", "0", "15", UsableWhen.COMBAT_ONLY)));
    }

    /** 오행공법 단계: 필요 깨달음, 자질 조건, 법력, 의식, 영력, 영근 자질 증가 */
    public static TechniqueStage stage(int insight, int aptitudeRequired, int mana, int sense, int spirit, int aptitudeGain) {
        return new TechniqueStage(insight, aptitudeRequired, mana, sense, spirit, 0, 0, 0, aptitudeGain);
    }

    private static TechniqueStage stage(int insight, int aptitudeRequired, int mana, int sense, int spirit) {
        return stage(insight, aptitudeRequired, mana, sense, spirit, 0);
    }

    /** 연체술 단계: 필요 깨달음, 육신, 근력 */
    public static TechniqueStage bodyStage(int insight, int body, int strength) {
        return new TechniqueStage(insight, 0, 0, 0, 0, 0, body, strength, 0);
    }

    /**
     * 신통 입력. 이펙트 문장은 {@code <이름> 기운이 퍼진다.} 하나, 공격이면 단계별 값은 배우는 단계~9성
     * (기본 위력 2 + 0.2 × (s − 1), 계수 1.2 — 장춘공 9성 기준). 9단계가 아닌 공법은 {@link #withStagePowers} 로 바꾼다
     */
    public static AbilitySaveInfo ability(String name, int learnStage, AbilityKind kind, List<Element> elements,
                                          AbilityTarget target, CostStat costStat, String costAmount,
                                          String castSeconds, String cooldownSeconds, UsableWhen usableWhen) {
        List<AbilityStagePower> stagePowers = kind == AbilityKind.ATTACK
                ? stagePowers(learnStage, 9, new BigDecimal("2").add(new BigDecimal("0.2")
                        .multiply(BigDecimal.valueOf(Math.max(learnStage - 1, 0)))).toPlainString(), "0.2", "1.2")
                : List.of();
        return new AbilitySaveInfo(name, learnStage, kind, elements, false, target, costStat,
                new BigDecimal(costAmount), false, new BigDecimal(castSeconds), new BigDecimal(cooldownSeconds),
                usableWhen, List.of(name + " 기운이 퍼진다."), stagePowers);
    }

    /**
     * {@code from}성~{@code to}성의 단계별 값: {@code from}성 기본 위력 {@code base}, 단계마다 {@code step} 씩 더함,
     * 계수는 모두 {@code coefficient}. {@code from > to} 이면 빈 목록
     */
    public static List<AbilityStagePower> stagePowers(int from, int to, String base, String step, String coefficient) {
        List<AbilityStagePower> result = new ArrayList<>();
        for (int s = from; s <= to; s++) {
            BigDecimal basePower = new BigDecimal(base).add(new BigDecimal(step).multiply(BigDecimal.valueOf(s - from)));
            result.add(new AbilityStagePower(s, basePower, new BigDecimal(coefficient)));
        }
        return List.copyOf(result);
    }

    public static AbilitySaveInfo withEffectLines(AbilitySaveInfo info, List<String> effectLines) {
        return new AbilitySaveInfo(info.name(), info.learnStage(), info.kind(), info.elements(), info.bodyRefining(),
                info.target(), info.costStat(), info.costAmount(), info.costPerTick(), info.castSeconds(),
                info.cooldownSeconds(), info.usableWhen(), effectLines, info.stagePowers());
    }

    public static AbilitySaveInfo withStagePowers(AbilitySaveInfo info, List<AbilityStagePower> stagePowers) {
        return new AbilitySaveInfo(info.name(), info.learnStage(), info.kind(), info.elements(), info.bodyRefining(),
                info.target(), info.costStat(), info.costAmount(), info.costPerTick(), info.castSeconds(),
                info.cooldownSeconds(), info.usableWhen(), info.effectLines(), stagePowers);
    }

    public static AbilitySaveInfo withCast(AbilitySaveInfo info, String castSeconds) {
        return new AbilitySaveInfo(info.name(), info.learnStage(), info.kind(), info.elements(), info.bodyRefining(),
                info.target(), info.costStat(), info.costAmount(), info.costPerTick(), new BigDecimal(castSeconds),
                info.cooldownSeconds(), info.usableWhen(), info.effectLines(), info.stagePowers());
    }

    public static AbilitySaveInfo withCost(AbilitySaveInfo info, CostStat costStat, String amount, boolean perTick) {
        return new AbilitySaveInfo(info.name(), info.learnStage(), info.kind(), info.elements(), info.bodyRefining(),
                info.target(), costStat, new BigDecimal(amount), perTick, info.castSeconds(),
                info.cooldownSeconds(), info.usableWhen(), info.effectLines(), info.stagePowers());
    }

    /** 단계별 값을 {@code stageCount}성까지로 자른다(단계 수를 줄인 공법에 넣을 때) */
    public static AbilitySaveInfo upToStage(AbilitySaveInfo info, int stageCount) {
        return withStagePowers(info, info.stagePowers().stream().filter(power -> power.stage() <= stageCount).toList());
    }

    /** 화탄술을 바꾼 신통 하나로 장춘공 입력을 만든다 */
    public static TechniqueSaveInfo changchunWith(List<AbilitySaveInfo> abilities) {
        return new TechniqueSaveInfo("장춘공", "", List.of(Element.WOOD), Realm.MORTAL, changchunStages(), abilities);
    }

    /** 단계 표를 바꾼 장춘공 입력. 화탄술의 단계별 값은 단계 수에 맞춘다 */
    public static TechniqueSaveInfo changchunWithStages(List<TechniqueStage> stages) {
        return new TechniqueSaveInfo("장춘공", "", List.of(Element.WOOD), Realm.MORTAL, stages,
                List.of(withStagePowers(fireBall(), fireBallStagePowers(stages.size()))));
    }
}
