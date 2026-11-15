import { describe, expect, it } from "vitest";
import type { AbilityResponse } from "./api";
import {
  ABILITY_KINDS,
  ABILITY_TARGETS,
  abilityDraftOf,
  COST_STATS,
  DEFAULT_FILL,
  ELEMENTS,
  emptyStage,
  fillStagePowers,
  inElementOrder,
  MAX_EFFECT_LINES,
  newAbility,
  resizeStagePowers,
  resizeStages,
  toAbility,
  toggleElement,
  toStages,
  USABLE_WHENS,
} from "./techniques";

describe("공법 상수", () => {
  it("오행은 금·목·수·화·토 순서다", () => {
    expect(ELEMENTS).toEqual([
      { code: "METAL", name: "금" },
      { code: "WOOD", name: "목" },
      { code: "WATER", name: "수" },
      { code: "FIRE", name: "화" },
      { code: "EARTH", name: "토" },
    ]);
  });

  it("신통 종류 10가지는 룰북 순서다", () => {
    expect(ABILITY_KINDS.map((k) => `${k.code}:${k.name}`)).toEqual([
      "ATTACK:공격",
      "AREA_ATTACK:범위 공격",
      "DEFENSE:방어",
      "EVASION:회피",
      "HEALING:치료",
      "ESCAPE:둔술",
      "DETECTION:탐지",
      "STEALTH:은신",
      "CONCEALMENT:렴기",
      "BUFF:보조",
    ]);
  });

  it("대상·소모 스탯·쓸 수 있는 때의 코드와 이름", () => {
    expect(ABILITY_TARGETS.map((t) => `${t.code}:${t.name}`)).toEqual([
      "SELF:자신",
      "SINGLE:한 대상",
      "ALL_ENEMIES:방의 모든 적",
    ]);
    expect(COST_STATS.map((c) => `${c.code}:${c.name}`)).toEqual([
      "NONE:없음",
      "MANA:법력",
      "SENSE:의식",
      "ESSENCE:정혈",
    ]);
    expect(USABLE_WHENS.map((u) => `${u.code}:${u.name}`)).toEqual(["COMBAT_ONLY:전투 중만", "ANYTIME:전투 밖에도"]);
  });
});

describe("공법 폼 값", () => {
  it("속성은 고른 순서와 상관없이 오행 순서다", () => {
    expect(inElementOrder(["EARTH", "WOOD", "FIRE"])).toEqual(["WOOD", "FIRE", "EARTH"]);
    expect(toggleElement(["FIRE"], "METAL", true)).toEqual(["METAL", "FIRE"]);
    expect(toggleElement(["METAL", "FIRE"], "METAL", false)).toEqual(["FIRE"]);
  });

  it("단계 수를 늘리면 0 행을 더하고 줄이면 끝 행을 버린다", () => {
    const stages = [{ ...emptyStage(), manaBonus: "1" }, { ...emptyStage(), manaBonus: "2" }];

    expect(resizeStages(stages, 3)).toEqual([stages[0], stages[1], emptyStage()]);
    expect(resizeStages(stages, 1)).toEqual([stages[0]]);
  });

  it("저장 본문의 단계는 숫자이고 1성 필요 깨달음은 늘 0, 빈칸은 0 이다", () => {
    const stages = toStages([
      { ...emptyStage(), insightRequired: "5" },
      { ...emptyStage(), insightRequired: "120", manaBonus: "" },
    ]);

    expect(stages[0].insightRequired).toBe(0);
    expect(stages[1]).toEqual({
      insightRequired: 120,
      aptitudeRequired: 0,
      manaBonus: 0,
      senseBonus: 0,
      spiritBonus: 0,
      essenceBonus: 0,
      bodyBonus: 0,
      strengthBonus: 0,
      aptitudeGain: 0,
    });
  });

  it("새 신통을 저장 본문으로 바꾸면 소수 칸은 숫자이고 bodyRefining·costPerTick 이 있다", () => {
    expect(
      toAbility({ ...newAbility(1), name: "화탄술", costStat: "MANA", costAmount: "5.5", effectLines: ["{대상}을 친다!"] }),
    ).toEqual({
      name: "화탄술",
      learnStage: 1,
      kind: "ATTACK",
      elements: [],
      bodyRefining: false,
      target: "SINGLE",
      costStat: "MANA",
      costAmount: 5.5,
      costPerTick: false,
      castSeconds: 0,
      cooldownSeconds: 3,
      usableWhen: "COMBAT_ONLY",
      effectLines: ["{대상}을 친다!"],
      stagePowers: [{ stage: 1, basePower: 2, coefficient: 1.2 }],
    });
  });
});

