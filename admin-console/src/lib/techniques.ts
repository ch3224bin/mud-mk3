import type {
  AbilityKindCode,
  AbilityResponse,
  AbilitySaveRequest,
  AbilityTargetCode,
  CostStatCode,
  ElementCode,
  TechniqueStage,
  UsableWhenCode,
} from "./api";

/** 오행(world-server Element 와 같음, 오행 순서) */
export const ELEMENTS: { code: ElementCode; name: string }[] = [
  { code: "METAL", name: "금" },
  { code: "WOOD", name: "목" },
  { code: "WATER", name: "수" },
  { code: "FIRE", name: "화" },
  { code: "EARTH", name: "토" },
];

/** 신통 종류(world-server AbilityKind 와 같음, 룰북 종류 순서) */
export const ABILITY_KINDS: { code: AbilityKindCode; name: string }[] = [
  { code: "ATTACK", name: "공격" },
  { code: "AREA_ATTACK", name: "범위 공격" },
  { code: "DEFENSE", name: "방어" },
  { code: "EVASION", name: "회피" },
  { code: "HEALING", name: "치료" },
  { code: "ESCAPE", name: "둔술" },
  { code: "DETECTION", name: "탐지" },
  { code: "STEALTH", name: "은신" },
  { code: "CONCEALMENT", name: "렴기" },
  { code: "BUFF", name: "보조" },
];

/** 신통 대상(world-server AbilityTarget 과 같음) */
export const ABILITY_TARGETS: { code: AbilityTargetCode; name: string }[] = [
  { code: "SELF", name: "자신" },
  { code: "SINGLE", name: "한 대상" },
  { code: "ALL_ENEMIES", name: "방의 모든 적" },
];

/** 신통 소모 스탯(world-server CostStat 과 같음) */
export const COST_STATS: { code: CostStatCode; name: string }[] = [
  { code: "NONE", name: "없음" },
  { code: "MANA", name: "법력" },
  { code: "SENSE", name: "의식" },
  { code: "ESSENCE", name: "정혈" },
];

/** 신통을 쓸 수 있는 때(world-server UsableWhen 과 같음) */
export const USABLE_WHENS: { code: UsableWhenCode; name: string }[] = [
  { code: "COMBAT_ONLY", name: "전투 중만" },
  { code: "ANYTIME", name: "전투 밖에도" },
];

/** 단계 수 상한(룰북 기준값의 최대) */
export const MAX_STAGES = 12;
/** 새 공법의 단계 수 */
export const DEFAULT_STAGE_COUNT = 9;
/** 한 공법의 신통 수 상한 */
export const MAX_ABILITIES = 30;

/** 단계 표의 값 칸(열 순서) */
export const STAGE_FIELDS: { key: keyof TechniqueStage; label: string }[] = [
  { key: "insightRequired", label: "필요 깨달음" },
  { key: "aptitudeRequired", label: "자질 조건" },
  { key: "manaBonus", label: "법력" },
  { key: "senseBonus", label: "의식" },
  { key: "spiritBonus", label: "영력" },
  { key: "essenceBonus", label: "정혈" },
  { key: "bodyBonus", label: "육신" },
  { key: "strengthBonus", label: "근력" },
  { key: "aptitudeGain", label: "영근 자질" },
];

/** 단계 이름. 예: 1성 */
export function stageLabel(stage: number): string {
  return `${stage}성`;
}

/** 오행 순서로 고른 속성. 예: [FIRE, WOOD] → [WOOD, FIRE] */
export function inElementOrder(elements: ElementCode[]): ElementCode[] {
  return ELEMENTS.map((e) => e.code).filter((code) => elements.includes(code));
}

/** 고른 속성에 하나를 넣거나 뺀다(오행 순서) */
export function toggleElement(elements: ElementCode[], code: ElementCode, checked: boolean): ElementCode[] {
  return inElementOrder(checked ? [...elements, code] : elements.filter((e) => e !== code));
}

