import type { FactionKind } from "./api";

export const FACTION_KINDS: { code: FactionKind; name: "성" | "종문" }[] = [
  { code: "CASTLE", name: "성" },
  { code: "SECT", name: "종문" },
];

/** 소유 선택·표시에 쓰는 이름. 예: 청석성 (성) */
export function factionLabel(name: string, kind: FactionKind): string {
  const kindName = FACTION_KINDS.find((k) => k.code === kind)?.name ?? kind;
  return `${name} (${kindName})`;
}
