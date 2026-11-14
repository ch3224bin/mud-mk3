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
