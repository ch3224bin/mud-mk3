import type {
  DropTierCode,
  MobDetailResponse,
  MobKindCode,
  MobStatsResponse,
  MobTierCode,
  MobTypeSaveRequest,
  RealmResponse,
} from "./api";
import { formatCount } from "./items";

/** 몹 종류(world-server MobKind 와 같음, 선언 순서) */
export const MOB_KINDS: { code: MobKindCode; name: string }[] = [
  { code: "BEAST", name: "요수" },
  { code: "CULTIVATOR", name: "적대 수선자" },
  { code: "GHOST", name: "귀물·음혼" },
  { code: "PUPPET", name: "괴뢰" },
];

/** 몹 체급(world-server MobTier 와 같음, 선언 순서) */
export const MOB_TIERS: { code: MobTierCode; name: string }[] = [
  { code: "NORMAL", name: "일반" },
  { code: "STRONG", name: "강적" },
  { code: "ELITE", name: "정예" },
  { code: "BOSS", name: "두목" },
];

/** 드롭 계층(world-server DropTier 와 같음, 선언 순서가 화면·요청 순서) */
export const DROP_TIERS: { code: DropTierCode; name: string }[] = [
  { code: "ADVANCED", name: "고급" },
  { code: "RARE", name: "희귀" },
  { code: "TREASURE", name: "진귀" },
];

/** 두목 공격 계수 칸의 기본값 */
export const DEFAULT_BOSS_ATTACK_COEFFICIENT = "0.6";

/** 재등장 시간(4차 시간 문체): 60초 미만 `30초`, 나누어떨어지는 분 `30분`, 그 밖 `1분 30초` */
export function formatRespawn(seconds: number): string {
  if (seconds < 60) {
    return `${seconds}초`;
  }
  const minutes = Math.floor(seconds / 60);
  const rest = seconds % 60;
  return rest === 0 ? `${formatCount(minutes)}분` : `${formatCount(minutes)}분 ${rest}초`;
}

/** `기혈 49 · 육신 8 · 위력 8` */
export function statsText(stats: MobStatsResponse): string {
  return `기혈 ${formatCount(stats.qiBlood)} · 육신 ${formatCount(stats.body)} · 위력 ${formatCount(stats.power)}`;
}

/** 몹 기준 경지 선택지: 경지 목록에서 범인을 뺀다(연기 1층 ~ 축기 대원만) */
export function mobRealms(realms: RealmResponse[]): RealmResponse[] {
  return realms.filter((realm) => realm.code !== "MORTAL");
}

/** 폼에서 고치는 드롭 표. 고정 재료 수는 입력 그대로의 글자, 계층은 물건 종류 id 목록이다 */
export type DropTableDraft = {
  fixedDrops: { itemTypeId: number; count: string }[];
  tierDrops: Record<DropTierCode, number[]>;
};

export function emptyDropTable(): DropTableDraft {
  return { fixedDrops: [], tierDrops: { ADVANCED: [], RARE: [], TREASURE: [] } };
}

export function dropTableDraftOf(
  fixedDrops: MobDetailResponse["fixedDrops"],
  tierDrops: MobDetailResponse["tierDrops"],
): DropTableDraft {
  const draft = emptyDropTable();
  draft.fixedDrops = fixedDrops.map((drop) => ({ itemTypeId: drop.itemTypeId, count: String(drop.count) }));
  for (const drop of tierDrops) {
    draft.tierDrops[drop.tier].push(drop.itemTypeId);
  }
  return draft;
}

/** 저장 본문의 드롭 표. 계층 드롭은 고급·희귀·진귀 순서로 편다 */
export function toDropTable(draft: DropTableDraft): Pick<MobTypeSaveRequest, "fixedDrops" | "tierDrops"> {
  return {
    fixedDrops: draft.fixedDrops.map((drop) => ({ itemTypeId: drop.itemTypeId, count: Number(drop.count) })),
    tierDrops: DROP_TIERS.flatMap((tier) =>
      draft.tierDrops[tier.code].map((itemTypeId) => ({ tier: tier.code, itemTypeId })),
    ),
  };
}