/** 폼에서 고치는 단계 한 줄. 숫자 칸은 입력 그대로의 글자다 */
export type StageDraft = Record<keyof TechniqueStage, string>;

/** 새 단계 행(모두 0) */
export function emptyStage(): StageDraft {
  return Object.fromEntries(STAGE_FIELDS.map((field) => [field.key, "0"])) as StageDraft;
}

export function stageDraftOf(stage: TechniqueStage): StageDraft {
  return Object.fromEntries(STAGE_FIELDS.map((field) => [field.key, String(stage[field.key])])) as StageDraft;
}

/** 단계 수를 바꾼다. 늘리면 끝에 0 행을 더하고, 줄이면 끝 행을 버린다 */
export function resizeStages(stages: StageDraft[], count: number): StageDraft[] {
  if (count <= stages.length) {
    return stages.slice(0, count);
  }
  return [...stages, ...Array.from({ length: count - stages.length }, emptyStage)];
}

/** 저장 본문의 단계. 1성은 읽으면 이르므로 필요 깨달음이 늘 0 이고, 빈칸은 0 이다 */
export function toStages(stages: StageDraft[]): TechniqueStage[] {
  return stages.map((draft, index) => {
    const stage = Object.fromEntries(
      STAGE_FIELDS.map((field) => [field.key, Number(draft[field.key])]),
    ) as TechniqueStage;
    return index === 0 ? { ...stage, insightRequired: 0 } : stage;
  });
}

/** 한 신통의 이펙트 문장 수 상한 */
export const MAX_EFFECT_LINES = 5;

/** 폼에서 고치는 단계별 값 한 줄. 값 칸은 입력 그대로의 글자다 */
export type StagePowerDraft = { stage: number; basePower: string; coefficient: string };

/** `채우기` 칸: 배우는 단계의 기본 위력·계수와 단계마다 더하는 양 */
export type FillDraft = { basePower: string; coefficient: string; baseStep: string; coefficientStep: string };

/** 채우기 기본값(룰북 입문 공격 신통: 1성 2 · 1.2, 단계마다 기본 +0.2) */
export const DEFAULT_FILL: FillDraft = { basePower: "2", coefficient: "1.2", baseStep: "0.2", coefficientStep: "0" };

/** 소수 digits 자리에서 반올림하고 끝 0 을 지운 글자. 예: 2.2000000000000002 → `2.2` */
function roundedText(value: number, digits: number): string {
  const scale = 10 ** digits;
  // 부동소수 오차(1.3335 × 1000 = 1333.4999…)를 먼저 걷어 내고 반올림한다.
  return String(Math.round(Number((value * scale).toFixed(6))) / scale);
}

/** 배우는 단계 ~ 단계 수의 단계별 값: 배우는 단계 값 + (단계 − 배우는 단계) × 단계마다 더하는 양 */
export function fillStagePowers(learnStage: number, stageCount: number, fill: FillDraft): StagePowerDraft[] {
  const powers: StagePowerDraft[] = [];
  for (let stage = learnStage; stage <= stageCount; stage++) {
    const steps = stage - learnStage;
    powers.push({
      stage,
      basePower: roundedText(Number(fill.basePower) + steps * Number(fill.baseStep), 2),
      coefficient: roundedText(Number(fill.coefficient) + steps * Number(fill.coefficientStep), 3),
    });
  }
  return powers;
}

/**
 * 단계별 값 행을 배우는 단계 ~ 단계 수로 맞춘다. 남는 단계의 값은 그대로, 새 단계는 새 목록의 바로 앞 단계 값을
 * 복사하고 앞이 없으면 채우기 기본값의 배우는 단계 값이다
 */
