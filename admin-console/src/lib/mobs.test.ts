import { describe, expect, it } from "vitest";
import type { MobDetailResponse, RealmResponse } from "./api";
import {
  DROP_TIERS,
  dropTableDraftOf,
  emptyDropTable,
  formatRespawn,
  MOB_KINDS,
  MOB_TIERS,
  mobRealms,
  statsText,
  toDropTable,
} from "./mobs";

describe("몹 상수", () => {
  it("종류·체급·계층의 코드와 이름은 world-server enum 선언 순서다", () => {
    expect(MOB_KINDS.map((k) => `${k.code}:${k.name}`)).toEqual([
      "BEAST:요수",
      "CULTIVATOR:적대 수선자",
      "GHOST:귀물·음혼",
      "PUPPET:괴뢰",
    ]);
    expect(MOB_TIERS.map((t) => `${t.code}:${t.name}`)).toEqual([
      "NORMAL:일반",
      "STRONG:강적",
      "ELITE:정예",
      "BOSS:두목",
    ]);
    expect(DROP_TIERS.map((t) => `${t.code}:${t.name}`)).toEqual(["ADVANCED:고급", "RARE:희귀", "TREASURE:진귀"]);
  });
});

describe("formatRespawn", () => {
  it("60초 미만은 초, 나누어떨어지는 분은 분, 그 밖은 분과 초다", () => {
    expect(formatRespawn(30)).toBe("30초");
    expect(formatRespawn(60)).toBe("1분");
    expect(formatRespawn(90)).toBe("1분 30초");
    expect(formatRespawn(180)).toBe("3분");
    expect(formatRespawn(1800)).toBe("30분");
    expect(formatRespawn(86400)).toBe("1,440분");
  });
});

describe("statsText", () => {
  it("기혈 · 육신 · 위력 한 줄", () => {
    expect(statsText({ qiBlood: 49, body: 8, power: 8 })).toBe("기혈 49 · 육신 8 · 위력 8");
    expect(statsText({ qiBlood: 1880, body: 34, power: 21 })).toBe("기혈 1,880 · 육신 34 · 위력 21");
  });
});

describe("mobRealms", () => {
  it("범인을 뺀다", () => {
    const realms: RealmResponse[] = [
      { code: "MORTAL", name: "범인" },
      { code: "QI_1", name: "연기 1층" },
      { code: "FOUNDATION_PEAK", name: "축기 대원만" },
    ];
    expect(mobRealms(realms).map((r) => r.code)).toEqual(["QI_1", "FOUNDATION_PEAK"]);
  });
});

describe("드롭 표 초안", () => {
  it("빈 드롭 표는 고정 재료 없음·계층 셋 빈 목록이다", () => {
    expect(emptyDropTable()).toEqual({ fixedDrops: [], tierDrops: { ADVANCED: [], RARE: [], TREASURE: [] } });
    expect(toDropTable(emptyDropTable())).toEqual({ fixedDrops: [], tierDrops: [] });
  });

  it("저장된 드롭 표를 계층별로 나누고, 요청은 고급·희귀·진귀 순서로 편다", () => {
    const fixedDrops: MobDetailResponse["fixedDrops"] = [{ itemTypeId: 7, itemTypeName: "늑대 가죽", count: 2 }];
    const tierDrops: MobDetailResponse["tierDrops"] = [
      { tier: "RARE", tierName: "희귀", itemTypeId: 9, itemTypeName: "청랑 송곳니" },
      { tier: "ADVANCED", tierName: "고급", itemTypeId: 8, itemTypeName: "늑대 힘줄" },
      { tier: "ADVANCED", tierName: "고급", itemTypeId: 5, itemTypeName: "회기단" },
    ];

    const draft = dropTableDraftOf(fixedDrops, tierDrops);

    expect(draft).toEqual({
      fixedDrops: [{ itemTypeId: 7, count: "2" }],
      tierDrops: { ADVANCED: [8, 5], RARE: [9], TREASURE: [] },
    });
    expect(toDropTable(draft)).toEqual({
      fixedDrops: [{ itemTypeId: 7, count: 2 }],
      tierDrops: [
        { tier: "ADVANCED", itemTypeId: 8 },
        { tier: "ADVANCED", itemTypeId: 5 },
        { tier: "RARE", itemTypeId: 9 },
      ],
    });
  });
});
