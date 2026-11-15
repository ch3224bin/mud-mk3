import type { CharacterDetailResponse, CharacterInsightResponse, CharacterStatsResponse } from "./api";
import { formatCount } from "./items";

/** 상세 스탯 표의 행(위에서부터). 기혈~영력은 최대치다 */
export const STAT_ROWS: { key: keyof CharacterStatsResponse; label: string }[] = [
  { key: "qiBlood", label: "기혈 최대치" },
  { key: "mana", label: "법력 최대치" },
  { key: "sense", label: "의식 최대치" },
  { key: "essence", label: "정혈 최대치" },
  { key: "spirit", label: "영력 최대치" },
  { key: "body", label: "육신" },
  { key: "strength", label: "근력" },
];

/** 자질은 소수 그대로 두고 화면에는 소수점을 버려 보인다(룰북 스탯, 게임 `상태` 와 같음: 52.25 → `52`) */
export function aptitudeText(aptitude: number): string {
  return String(Math.floor(aptitude));
}

/** `목·화 진영근 (목 50, 화 50)`, 정하지 않았으면 `아직 정하지 않음` */
export function spiritRootText(spiritRoot: CharacterDetailResponse["spiritRoot"]): string {
  if (spiritRoot === null) {
    return "아직 정하지 않음";
  }
  const roots = spiritRoot.roots.map((root) => `${root.elementName} ${aptitudeText(root.aptitude)}`).join(", ");
  return `${spiritRoot.name} (${roots})`;
}

/** 룰북 상태 화면의 깨달음 줄. 예: `깨달음  금 0 · 목 112 · 수 0 · 화 230 · 토 0 · 연체 0` */
export function insightText(insight: CharacterInsightResponse[]): string {
  return `깨달음  ${insight.map((entry) => `${entry.name} ${formatCount(entry.amount)}`).join(" · ")}`;
}