export function resizeStagePowers(
  drafts: StagePowerDraft[],
  learnStage: number,
  stageCount: number,
): StagePowerDraft[] {
  const powers: StagePowerDraft[] = [];
  for (let stage = learnStage; stage <= stageCount; stage++) {
    const kept = drafts.find((draft) => draft.stage === stage);
    const previous = powers.at(-1) ?? { basePower: DEFAULT_FILL.basePower, coefficient: DEFAULT_FILL.coefficient };
    powers.push(kept ?? { stage, basePower: previous.basePower, coefficient: previous.coefficient });
  }
  return powers;
}

/**
 * 폼에서 고치는 신통 한 줄. 소수가 있는 칸은 입력 그대로의 글자다.
 * stagePowers 는 종류가 공격이 아니어도 지우지 않고 들고 있다(다시 공격으로 돌리면 보인다).
 * usableInGame 은 저장된 응답의 값이고 새 행에는 없다
 */
export type AbilityDraft = Omit<
  AbilitySaveRequest,
  "costAmount" | "castSeconds" | "cooldownSeconds" | "stagePowers"
> & {
  costAmount: string;
  castSeconds: string;
  cooldownSeconds: string;
  stagePowers: StagePowerDraft[];
  usableInGame?: boolean;
};

/**
 * `신통 추가` 의 새 행: 1성, 공격, 속성 없음, 한 대상, 소모 없음, 시전 0, 대기 3, 전투 중만,
 * 이펙트 문장 빈 줄 하나, 단계별 값은 1성 ~ 단계 수를 채우기 기본값으로
 */
export function newAbility(stageCount: number): AbilityDraft {
  return {
    name: "",
    learnStage: 1,
    kind: "ATTACK",
    elements: [],
    bodyRefining: false,
    target: "SINGLE",
    costStat: "NONE",
    costAmount: "0",
    costPerTick: false,
    castSeconds: "0",
    cooldownSeconds: "3",
    usableWhen: "COMBAT_ONLY",
    effectLines: [""],
    stagePowers: fillStagePowers(1, stageCount, DEFAULT_FILL),
  };
}

export function abilityDraftOf(ability: AbilityResponse): AbilityDraft {
  return {
    name: ability.name,
    learnStage: ability.learnStage,
    kind: ability.kind,
    elements: inElementOrder(ability.elements),
    bodyRefining: ability.bodyRefining,
    target: ability.target,
    costStat: ability.costStat,
    costAmount: String(ability.costAmount),
    costPerTick: ability.costPerTick,
    castSeconds: String(ability.castSeconds),
    cooldownSeconds: String(ability.cooldownSeconds),
    usableWhen: ability.usableWhen,
    effectLines: [...ability.effectLines],
    stagePowers: ability.stagePowers.map((power) => ({
      stage: power.stage,
      basePower: String(power.basePower),
      coefficient: String(power.coefficient),
    })),
    usableInGame: ability.usableInGame,
  };
}

/**
 * 저장 본문의 신통. 빈칸은 0 이고, bodyRefining·costPerTick 은 늘 보낸다.
 * 단계별 값은 공격일 때만 숫자로 보내고 그 밖은 빈 목록이다. usableInGame 은 보내지 않는다
 */
export function toAbility(draft: AbilityDraft): AbilitySaveRequest {
  return {
    name: draft.name,
    learnStage: draft.learnStage,
    kind: draft.kind,
    elements: inElementOrder(draft.elements),
    bodyRefining: draft.bodyRefining,
    target: draft.target,
    costStat: draft.costStat,
    costAmount: Number(draft.costAmount),
    costPerTick: draft.costPerTick,
    castSeconds: Number(draft.castSeconds),
    cooldownSeconds: Number(draft.cooldownSeconds),
    usableWhen: draft.usableWhen,
    effectLines: draft.effectLines,
    stagePowers:
      draft.kind === "ATTACK"
        ? draft.stagePowers.map((power) => ({
            stage: power.stage,
            basePower: Number(power.basePower),
            coefficient: Number(power.coefficient),
          }))
        : [],
  };
}