describe("이펙트 문장·단계별 값", () => {
  it("채우기 기본값은 2·1.2·0.2·0 이고 이펙트 문장은 5개까지다", () => {
    expect(DEFAULT_FILL).toEqual({ basePower: "2", coefficient: "1.2", baseStep: "0.2", coefficientStep: "0" });
    expect(MAX_EFFECT_LINES).toBe(5);
  });

  it("채우기는 배우는 단계 값에 단계마다 더한 값이다(장춘공 화탄술 1성 2·1.2 … 9성 3.6·1.2)", () => {
    const powers = fillStagePowers(1, 9, DEFAULT_FILL);

    expect(powers.map((p) => p.stage)).toEqual([1, 2, 3, 4, 5, 6, 7, 8, 9]);
    expect(powers[0]).toEqual({ stage: 1, basePower: "2", coefficient: "1.2" });
    expect(powers[1]).toEqual({ stage: 2, basePower: "2.2", coefficient: "1.2" });
    expect(powers[8]).toEqual({ stage: 9, basePower: "3.6", coefficient: "1.2" });
  });

  it("채우기는 계수 단계도 더한다(3성 계수 1.248)", () => {
    const powers = fillStagePowers(1, 3, { basePower: "2", coefficient: "1.2", baseStep: "0.2", coefficientStep: "0.024" });

    expect(powers[2]).toEqual({ stage: 3, basePower: "2.4", coefficient: "1.248" });
  });

  it("채우기는 기본 위력을 소수 둘째, 계수를 셋째 자리에서 반올림한다", () => {
    const powers = fillStagePowers(2, 4, { basePower: "1", coefficient: "1", baseStep: "0.125", coefficientStep: "0.3335" });

    expect(powers).toEqual([
      { stage: 2, basePower: "1", coefficient: "1" },
      { stage: 3, basePower: "1.13", coefficient: "1.334" },
      { stage: 4, basePower: "1.25", coefficient: "1.667" },
    ]);
  });

  it("배우는 단계를 올리면 남는 단계 값은 그대로다", () => {
    const powers = fillStagePowers(1, 9, { ...DEFAULT_FILL, coefficientStep: "0.01" });

    expect(resizeStagePowers(powers, 3, 9)).toEqual(powers.slice(2));
  });

  it("단계 수를 늘리면 새 단계는 바로 앞 단계 값을 복사한다", () => {
    const powers = fillStagePowers(1, 9, DEFAULT_FILL);

    const resized = resizeStagePowers(powers, 1, 11);

    expect(resized.slice(0, 9)).toEqual(powers);
    expect(resized[9]).toEqual({ stage: 10, basePower: "3.6", coefficient: "1.2" });
    expect(resized[10]).toEqual({ stage: 11, basePower: "3.6", coefficient: "1.2" });
  });

  it("배우는 단계를 내리면 앞이 없는 단계는 채우기 기본값, 그다음은 앞 단계 복사다", () => {
    const powers = fillStagePowers(3, 9, { basePower: "5", coefficient: "2.2", baseStep: "0.5", coefficientStep: "0" });

    const resized = resizeStagePowers(powers, 1, 9);

    expect(resized[0]).toEqual({ stage: 1, basePower: "2", coefficient: "1.2" });
    expect(resized[1]).toEqual({ stage: 2, basePower: "2", coefficient: "1.2" });
    expect(resized.slice(2)).toEqual(powers);
  });

  it("새 신통은 이펙트 문장 빈 줄 하나와 공격이라 1성 ~ 단계 수 채우기 기본값을 가진다", () => {
    const ability = newAbility(9);

    expect(ability.effectLines).toEqual([""]);
    expect(ability.stagePowers).toEqual(fillStagePowers(1, 9, DEFAULT_FILL));
    expect(ability.stagePowers).toHaveLength(9);
  });

  it("공격이 아닌 신통은 단계별 값을 보내지 않고, 공격이면 숫자로 보낸다", () => {
    const draft = {
      ...newAbility(2),
      effectLines: ["바람이 분다.", "몸이 가벼워진다."],
      stagePowers: [
        { stage: 1, basePower: "2", coefficient: "1.2" },
        { stage: 2, basePower: "2.25", coefficient: "" },
      ],
    };

    expect(toAbility({ ...draft, kind: "ESCAPE" })).toEqual(
      expect.objectContaining({ effectLines: ["바람이 분다.", "몸이 가벼워진다."], stagePowers: [] }),
    );
    expect(toAbility(draft)).toEqual(
      expect.objectContaining({
        effectLines: ["바람이 분다.", "몸이 가벼워진다."],
        stagePowers: [
          { stage: 1, basePower: 2, coefficient: 1.2 },
          { stage: 2, basePower: 2.25, coefficient: 0 },
        ],
      }),
    );
  });

  it("저장된 신통은 이펙트 문장·단계별 값·usableInGame 을 폼 값으로 옮기고, 보낼 때 usableInGame 은 뺀다", () => {
    const response: AbilityResponse = {
      name: "화탄술",
      learnStage: 1,
      kind: "ATTACK",
      kindName: "공격",
      elements: ["FIRE"],
      bodyRefining: false,
      target: "SINGLE",
      targetName: "한 대상",
      costStat: "MANA",
      costAmount: 5,
      costPerTick: false,
      castSeconds: 0,
      cooldownSeconds: 3,
      usableWhen: "COMBAT_ONLY",
      effectLines: ["{대상}을 덮친다!"],
      stagePowers: [{ stage: 1, basePower: 2.2, coefficient: 1.224 }],
      usableInGame: false,
    };

    const draft = abilityDraftOf(response);

    expect(draft.effectLines).toEqual(["{대상}을 덮친다!"]);
    expect(draft.stagePowers).toEqual([{ stage: 1, basePower: "2.2", coefficient: "1.224" }]);
    expect(draft.usableInGame).toBe(false);
    expect(toAbility(draft)).not.toHaveProperty("usableInGame");
    expect(toAbility(draft)).not.toHaveProperty("kindName");
  });
});
