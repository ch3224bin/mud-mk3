import type { InsightEntry } from "./lib/protocol";

/** 깨달음 여섯 종류, 모두 0(서버 status 메시지 insight 의 순서). */
export function zeroInsight(): InsightEntry[] {
  return [
    { kind: "METAL", name: "금", amount: 0 },
    { kind: "WOOD", name: "목", amount: 0 },
    { kind: "WATER", name: "수", amount: 0 },
    { kind: "FIRE", name: "화", amount: 0 },
    { kind: "EARTH", name: "토", amount: 0 },
    { kind: "BODY", name: "연체", amount: 0 },
  ];
}

/** 깨달음 여섯 종류에서 한 종류만 amount 로. */
export function insightWith(kind: InsightEntry["kind"], amount: number): InsightEntry[] {
  return zeroInsight().map((entry) => (entry.kind === kind ? { ...entry, amount } : entry));
}
