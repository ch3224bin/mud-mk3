import { describe, expect, it } from "vitest";
import {
  ABILITY_KINDS,
  ABILITY_TARGETS,
  COST_STATS,
  ELEMENTS,
  emptyStage,
  inElementOrder,
  newAbility,
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
    expect(toAbility({ ...newAbility(), name: "화탄술", costStat: "MANA", costAmount: "5.5" })).toEqual({
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
    });
  });
});
