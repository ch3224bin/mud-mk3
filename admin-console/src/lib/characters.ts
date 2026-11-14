import type { CharacterDetailResponse, CharacterStatsResponse } from "./api";

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

/** 자질은 소수라 소수 첫째 자리까지 보인다(50.0 → `50`, 52.25 → `52.3`) */
export function aptitudeText(aptitude: number): string {
  return String(Math.round(aptitude * 10) / 10);
}

/** `목·화 진영근 (목 50, 화 50)`, 정하지 않았으면 `아직 정하지 않음` */
export function spiritRootText(spiritRoot: CharacterDetailResponse["spiritRoot"]): string {
  if (spiritRoot === null) {
    return "아직 정하지 않음";
  }
  const roots = spiritRoot.roots.map((root) => `${root.elementName} ${aptitudeText(root.aptitude)}`).join(", ");
  return `${spiritRoot.name} (${roots})`;
}
